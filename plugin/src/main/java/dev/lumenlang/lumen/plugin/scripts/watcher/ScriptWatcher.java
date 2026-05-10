package dev.lumenlang.lumen.plugin.scripts.watcher;

import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.Lumen;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import dev.lumenlang.lumen.plugin.scripts.Scripts;
import dev.lumenlang.lumen.plugin.scripts.runtime.ScriptLifecycle;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Stream;

/**
 * Recursively watches the scripts directory for file changes.
 */
public final class ScriptWatcher {

    private final Map<WatchKey, Path> watchedDirs = new HashMap<>();

    private volatile boolean running;
    private Thread watchThread;
    private WatchService watchService;
    private Path rootDir;

    public void start(@NotNull Path scriptsDir) {
        if (!LumenConfiguration.SCRIPTS.RELOAD_ON_SAVE && !LumenConfiguration.SCRIPTS.LOAD_ON_CREATE && !LumenConfiguration.SCRIPTS.UNLOAD_ON_DELETE) {
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            rootDir = scriptsDir;
            registerTree(scriptsDir);
        } catch (IOException e) {
            LumenLogger.severe("Failed to start script file watcher: " + e.getMessage());
            return;
        }

        running = true;
        watchThread = new Thread(this::pollLoop, "Lumen-ScriptWatcher");
        watchThread.setDaemon(true);
        watchThread.start();

        LumenLogger.debug("ScriptWatcher", "Script file watcher started for: " + scriptsDir.toAbsolutePath());
    }

    public void stop() {
        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
        }
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
        watchedDirs.clear();
    }

    private void registerTree(@NotNull Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isDirectory).forEach(this::registerOne);
        }
    }

    private void registerOne(@NotNull Path dir) {
        if (rootDir != null) {
            for (Path part : rootDir.relativize(dir)) {
                if (part.toString().startsWith("-")) return;
            }
        }
        try {
            WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            watchedDirs.put(key, dir);
        } catch (IOException e) {
            LumenLogger.warning("[Watcher] Failed to register " + dir + ": " + e.getMessage());
        }
    }

    /**
     * Loads every {@code .luma} file already inside a freshly-created directory.
     * Files that the OS later fires CREATE events for are loaded by the normal path.
     */
    private void loadExistingFiles(@NotNull Path dir) {
        String extension = LumenConfiguration.SCRIPTS.EXTENSION;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(extension)).forEach(file -> {
                String relative = rootDir.relativize(file).toString().replace('\\', '/');
                for (Path part : Path.of(relative)) {
                    if (part.toString().startsWith("-")) return;
                }
                LumenLogger.info("[Watcher] Loading file from new directory: " + relative);
                scheduleLoad(file, relative);
            });
        } catch (IOException e) {
            LumenLogger.warning("[Watcher] Failed to scan new directory " + dir + ": " + e.getMessage());
        }
    }

    private static void scheduleLoad(@NotNull Path file, @NotNull String name) {
        try {
            if (!Files.isRegularFile(file)) return;
            String source = Files.readString(file);
            Scripts.load(name, source).exceptionally(t -> {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                if (cause instanceof LumenScriptException lse) {
                    if (lse.diagnostic() != null || lse.getMessage().contains("\n")) {
                        LumenLogger.severe("[Watcher] Script error in " + name + ":\n" + lse.getMessage());
                    } else {
                        LumenLogger.severe("[Watcher] Script error in " + name + ": " + lse.getMessage());
                    }
                } else {
                    LumenLogger.severe("[Watcher] Failed to load/reload script: " + name + " (" + cause.getMessage() + ")");
                }
                return null;
            });
        } catch (Throwable t) {
            LumenLogger.severe("[Watcher] Failed to load/reload script: " + name + " (" + t.getMessage() + ")");
        }
    }

    private void pollLoop() {
        String extension = LumenConfiguration.SCRIPTS.EXTENSION;

        while (running) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }

            LockSupport.parkNanos(LumenConfiguration.SCRIPTS.WATCHER_DEBOUNCE_MS * 1_000_000L);
            if (Thread.interrupted()) break;

            Path dir = watchedDirs.get(key);
            if (dir == null) {
                key.reset();
                continue;
            }

            try {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path absolute = dir.resolve(pathEvent.context());
                    String relative = rootDir.relativize(absolute).toString().replace('\\', '/');

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(absolute)) {
                        try {
                            registerTree(absolute);
                            loadExistingFiles(absolute);
                        } catch (IOException ignored) {
                        }
                        continue;
                    }

                    if (!relative.endsWith(extension)) continue;
                    boolean hidden = false;
                    for (Path part : Path.of(relative)) {
                        if (part.toString().startsWith("-")) {
                            hidden = true;
                            break;
                        }
                    }
                    if (hidden) continue;

                    try {
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE && LumenConfiguration.SCRIPTS.LOAD_ON_CREATE) {
                            LumenLogger.info("[Watcher] Detected new script: " + relative);
                            scheduleLoad(absolute, relative);
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY && LumenConfiguration.SCRIPTS.RELOAD_ON_SAVE) {
                            LumenLogger.info("[Watcher] Detected modification, reloading: " + relative);
                            scheduleLoad(absolute, relative);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE && LumenConfiguration.SCRIPTS.UNLOAD_ON_DELETE) {
                            if (ScriptLifecycle.isLoaded(relative)) {
                                LumenLogger.info("[Watcher] Detected deletion, unloading: " + relative);
                                Bukkit.getScheduler().runTask(Lumen.instance(), () -> ScriptLifecycle.unload(relative));
                            }
                        }
                    } catch (Throwable t) {
                        LumenLogger.severe("[Watcher] Error processing event for " + relative + ": " + t.getMessage());
                    }
                }
            } catch (Throwable t) {
                LumenLogger.severe("[Watcher] Error processing watch events: " + t.getMessage());
            }

            boolean valid = key.reset();
            if (!valid) {
                watchedDirs.remove(key);
                if (watchedDirs.isEmpty()) {
                    LumenLogger.warning("Script watcher lost access to all directories, stopping.");
                    break;
                }
            }
        }
    }
}
