package dev.lumenlang.lumen.plugin.configuration;

import dev.lumenlang.lumen.api.ConfigOption;
import dev.lumenlang.lumen.api.ConfigOverride;
import dev.lumenlang.lumen.api.StringConfigOption;
import dev.lumenlang.lumen.api.StringConfigOverride;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.Lumen;
import net.vansencool.lsyaml.LSYAML;
import net.vansencool.lsyaml.binding.ConfigFile;
import net.vansencool.lsyaml.binding.ConfigLoader;
import net.vansencool.lsyaml.binding.Ignore;
import net.vansencool.lsyaml.binding.LatestConfig;
import net.vansencool.lsyaml.binding.PreferKeysWith;
import net.vansencool.lsyaml.binding.watcher.ConfigWatcher;
import net.vansencool.lsyaml.binding.watcher.WatchAction;
import net.vansencool.lsyaml.binding.watcher.WatcherOptions;
import net.vansencool.lsyaml.node.MapNode;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * This uses LSYAML's binding system to load configuration values from a YAML file into static fields.
 * <p>
 * Doing static fields allows the codebase to be cleaner, and more readable than constantly calling getters on a config instance, while being faster.
 * View more at {@link ConfigLoader}
 */
@ConfigFile("plugins/Lumen/config.yml")
@LatestConfig("config.yml")
@PreferKeysWith("-")
public final class LumenConfiguration {

    public static Scripts SCRIPTS = new Scripts();
    public static Debug DEBUG = new Debug();
    public static Performance PERFORMANCE = new Performance();
    public static Language LANGUAGE = new Language();
    public static Features FEATURES = new Features();
    public static Extra EXTRA = new Extra();

    @Ignore
    private static final List<ConfigOverride> lastingOverrides = new ArrayList<>();

    @Ignore
    private static final List<StringConfigOverride> lastingStringOverrides = new ArrayList<>();

    public static void load() {
        if (!Files.exists(Lumen.instance().getDataFolder().toPath().resolve("config.yml"))) {
            Lumen.instance().saveResource("config.yml", false);
        }
        ConfigLoader.disableAutoWatching();
        ConfigLoader.load(LumenConfiguration.class);
        LumenLogger.configure(
                LumenConfiguration.DEBUG.FULL_DEBUG,
                LumenConfiguration.DEBUG.LOG_INFO,
                LumenConfiguration.DEBUG.LOG_WARNINGS
        );
        LumenDiagnostic.configureVerbose(LumenConfiguration.DEBUG.VERBOSE_DIAGNOSTICS);
        if (LumenConfiguration.EXTRA.ENABLE_CONFIG_FILE_WATCHER)
            ConfigWatcher.watch(LumenConfiguration.class, WatcherOptions.builder().listener(((file, action) -> {
                if (action == WatchAction.DELETED)
                    LumenLogger.warning("Configuration file deleted! The plugin will continue using the old configuration.");
                else if (action == WatchAction.MODIFIED) {
                    LumenLogger.info("Reloading configuration...");
                    reapplyLastingOverrides();
                } else if (action == WatchAction.CREATED) {
                    LumenLogger.info("Configuration file created, loading...");
                    reapplyLastingOverrides();
                }
            })).build());
    }

    /**
     * Applies a {@link ConfigOverride} to the running configuration.
     *
     * <p>For {@link ConfigOverride.Persistence#SESSION}, the value is set in memory only.
     * For {@link ConfigOverride.Persistence#LASTING_SESSION}, it is set in memory and
     * re-applied automatically after configuration reloads.
     * For {@link ConfigOverride.Persistence#PERMANENT}, it is written to {@code config.yml}.
     *
     * @param override the override to apply
     */
    public static void applyOverride(@NotNull ConfigOverride override) {
        applyInMemory(override.option(), override.value());
        if (override.persistence() == ConfigOverride.Persistence.LASTING_SESSION) {
            lastingOverrides.add(override);
        } else if (override.persistence() == ConfigOverride.Persistence.PERMANENT) {
            writeOption(override.option(), override.value());
        }
    }

    private static void reapplyLastingOverrides() {
        for (ConfigOverride override : lastingOverrides) {
            applyInMemory(override.option(), override.value());
        }
        for (StringConfigOverride override : lastingStringOverrides) {
            applyInMemory(override.option(), override.value());
        }
    }

    /**
     * Applies a configuration option value in memory without writing to disk.
     *
     * @param option the config option to change
     * @param value  the desired value
     */
    public static void applyInMemory(@NotNull ConfigOption option, boolean value) {
        switch (option) {
            case PAPER_ONLY_FEATURES -> FEATURES.PAPER_ONLY_FEATURES = value;
            case REDUCE_CLASSPATH -> PERFORMANCE.REDUCE_CLASSPATH = value;
            case ENABLE_ALL_SCRIPTS_IMMEDIATELY -> SCRIPTS.ENABLE_ALL_SCRIPTS_IMMEDIATELY_ON_STARTUP = value;
            case CODE_TRANSFORM -> LANGUAGE.EXPERIMENTAL.CODE_TRANSFORM = value;
            case RAW_JAVA -> LANGUAGE.EXPERIMENTAL.RAW_JAVA = value;
            case INVENTORY_HOT_RELOAD -> FEATURES.INVENTORIES.HOT_RELOAD = value;
        }
    }

    /**
     * Applies a {@link StringConfigOverride} to the running configuration.
     *
     * @param override the override to apply
     */
    public static void applyOverride(@NotNull StringConfigOverride override) {
        applyInMemory(override.option(), override.value());
        if (override.persistence() == ConfigOverride.Persistence.LASTING_SESSION) {
            lastingStringOverrides.add(override);
        } else if (override.persistence() == ConfigOverride.Persistence.PERMANENT) {
            writeOption(override.option(), override.value());
        }
    }

    /**
     * Applies a string configuration option value in memory without writing to disk.
     *
     * @param option the config option to change
     * @param value  the desired value
     */
    public static void applyInMemory(@NotNull StringConfigOption option, @NotNull String value) {
        if (option == StringConfigOption.COMPILER) PERFORMANCE.COMPILER = value;
    }

    /**
     * Applies a string configuration option value in memory and writes it to {@code config.yml}.
     *
     * @param option the config option to change
     * @param value  the desired value
     */
    public static void writeOption(@NotNull StringConfigOption option, @NotNull String value) {
        applyInMemory(option, value);
        MapNode node = ConfigLoader.node(LumenConfiguration.class);
        if (node == null) throw new RuntimeException("writeOption called before startup");
        node.setString(option.path(), value);
        LSYAML.writeToFile(node, Lumen.instance().getDataFolder().toPath().resolve("config.yml"));
    }

    /**
     * Applies a configuration option value in memory and writes it to {@code config.yml}.
     *
     * @param option the config option to change
     * @param value  the desired value
     */
    public static void writeOption(@NotNull ConfigOption option, boolean value) {
        applyInMemory(option, value);
        MapNode node = ConfigLoader.node(LumenConfiguration.class);
        if (node == null) {
            throw new RuntimeException("writeOption called before startup");
        }
        if (Boolean.valueOf(value).equals(node.getBoolean(option.path()))) return;
        node.setBoolean(option.path(), value);
        LSYAML.writeToFile(node, Lumen.instance().getDataFolder().toPath().resolve("config.yml"));
    }

    public static final class Scripts {

        public String FOLDER = "scripts";

        public String EXTENSION = ".luma";

        public boolean LOAD_ALL_ON_STARTUP = true;

        public boolean ENABLE_ALL_SCRIPTS_IMMEDIATELY_ON_STARTUP = false;

        public boolean RELOAD_ON_SAVE = true;

        public boolean LOAD_ON_CREATE = true;

        public boolean UNLOAD_ON_DELETE = true;

        public long WATCHER_DEBOUNCE_MS = 200;
    }

    public static final class Debug {

        public boolean LOG_WARNINGS = true;

        public boolean LOG_INFO = true;

        public boolean LOG_COMPILATION = false;

        public boolean DUMP_GENERATED_JAVA = false;

        public boolean FULL_DEBUG = false;

        public boolean VERBOSE_DIAGNOSTICS = false;
    }

    public static final class Performance {

        public String COMPILER = "auto";

        public boolean CACHE_COMPILED_CLASSES = true;

        public boolean LOAD_SCRIPTS_ASYNC_ON_STARTUP = false;

        public boolean WARMUP_ON_STARTUP = true;

        public int COMPILE_THREADS = 2;

        public boolean REDUCE_CLASSPATH = false;

        public boolean ASYNC_DEFINE_CLASS = true;

        public int PARALLEL_PARSE_THREADS = 3;
    }

    public static final class Language {

        public Experimental EXPERIMENTAL = new Experimental();

        public static final class Experimental {

            public boolean RAW_JAVA = false;

            public boolean CODE_TRANSFORM = false;
        }
    }

    public static final class Features {

        public boolean USE_LEGACY_COLORS = true;

        public boolean PAPER_ONLY_FEATURES = true;

        public Inventories INVENTORIES = new Inventories();

        public static final class Inventories {

            public boolean HOT_RELOAD = true;
        }
    }

    public static final class Extra {

        public boolean ENABLE_CONFIG_FILE_WATCHER = true;

        public boolean ENABLE_DOCUMENTATION_TOOL = false;
    }
}
