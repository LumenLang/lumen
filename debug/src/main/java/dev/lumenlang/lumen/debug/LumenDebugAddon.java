package dev.lumenlang.lumen.debug;

import com.google.auto.service.AutoService;
import dev.lumenlang.lumen.api.ConfigOption;
import dev.lumenlang.lumen.api.ConfigOverride;
import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.LumenAddon;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.LumenTypeRegistry;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.debug.auth.policy.AuthManager;
import dev.lumenlang.lumen.debug.auth.store.TrustStore;
import dev.lumenlang.lumen.debug.command.LumenDebugCommand;
import dev.lumenlang.lumen.debug.hook.ScriptHooks;
import dev.lumenlang.lumen.debug.log.AnsiBanner;
import dev.lumenlang.lumen.debug.server.DebugServer;
import dev.lumenlang.lumen.debug.session.DebugSession;
import dev.lumenlang.lumen.debug.transform.LineInstrumentTransformer;
import dev.lumenlang.lumen.plugin.Lumen;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import dev.lumenlang.lumen.plugin.scripts.CompiledClassCache;
import dev.lumenlang.lumen.plugin.scripts.ScriptManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lumen addon that provides script instrumentation, snippet execution,
 * and a WebSocket server for editor integration.
 */
@AutoService(LumenAddon.class)
public final class LumenDebugAddon implements LumenAddon {

    private static final Logger LOG = Logger.getLogger(LumenDebugAddon.class.getSimpleName());

    private DebugServer server;

    private static void registerSnippetVarExpression(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("LumenDebug")
                .pattern("snippet var %name:STRING%")
                .description("Retrieves a variable from the debug snippet context with its original type.")
                .handler(ctx -> {
                    ctx.codegen().addImport(ScriptHooks.class.getName());
                    String varName = String.valueOf(ctx.value("name"));
                    ScriptHooks.SnippetVarMeta meta = ScriptHooks.snippetVarMeta().get(varName);
                    if (meta == null) {
                        return new ExpressionHandler.ExpressionResult("ScriptHooks.snippetVars().get(\"" + varName + "\")", PrimitiveType.STRING);
                    }
                    if (meta.importClass() != null) ctx.codegen().addImport(meta.importClass());
                    LumenType type = resolveSnippetType(meta);
                    String java = "((" + meta.castType() + ") ScriptHooks.snippetVars().get(\"" + varName + "\"))";
                    return new ExpressionHandler.ExpressionResult(java, type);
                }));
    }

    private static @NotNull LumenType resolveSnippetType(@NotNull ScriptHooks.SnippetVarMeta meta) {
        if (meta.refTypeId() != null) {
            ObjectType ot = LumenTypeRegistry.byId(meta.refTypeId());
            if (ot != null) return ot;
        }
        if (meta.javaType() != null) {
            PrimitiveType p = PrimitiveType.fromJavaType(meta.javaType());
            if (p != null) return p;
        }
        return PrimitiveType.STRING;
    }

    /**
     * Resolves the Lumen type metadata for a live runtime object by walking the class
     * hierarchy and interface tree and matching each type against the registered
     * {@link LumenTypeRegistry}.
     *
     * @param value the runtime object
     */
    public static @NotNull ScriptHooks.SnippetVarMeta resolveType(@NotNull Object value) {
        if (value instanceof String) return new ScriptHooks.SnippetVarMeta("String", null, "String", null);
        if (value instanceof Integer) return new ScriptHooks.SnippetVarMeta("Integer", null, "int", null);
        if (value instanceof Double) return new ScriptHooks.SnippetVarMeta("Double", null, "double", null);
        if (value instanceof Long) return new ScriptHooks.SnippetVarMeta("Long", null, "long", null);
        if (value instanceof Float) return new ScriptHooks.SnippetVarMeta("Float", null, "float", null);
        if (value instanceof Boolean) return new ScriptHooks.SnippetVarMeta("Boolean", null, "boolean", null);
        return resolveObjectType(value.getClass());
    }

    private static @NotNull ScriptHooks.SnippetVarMeta resolveObjectType(@NotNull Class<?> start) {
        Deque<Class<?>> queue = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            Class<?> cls = queue.poll();
            if (cls == null || cls == Object.class || !visited.add(cls)) continue;
            ObjectType found = LumenTypeRegistry.fromJava(cls.getName());
            if (found != null)
                return new ScriptHooks.SnippetVarMeta(cls.getSimpleName(), found.id(), null, cls.getName());
            if (cls.getSuperclass() != null) queue.add(cls.getSuperclass());
            Collections.addAll(queue, cls.getInterfaces());
        }
        return new ScriptHooks.SnippetVarMeta("Object", null, null, null);
    }

    @Override
    public @NotNull String name() {
        return "LumenDebug";
    }

    @Override
    public @NotNull String description() {
        return "Live debug bridge for Lumen scripts. Designed for AI.";
    }

    @Override
    public @NotNull String version() {
        return "1.0.0";
    }

    @Override
    public @NotNull List<ConfigOverride> configOverrides() {
        return List.of(
                ConfigOverride.enable(ConfigOption.CODE_TRANSFORM).permanent("required for script instrumentation"),
                ConfigOverride.enable(ConfigOption.RAW_JAVA).lastingSession("required for java block support in snippets")
        );
    }

    @Override
    public void onEnable(@NotNull LumenAPI api) {
        DebugSession session = new DebugSession();
        LineInstrumentTransformer transformer = new LineInstrumentTransformer();

        api.transformers().register(transformer);
        ScriptHooks.install(session);

        registerSnippetVarExpression(api);

        LumenConfiguration.Debug.Service cfg = LumenConfiguration.DEBUG.SERVICE;
        Path trustFile = Lumen.instance().getDataFolder().toPath().resolve("debug-trust.json");
        TrustStore trust = new TrustStore(trustFile);
        AuthManager auth = new AuthManager(trust);

        server = new DebugServer(cfg.BIND_HOST, cfg.PORT, auth, session, transformer, (name, source) -> {
            CompiledClassCache.invalidate(name);
            return ScriptManager.load(name, source);
        });

        LumenDebugCommand.register(auth, req -> {
            if (server != null) server.onPairingApproved(req);
        });

        try {
            server.start();
            LOG.info(AnsiBanner.enabledWarning(cfg.PORT, cfg.BIND_HOST, cfg.REMOTE.ALLOW_REMOTE_ACCESS));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to start debug server on " + cfg.BIND_HOST + ":" + cfg.PORT, e);
        }
    }

    @Override
    public void onDisable() {
        ScriptHooks.install(null);
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error stopping debug server", e);
            }
            server = null;
            LumenDebugCommand.unregister();
        }
    }
}
