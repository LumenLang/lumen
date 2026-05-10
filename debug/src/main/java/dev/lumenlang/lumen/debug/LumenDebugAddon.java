package dev.lumenlang.lumen.debug;

import com.google.auto.service.AutoService;
import dev.lumenlang.lumen.api.ConfigOption;
import dev.lumenlang.lumen.api.ConfigOverride;
import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.LumenAddon;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.debug.auth.policy.AuthManager;
import dev.lumenlang.lumen.debug.auth.store.TrustStore;
import dev.lumenlang.lumen.debug.command.LumenDebugCommand;
import dev.lumenlang.lumen.debug.hook.ScriptHooks;
import dev.lumenlang.lumen.debug.log.AnsiBanner;
import dev.lumenlang.lumen.debug.server.DebugServer;
import dev.lumenlang.lumen.debug.server.snippet.SnippetTypeResolver;
import dev.lumenlang.lumen.debug.session.DebugSession;
import dev.lumenlang.lumen.debug.transform.LineInstrumentTransformer;
import dev.lumenlang.lumen.plugin.Lumen;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import dev.lumenlang.lumen.plugin.scripts.Scripts;
import dev.lumenlang.lumen.plugin.scripts.cache.CompiledClassCache;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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
                    return new ExpressionHandler.ExpressionResult("((" + meta.castType() + ") ScriptHooks.snippetVars().get(\"" + varName + "\"))", SnippetTypeResolver.lumenTypeOf(meta));
                }));
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
        AuthManager auth = new AuthManager(new TrustStore(Lumen.instance().getDataFolder().toPath().resolve("debug-trust.json")));

        server = new DebugServer(cfg.BIND_HOST, cfg.PORT, auth, session, transformer, (name, source) -> {
            CompiledClassCache.invalidate(name);
            return Scripts.load(name, source);
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
