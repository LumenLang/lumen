package net.vansencool.lumen.plugin.configuration;

import net.vansencool.lsyaml.LSYAML;
import net.vansencool.lsyaml.binding.ConfigFile;
import net.vansencool.lsyaml.binding.ConfigLoader;
import net.vansencool.lsyaml.binding.Key;
import net.vansencool.lsyaml.binding.LatestConfig;
import net.vansencool.lsyaml.binding.watcher.ConfigWatcher;
import net.vansencool.lsyaml.binding.watcher.WatchAction;
import net.vansencool.lsyaml.binding.watcher.WatcherOptions;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lumen.pipeline.logger.LumenLogger;
import net.vansencool.lumen.plugin.Lumen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@ConfigFile("plugins/Lumen/config.yml")
@LatestConfig("config.yml")
public final class LumenConfiguration {

    public static Scripts SCRIPTS = new Scripts();
    public static Debug DEBUG = new Debug();
    public static Performance PERFORMANCE = new Performance();
    public static Language LANGUAGE = new Language();
    public static Features FEATURES = new Features();
    public static Extra EXTRA = new Extra();

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
        if (LumenConfiguration.EXTRA.ENABLE_CONFIG_FILE_WATCHER) ConfigWatcher.watch(LumenConfiguration.class, WatcherOptions.builder().listener(((file, action) -> {
            if (action == WatchAction.DELETED) LumenLogger.warning("Configuration file deleted! The plugin will continue using the old configuration.");
            else if (action == WatchAction.MODIFIED) LumenLogger.info("Reloading configuration...");
            else if (action == WatchAction.CREATED) LumenLogger.info("Configuration file created, loading...");
        })).build());
    }

    public static void disablePaperOnlyFeatures() {
        YamlNode node = ConfigLoader.node(LumenConfiguration.class);
        if (node == null) {
            throw new RuntimeException("Called disablePaperOnlyFeatures before lumen's startup");
        }
        MapNode featuresNode = Objects.requireNonNull(node.get("features"), "No 'features' section in the YAML file?").asMap();
        if (Boolean.FALSE.equals(featuresNode.getBoolean("paper-only-features"))) return;

        featuresNode.asMap().modify("paper-only-features").value(false);
        LSYAML.writeToFile(node, Path.of("plugins/Lumen/config.yml"));
    }

    public static final class Scripts {

        public String FOLDER = "scripts";

        public String EXTENSION = ".luma";

        @Key("load-all-on-startup")
        public boolean LOAD_ALL_ON_STARTUP = true;

        @Key("enable-all-scripts-immediately-on-startup")
        public boolean ENABLE_ALL_SCRIPTS_IMMEDIATELY_ON_STARTUP = false;

        @Key("reload-on-save")
        public boolean RELOAD_ON_SAVE = true;

        @Key("load-on-create")
        public boolean LOAD_ON_CREATE = true;

        @Key("unload-on-delete")
        public boolean UNLOAD_ON_DELETE = true;

        @Key("watcher-debounce-ms")
        public long WATCHER_DEBOUNCE_MS = 200;
    }

    public static final class Debug {

        @Key("log-warnings")
        public boolean LOG_WARNINGS = true;

        @Key("log-info")
        public boolean LOG_INFO = true;

        @Key("log-compilation")
        public boolean LOG_COMPILATION = false;

        @Key("dump-generated-java")
        public boolean DUMP_GENERATED_JAVA = false;

        @Key("full-debug")
        public boolean FULL_DEBUG = false;
    }

    public static final class Performance {

        @Key("cache-compiled-classes")
        public boolean CACHE_COMPILED_CLASSES = true;

        @Key("load-scripts-async-on-startup")
        public boolean LOAD_SCRIPTS_ASYNC_ON_STARTUP = false;

        @Key("warmup-on-startup")
        public boolean WARMUP_ON_STARTUP = true;

        @Key("reduce-classpath")
        public boolean REDUCE_CLASSPATH = false;
    }

    public static final class Language {

        public Experimental EXPERIMENTAL = new Experimental();

        public static final class Experimental {

            @Key("raw-java")
            public boolean RAW_JAVA = false;
        }
    }

    public static final class Features {

        @Key("use-legacy-colors")
        public boolean USE_LEGACY_COLORS = true;

        @Key("paper-only-features")
        public boolean PAPER_ONLY_FEATURES = true;
    }

    public static final class Extra {

        @Key("enable-config-file-watcher")
        public boolean ENABLE_CONFIG_FILE_WATCHER = true;

        @Key("enable-documentation-tool")
        public boolean ENABLE_DOCUMENTATION_TOOL = false;
    }
}
