package dev.lumenlang.lumen.plugin;

import dev.lumenlang.lumen.api.ConfigOption;
import dev.lumenlang.lumen.api.ConfigOverride;
import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.LumenAddon;
import dev.lumenlang.lumen.api.LumenProvider;
import dev.lumenlang.lumen.api.StringConfigOverride;
import dev.lumenlang.lumen.api.scanner.RegistrationScanner;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.version.MinecraftVersion;
import dev.lumenlang.lumen.pipeline.addon.AddonManager;
import dev.lumenlang.lumen.pipeline.addon.LumenAPIImpl;
import dev.lumenlang.lumen.pipeline.addon.ScriptBinderManager;
import dev.lumenlang.lumen.pipeline.binder.ScriptBinder;
import dev.lumenlang.lumen.pipeline.bus.LumenEventBus;
import dev.lumenlang.lumen.pipeline.inject.InjectableHandlers;
import dev.lumenlang.lumen.pipeline.java.compiled.DefaultImportRegistry;
import dev.lumenlang.lumen.pipeline.java.compiler.CompilerClasspath;
import dev.lumenlang.lumen.pipeline.language.emit.EmitRegistry;
import dev.lumenlang.lumen.pipeline.language.emit.TransformerRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.pipeline.persist.GlobalVars;
import dev.lumenlang.lumen.pipeline.persist.PersistentVars;
import dev.lumenlang.lumen.pipeline.persist.impl.FilePersistentStorage;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import dev.lumenlang.lumen.plugin.commands.CommandRegistry;
import dev.lumenlang.lumen.plugin.commands.lumen.LumenCommand;
import dev.lumenlang.lumen.plugin.compiler.JavaCompilerBackend;
import dev.lumenlang.lumen.plugin.compiler.ScriptCompiler;
import dev.lumenlang.lumen.plugin.compiler.VantaCompilerBackend;
import dev.lumenlang.lumen.plugin.compiler.system.SystemCompiler;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import dev.lumenlang.lumen.plugin.defaults.type.BuiltinTypeBindings;
import dev.lumenlang.lumen.plugin.documentation.DocumentationDumper;
import dev.lumenlang.lumen.plugin.inject.InjectableHandlerFactoryImpl;
import dev.lumenlang.lumen.plugin.platform.ServerPlatform;
import dev.lumenlang.lumen.plugin.scanner.RegistrationScannerBackend;
import dev.lumenlang.lumen.plugin.scheduler.ScriptScheduler;
import dev.lumenlang.lumen.plugin.scripts.ExampleCopier;
import dev.lumenlang.lumen.plugin.scripts.ScriptManager;
import dev.lumenlang.lumen.plugin.scripts.ScriptSourceLoader;
import dev.lumenlang.lumen.plugin.scripts.ScriptWatcher;
import dev.lumenlang.lumen.plugin.util.BukkitValueResolver;
import dev.lumenlang.lumen.plugin.util.InventoryHotReload;
import net.vansencool.lsyaml.binding.watcher.ConfigWatcher;
import net.vansencool.lsyaml.logger.JulLogAdapter;
import net.vansencool.lsyaml.logger.LSYAMLLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Locale;

/**
 * Lumen is a high-performance scripting platform for Minecraft servers.
 *
 * <p>Lumen allows server owners to write simple, readable scripts that define
 * custom server behavior without requiring Java knowledge. Unlike traditional
 * scripting plugins that interpret scripts at runtime, Lumen processes scripts
 * once and converts them into native Java code that runs directly on the server.
 */
@SuppressWarnings("unused")
public final class Lumen extends JavaPlugin {

    private static JavaPlugin plugin;
    private static AddonManager addonManager;
    private static LumenAPI lumenApi;
    private PatternRegistry patternRegistry;
    private ScriptWatcher scriptWatcher;
    private ConfigWatcher configWatcher;
    private LumenEventBus eventBus;

    /**
     * Returns the plugin instance.
     *
     * @return the plugin
     */
    public static @NotNull JavaPlugin instance() {
        return plugin;
    }

    /**
     * Returns the addon manager.
     *
     * @return the addon manager
     */
    public static @NotNull AddonManager addonManager() {
        return addonManager;
    }

    @Override
    public void onLoad() {
        plugin = this;
        MinecraftVersion.detect(Bukkit.getBukkitVersion().split("-")[0]);
        LumenLogger.init(this.getLogger());
        LSYAMLLogger.setAdapter(new JulLogAdapter("Lumen"));
        LumenConfiguration.load();
        initApi();
    }

    @Override
    public void onEnable() {
        applyAddonSettings();
        platformCheck();
        addonManager.enableAll(lumenApi);
        ExampleCopier.copyExamples(ScriptSourceLoader.scriptsDir());
        setupCompiler();
        LumenCommand.register();
        if (LumenConfiguration.SCRIPTS.ENABLE_ALL_SCRIPTS_IMMEDIATELY_ON_STARTUP) {
            loadScripts();
            scriptWatcher = new ScriptWatcher();
            scriptWatcher.start(ScriptSourceLoader.scriptsDir());
            if (LumenConfiguration.EXTRA.ENABLE_DOCUMENTATION_TOOL) DocumentationDumper.dump(patternRegistry, addonManager);
        } else {
            Bukkit.getScheduler().runTask(this, () -> {
                loadScripts();
                scriptWatcher = new ScriptWatcher();
                scriptWatcher.start(ScriptSourceLoader.scriptsDir());
                if (LumenConfiguration.EXTRA.ENABLE_DOCUMENTATION_TOOL) DocumentationDumper.dump(patternRegistry, addonManager);
            });
        }
    }

    @Override
    public void onDisable() {
        if (scriptWatcher != null) {
            scriptWatcher.stop();
            scriptWatcher = null;
        }
        ConfigWatcher.shutdown();
        ScriptManager.unloadAllSync();
        ScriptManager.shutdownPool();
        InventoryHotReload.clear();
        if (eventBus != null) eventBus.shutdown();
        RegistrationScanner.teardown();
        ScriptBinder.teardown();
        LumenProvider.teardown();
        if (addonManager != null) {
            addonManager.disableAll();
        }
        LumenCommand.unregister();
        CommandRegistry.unregisterAll();
        ScriptScheduler.cancelAllGlobal();
        PersistentVars.shutdown();
        lumenApi = null;
    }

    /**
     * Initialises the Lumen API: creates registries, scans all built-in defaults,
     * loads jar-based addons, and enables them.
     */
    private void initApi() {
        MinecraftTypes.registerAll();
        BuiltinLumenTypes.registerAll();
        InjectableHandlers.factory(new InjectableHandlerFactoryImpl());
        TypeRegistry types = new TypeRegistry();
        BuiltinTypeBindings.register(types);
        patternRegistry = new PatternRegistry(types);
        PatternRegistry.instance(patternRegistry);

        EmitRegistry emitReg = new EmitRegistry();
        EmitRegistry.instance(emitReg);

        TransformerRegistry transformerReg = new TransformerRegistry();
        TransformerRegistry.instance(transformerReg);

        ScriptBinderManager binderManager = new ScriptBinderManager();
        ScriptBinder.init(binderManager);

        lumenApi = new LumenAPIImpl(patternRegistry, types, emitReg, transformerReg, binderManager);

        DefaultImportRegistry.register("org.bukkit.event.Listener");
        DefaultImportRegistry.register("org.bukkit.command.CommandSender");
        DefaultImportRegistry.register("org.bukkit.entity.Player");
        DefaultImportRegistry.register("org.bukkit.plugin.Plugin");
        DefaultImportRegistry.register("org.bukkit.Bukkit");
        DefaultImportRegistry.register(PersistentVars.class.getName());
        DefaultImportRegistry.register(GlobalVars.class.getName());
        DefaultImportRegistry.register("dev.lumenlang.lumen.plugin.text.LumenText");
        DefaultImportRegistry.register("dev.lumenlang.lumen.plugin.annotations.LumenEvent");
        DefaultImportRegistry.register("dev.lumenlang.lumen.plugin.annotations.LumenCmd");
        DefaultImportRegistry.register("dev.lumenlang.lumen.plugin.annotations.LumenInventory");
        DefaultImportRegistry.register("dev.lumenlang.lumen.api.annotations.LumenPreload");
        DefaultImportRegistry.register("dev.lumenlang.lumen.api.annotations.LumenLoad");

        PersistentVars.init(new FilePersistentStorage(getDataFolder().toPath().resolve("persist.dat")));
        PersistentVars.setValueResolver(BukkitValueResolver.INSTANCE);

        addonManager = new AddonManager();
        LumenProvider.init(lumenApi, addonManager::registerAddon);
        eventBus = new LumenEventBus();
        LumenProvider.initBus(eventBus);
        File addonsDir = new File(getDataFolder(), "addons");
        if (!addonsDir.exists() && !addonsDir.mkdirs()) throw new RuntimeException("Failed to create addons directory");
        addonManager.loadAddons(addonsDir);

        RegistrationScanner.init(new RegistrationScannerBackend(lumenApi));
        RegistrationScanner.scan("dev.lumenlang.lumen.plugin.defaults");
        patternRegistry.warmup();
    }

    /**
     * Loads all scripts according to the current configuration.
     */
    private void loadScripts() {
        if (!LumenConfiguration.SCRIPTS.LOAD_ALL_ON_STARTUP) return;
        if (LumenConfiguration.PERFORMANCE.LOAD_SCRIPTS_ASYNC_ON_STARTUP) {
            long startTime = System.nanoTime();
            ScriptManager.loadAll().whenComplete((prepared, err) -> {
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                if (err != null) {
                    LumenLogger.severe("Failed to load scripts on startup (async): " + err.getMessage());
                } else {
                    LumenLogger.info("Loaded all scripts (async) in " + durationMs + "ms");
                }
            });
        } else {
            try {
                long startTime = System.nanoTime();
                ScriptManager.loadAllSync();
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                LumenLogger.info("Loaded all scripts in " + durationMs + "ms");
            } catch (Throwable t) {
                LumenLogger.severe("Failed to load scripts on startup: " + t.getMessage());
            }
        }
    }

    private void applyAddonSettings() {
        for (LumenAddon addon : addonManager.addons()) {
            for (ConfigOverride override : addon.configOverrides()) {
                LumenLogger.warning("Addon " + addon.name() + " v" + addon.version() + " is " + (override.value() ? "enabling" : "disabling") + " " + override.option().path() + ": " + override.reason());
                LumenConfiguration.applyOverride(override);
            }
            for (StringConfigOverride override : addon.stringConfigOverrides()) {
                LumenLogger.warning("Addon " + addon.name() + " v" + addon.version() + " is setting " + override.option().path() + " to '" + override.value() + "': " + override.reason());
                LumenConfiguration.applyOverride(override);
            }
        }
    }

    private void platformCheck() {
        if (ServerPlatform.isFolia()) {
            this.getLogger().warning("Folia detected. Folia is currently not officially supported, features such as entity, schedules, and additional issues may occur. Use at your own risk.");
            LumenConfiguration.applyOverride(ConfigOverride.enable(ConfigOption.ENABLE_ALL_SCRIPTS_IMMEDIATELY)
                    .lastingSession("Folia does not support runTask; all scripts must be initialized immediately on startup."));
        }
        if (!ServerPlatform.isPaper() && LumenConfiguration.FEATURES.PAPER_ONLY_FEATURES) {
            this.getLogger().warning(
                    "Paper-only features are enabled in the configuration, but you're not running Paper. To prevent issues, we are automatically setting the 'paper-only-features' config option to false.");
            LumenConfiguration.writeOption(ConfigOption.PAPER_ONLY_FEATURES, false);
        }
        if (ServerPlatform.isCraftBukkit()) {
            this.getLogger().severe("====================================================");
            this.getLogger().severe("Unsupported server software detected: " + ServerPlatform.getSoftwareName());
            this.getLogger().severe("This plugin requires Spigot, Paper, or any forks of them.");
            this.getLogger().severe("Please switch to Paper or Spigot.");
            this.getLogger().severe("====================================================");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void setupCompiler() {
        CompilerClasspath.setReduceClasspath(LumenConfiguration.PERFORMANCE.REDUCE_CLASSPATH);
        ScriptCompiler.setBackend(pickBackend());
        if (LumenConfiguration.PERFORMANCE.WARMUP_ON_STARTUP) {
            Thread warmup = new Thread(ScriptManager::warmup, "Lumen-Warmup");
            warmup.setDaemon(true);
            warmup.start();
        }
    }

    private @NotNull JavaCompilerBackend pickBackend() {
        String configured = LumenConfiguration.PERFORMANCE.COMPILER.toLowerCase(Locale.ROOT).trim();
        boolean javacAvailable = SystemCompiler.isAvailable();

        if (configured.equals("vanta")) {
            LumenLogger.info("Using Vanta compiler backend (beta).");
            return new VantaCompilerBackend();
        }

        if (configured.equals("javac")) {
            if (!javacAvailable) {
                LumenLogger.warning("Compiler set to 'javac' but no system Java compiler found. Falling back to Vanta.");
                return new VantaCompilerBackend();
            }
            LumenLogger.info("Using javac compiler backend.");
            return new SystemCompiler();
        }

        if (javacAvailable) {
            LumenLogger.info("Using javac compiler backend.");
            return new SystemCompiler();
        }

        LumenLogger.info("No system Java compiler found. Using Vanta compiler backend (beta).");
        return new VantaCompilerBackend();
    }
}
