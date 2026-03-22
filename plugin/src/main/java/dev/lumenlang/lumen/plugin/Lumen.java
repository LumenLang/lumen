package dev.lumenlang.lumen.plugin;

import dev.lumenlang.lumen.api.ConfigOption;
import dev.lumenlang.lumen.api.ConfigOverride;
import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.LumenAddon;
import dev.lumenlang.lumen.api.LumenProvider;
import dev.lumenlang.lumen.api.scanner.RegistrationScanner;
import dev.lumenlang.lumen.api.version.MinecraftVersion;
import dev.lumenlang.lumen.pipeline.addon.AddonManager;
import dev.lumenlang.lumen.pipeline.addon.LumenAPIImpl;
import dev.lumenlang.lumen.pipeline.addon.ScriptBinderManager;
import dev.lumenlang.lumen.pipeline.binder.ScriptBinder;
import dev.lumenlang.lumen.pipeline.java.compiler.system.SystemCompiler;
import dev.lumenlang.lumen.pipeline.language.emit.EmitRegistry;
import dev.lumenlang.lumen.pipeline.language.emit.TransformerRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.pipeline.persist.PersistentVars;
import dev.lumenlang.lumen.pipeline.persist.impl.FilePersistentStorage;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import dev.lumenlang.lumen.plugin.commands.CommandRegistry;
import dev.lumenlang.lumen.plugin.commands.luma.LumaCommand;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import dev.lumenlang.lumen.plugin.defaults.type.BuiltinTypeBindings;
import dev.lumenlang.lumen.plugin.documentation.DocumentationDumper;
import dev.lumenlang.lumen.plugin.platform.ServerPlatform;
import dev.lumenlang.lumen.plugin.scanner.RegistrationScannerBackend;
import dev.lumenlang.lumen.plugin.scheduler.ScriptScheduler;
import dev.lumenlang.lumen.plugin.scripts.ExampleCopier;
import dev.lumenlang.lumen.plugin.scripts.ScriptManager;
import dev.lumenlang.lumen.plugin.scripts.ScriptSourceLoader;
import dev.lumenlang.lumen.plugin.scripts.ScriptWatcher;
import dev.lumenlang.lumen.plugin.util.BukkitValueResolver;
import net.vansencool.lsyaml.binding.watcher.ConfigWatcher;
import net.vansencool.lsyaml.logger.JulLogAdapter;
import net.vansencool.lsyaml.logger.LSYAMLLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

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
        setupSystemCompiler();
        LumaCommand.register();
        if (LumenConfiguration.SCRIPTS.ENABLE_ALL_SCRIPTS_IMMEDIATELY_ON_STARTUP) {
            loadScripts();
            scriptWatcher = new ScriptWatcher();
            scriptWatcher.start(ScriptSourceLoader.scriptsDir());
            if (LumenConfiguration.EXTRA.ENABLE_DOCUMENTATION_TOOL) DocumentationDumper.dump(patternRegistry);
        } else {
            Bukkit.getScheduler().runTask(this, () -> {
                loadScripts();
                scriptWatcher = new ScriptWatcher();
                scriptWatcher.start(ScriptSourceLoader.scriptsDir());
                if (LumenConfiguration.EXTRA.ENABLE_DOCUMENTATION_TOOL) DocumentationDumper.dump(patternRegistry);
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
        ScriptManager.shutdownPool();
        RegistrationScanner.teardown();
        ScriptBinder.teardown();
        LumenProvider.teardown();
        if (addonManager != null) {
            addonManager.disableAll();
        }
        LumaCommand.unregister();
        CommandRegistry.unregisterAll();
        ScriptScheduler.cancelAllGlobal();
        PersistentVars.shutdown();
        lumenApi = null;
    }

    /**
     * Initialises the Lumen API: creates registries, scans all built-in defaults,
     * loads jar-based addons, enables them, and exposes the API via
     * {@link LumenProvider} so that other plugins can access it.
     */
    private void initApi() {
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

        PersistentVars.init(new FilePersistentStorage(getDataFolder().toPath().resolve("persist.dat")));
        PersistentVars.setValueResolver(BukkitValueResolver.INSTANCE);

        addonManager = new AddonManager();
        LumenProvider.init(lumenApi, addonManager::registerAddon);
        File addonsDir = new File(getDataFolder(), "addons");
        if (!addonsDir.exists() && !addonsDir.mkdirs()) throw new RuntimeException("Failed to create addons directory");
        addonManager.loadAddons(addonsDir);

        RegistrationScanner.init(new RegistrationScannerBackend(lumenApi));
        RegistrationScanner.scan("dev.lumenlang.lumen.plugin.defaults");
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
                LumenLogger.info("Addon " + addon.name() + " v" + addon.version() +
                        " is " + (override.value() ? "enabling" : "disabling") + " " + override.option().path() + ": " + override.reason());
                LumenConfiguration.applyOverride(override);
            }
        }
    }

    private void platformCheck() {
        if (ServerPlatform.isFolia()) {
            this.getLogger().warning("Folia detected. Folia is currently not officially supported, features such as entity, schedules, and additional issues may occur. Use at your own risk.");
            LumenConfiguration.applyOverride(ConfigOverride.disable(ConfigOption.ENABLE_ALL_SCRIPTS_IMMEDIATELY)
                    .lastingSession("Disabling ENABLE_ALL_SCRIPTS_IMMEDIATELY will make the code use runTask, which is not supported on Folia."));
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

    private void setupSystemCompiler() {
        SystemCompiler.setReduceClasspath(LumenConfiguration.PERFORMANCE.REDUCE_CLASSPATH);
        if (LumenConfiguration.PERFORMANCE.WARMUP_ON_STARTUP) {
            Thread warmup = new Thread(ScriptManager::warmup, "Lumen-Warmup");
            warmup.setDaemon(true);
            warmup.start();
        }
    }
}
