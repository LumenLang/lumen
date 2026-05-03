package dev.lumenlang.lumen.headless;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.LumenProvider;
import dev.lumenlang.lumen.api.scanner.RegistrationScanner;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.version.MinecraftVersion;
import dev.lumenlang.lumen.pipeline.addon.AddonManager;
import dev.lumenlang.lumen.pipeline.addon.LumenAPIImpl;
import dev.lumenlang.lumen.pipeline.addon.ScriptBinderManager;
import dev.lumenlang.lumen.pipeline.binder.ScriptBinder;
import dev.lumenlang.lumen.pipeline.bus.LumenEventBus;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.events.EventDefRegistry;
import dev.lumenlang.lumen.pipeline.inject.InjectableHandlers;
import dev.lumenlang.lumen.pipeline.java.JavaBuilder;
import dev.lumenlang.lumen.pipeline.language.emit.CodeEmitter;
import dev.lumenlang.lumen.pipeline.language.emit.EmitRegistry;
import dev.lumenlang.lumen.pipeline.language.emit.TransformerRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import dev.lumenlang.lumen.plugin.defaults.type.BuiltinTypeBindings;
import dev.lumenlang.lumen.plugin.inject.InjectableHandlerFactoryImpl;
import dev.lumenlang.lumen.plugin.scanner.RegistrationScannerBackend;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Headless Lumen bootstrap and CLI for validating Lumen scripts without
 * a running Minecraft server. All pattern, type, event, and emit registrations
 * are loaded from the plugin defaults using spigot-api on the classpath.
 */
public final class HeadlessLumen {

    private static final Logger LOGGER = Logger.getLogger("HeadlessLumen");

    private final PatternRegistry patternRegistry;
    private final TypeRegistry typeRegistry;

    /**
     * Bootstraps the Lumen registration system, loading all builtin patterns,
     * types, events, and emitters without a running Minecraft server.
     */
    public HeadlessLumen() {
        HeadlessBukkitServer.install();
        LumenLogger.init(LOGGER);
        MinecraftVersion.detect("1.20");
        MinecraftTypes.registerAll();
        BuiltinLumenTypes.registerAll();
        InjectableHandlers.factory(new InjectableHandlerFactoryImpl());

        typeRegistry = new TypeRegistry();
        BuiltinTypeBindings.register(typeRegistry);
        patternRegistry = new PatternRegistry(typeRegistry);
        PatternRegistry.instance(patternRegistry);

        EmitRegistry emitRegistry = new EmitRegistry();
        EmitRegistry.instance(emitRegistry);

        TransformerRegistry transformerRegistry = new TransformerRegistry();
        TransformerRegistry.instance(transformerRegistry);

        ScriptBinderManager binderManager = new ScriptBinderManager();
        ScriptBinder.init(binderManager);

        LumenProvider.initBus(new LumenEventBus());

        LumenAPI api = new LumenAPIImpl(patternRegistry, typeRegistry, emitRegistry, transformerRegistry, binderManager);

        AddonManager addonManager = new AddonManager();
        LumenProvider.init(api, addonManager::registerAddon);

        RegistrationScanner.init(new RegistrationScannerBackend(api));
        RegistrationScanner.scan("dev.lumenlang.lumen.plugin.defaults");

        LOGGER.info("Bootstrap complete: " + patternCount() + " patterns, " + typeRegistry.allBindings().size() + " types, " + EventDefRegistry.defs().size() + " events");
    }

    /**
     * CLI entry point. Supports two modes:
     * <ul>
     *   <li>{@code --server} starts a JSON line protocol on stdin/stdout</li>
     *   <li>{@code <script.luma>} validates and compiles a single script file</li>
     * </ul>
     */
    public static void main(@NotNull String[] args) {
        if (args.length < 1) {
            System.err.println("Usage:");
            System.err.println("  java -jar LumenHeadless.jar <script.luma>");
            System.err.println("  java -jar LumenHeadless.jar --server");
            System.exit(1);
        }

        if (args[0].equals("--server")) {
            JsonProtocol.run(new HeadlessLumen());
            return;
        }

        runCli(Path.of(args[0]));
    }

    /**
     * Runs single-file CLI validation and compilation.
     */
    private static void runCli(@NotNull Path scriptPath) {
        if (!Files.exists(scriptPath)) {
            System.err.println("File not found: " + scriptPath);
            System.exit(1);
        }

        HeadlessLumen headless = new HeadlessLumen();
        String source;
        try {
            source = Files.readString(scriptPath);
        } catch (IOException e) {
            System.err.println("Failed to read file: " + e.getMessage());
            System.exit(1);
            return;
        }

        String name = scriptPath.getFileName().toString();
        try {
            headless.parse(source, name);
            System.out.println("Parsed successfully: " + name);
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Parse FAILED for: " + name);
            System.err.println("Error: " + e.getMessage());
            if (e.getCause() != null) System.err.println("Caused by: " + e.getCause().getMessage());
            System.exit(1);
        }
    }

    /**
     * Parses a Lumen script. Throws on parse errors.
     */
    public void parse(@NotNull String source, @NotNull String scriptName) {
        CodegenContextImpl ctx = new CodegenContextImpl(scriptName);
        TypeEnvImpl env = new TypeEnvImpl();
        CodeEmitter.generate(source, patternRegistry, env, ctx, new JavaBuilder());
    }

    /**
     * Returns the total count of all registered patterns across all categories.
     */
    public int patternCount() {
        return patternRegistry.getStatements().size() + patternRegistry.getBlocks().size() + patternRegistry.getExpressions().size() + patternRegistry.getConditionRegistry().getConditions().size() + patternRegistry.getLoopRegistry().getLoops().size();
    }

    /**
     * Returns the pattern registry containing all registered patterns.
     */
    public @NotNull PatternRegistry patternRegistry() {
        return patternRegistry;
    }

    /**
     * Returns the type registry containing all registered type bindings.
     */
    public @NotNull TypeRegistry typeRegistry() {
        return typeRegistry;
    }
}
