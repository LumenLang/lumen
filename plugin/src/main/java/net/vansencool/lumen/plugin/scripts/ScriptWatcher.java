package net.vansencool.lumen.plugin.scripts;

import net.vansencool.lumen.pipeline.language.exceptions.LumenScriptException;
import net.vansencool.lumen.pipeline.logger.LumenLogger;
import net.vansencool.lumen.plugin.Lumen;
import net.vansencool.lumen.plugin.configuration.LumenConfiguration;
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
import java.util.concurrent.locks.LockSupport;

/**
 * Watches the scripts directory for file changes and automatically loads,
 * reloads,
 * or unloads scripts based on the configuration.
 *
 * <p>
 * Uses the Java {@link WatchService} API to receive filesystem notifications.
 * Runs on a daemon thread so it does not prevent JVM shutdown. Events are
 * debounced
 * by a short sleep to coalesce rapid successive writes from editors that save
 * in
 * multiple steps.
 *
 * <p>
 * Controlled by three config flags under {@code scripts}:
 * <ul>
 * <li>{@code reload-on-save} - reloads an already-loaded script when its file
 * is modified</li>
 * <li>{@code load-on-create} - loads a new script when a new file appears in
 * the directory</li>
 * <li>{@code unload-on-delete} - unloads a script when its file is deleted</li>
 * </ul>
 *
 * @see ScriptManager
 * @see LumenConfiguration.Scripts
 */
public final class ScriptWatcher {

    private volatile boolean running;
    private Thread watchThread;
    private WatchService watchService;

    private static void scheduleLoad(@NotNull Path scriptsDir, @NotNull String name) {
        try {
            Path file = scriptsDir.resolve(name);
            if (!Files.isRegularFile(file))
                return;
            String source = Files.readString(file);
            ScriptManager.load(name, source).exceptionally(t -> {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                if (cause instanceof LumenScriptException lse) {
                    LumenLogger.severe("[Watcher] Script error in " + name + ": " + lse.getMessage());
                } else {
                    LumenLogger.severe(
                            "[Watcher] Failed to load/reload script: " + name + " (" + cause.getMessage() + ")");
                }
                return null;
            });
        } catch (Throwable t) {
            LumenLogger.severe("[Watcher] Failed to load/reload script: " + name + " (" + t.getMessage() + ")");
        }
    }

    /**
     * Starts the file watcher if any of the watch-related config flags are enabled.
     *
     * <p>
     * Does nothing if none of {@code reload-on-save}, {@code load-on-create}, or
     * {@code unload-on-delete} are enabled.
     *
     * @param scriptsDir the directory to watch
     */
    public void start(@NotNull Path scriptsDir) {
        if (!LumenConfiguration.SCRIPTS.RELOAD_ON_SAVE
                && !LumenConfiguration.SCRIPTS.LOAD_ON_CREATE
                && !LumenConfiguration.SCRIPTS.UNLOAD_ON_DELETE) {
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            scriptsDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
        } catch (IOException e) {
            LumenLogger.severe("Failed to start script file watcher: " + e.getMessage());
            return;
        }

        running = true;
        watchThread = new Thread(() -> pollLoop(scriptsDir), "Lumen-ScriptWatcher");
        watchThread.setDaemon(true);
        watchThread.start();

        LumenLogger.debug("ScrptWatcher", "Script file watcher started for: " + scriptsDir.toAbsolutePath());
    }

    /**
     * Stops the file watcher and cleans up resources.
     */
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
    }

    private void pollLoop(@NotNull Path scriptsDir) {
        String extension = LumenConfiguration.SCRIPTS.EXTENSION;

        while (running) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }

            long debounceNanos = LumenConfiguration.SCRIPTS.WATCHER_DEBOUNCE_MS * 1_000_000L;
            LockSupport.parkNanos(debounceNanos);
            if (Thread.interrupted())
                break;

            try {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW)
                        continue;

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path fileName = pathEvent.context();
                    String name = fileName.toString();

                    if (!name.endsWith(extension))
                        continue;

                    try {
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE && LumenConfiguration.SCRIPTS.LOAD_ON_CREATE) {
                            LumenLogger.info("[Watcher] Detected new script: " + name);
                            scheduleLoad(scriptsDir, name);
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY
                                && LumenConfiguration.SCRIPTS.RELOAD_ON_SAVE) {
                            LumenLogger.info("[Watcher] Detected modification, reloading: " + name);
                            scheduleLoad(scriptsDir, name);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE
                                && LumenConfiguration.SCRIPTS.UNLOAD_ON_DELETE) {
                            if (ScriptManager.isLoaded(name)) {
                                LumenLogger.info("[Watcher] Detected deletion, unloading: " + name);
                                Bukkit.getScheduler().runTask(Lumen.instance(), () -> ScriptManager.unload(name));
                            }
                        }
                    } catch (Throwable t) {
                        LumenLogger.severe("[Watcher] Error handling events for " + name + ": " + t.getMessage());
                    }
                }
            } catch (Throwable t) {
                LumenLogger.severe("[Watcher] Error processing watch events: " + t.getMessage());
            }

            boolean valid = key.reset();
            if (!valid) {
                LumenLogger.warning("Script watcher lost access to directory, stopping.");
                break;
            }
        }
    }
}
