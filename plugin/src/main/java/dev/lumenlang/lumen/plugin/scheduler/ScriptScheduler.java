package dev.lumenlang.lumen.plugin.scheduler;

import dev.lumenlang.lumen.pipeline.java.compiled.LumenNullException;
import dev.lumenlang.lumen.pipeline.java.compiled.LumenRuntimeException;
import dev.lumenlang.lumen.pipeline.java.compiled.ScriptSourceMap;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.Lumen;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages Bukkit scheduled tasks for compiled Lumen scripts with support for
 * hot-reloading, named schedules, and per-schedule reload behavior.
 *
 * <h2>Named Schedules</h2>
 * <p>Each schedule can optionally have a name. Named schedules are scoped to the
 * script that created them, so different scripts can use the same schedule name
 * without conflicts. Named schedules can be cancelled at any time from within
 * the owning script via {@link #cancelByName(Object, String)}.
 *
 * <h2>Hot Reload</h2>
 * <p>When a script is reloaded, named schedules survive by default: the
 * underlying {@link BukkitTask} keeps running with its original timing, but
 * the code it executes is swapped to the newly compiled version. This is
 * called a "hot reload" and means zero downtime for repeating tasks.
 *
 * <h2>Per-Schedule Reload Flags</h2>
 * <ul>
 *   <li>{@code restartOnReload}: when {@code true}, the schedule is cancelled
 *       and recreated from scratch on reload instead of hot-swapping the code.
 *       The timing resets to its initial delay.</li>
 *   <li>{@code cancelOnReload}: when {@code true}, the schedule is stopped
 *       entirely on reload or unload and is not recreated.</li>
 * </ul>
 *
 * <h2>Unnamed Schedules</h2>
 * <p>Schedules without a name cannot be matched across reloads and are always
 * cancelled when the owning script is reloaded or unloaded.
 */
@SuppressWarnings("unused")
public final class ScriptScheduler {

    private static final Map<String, Map<String, ScheduleEntry>> namedSchedules = new ConcurrentHashMap<>();
    private static final Map<String, List<BukkitTask>> unnamedTasks = new ConcurrentHashMap<>();

    /**
     * Schedules a one-shot delayed task without a name.
     *
     * <p>Unnamed tasks are cancelled on reload or unload. They can also be
     * cancelled from within the task body by using the cancellable overload
     * {@link #schedule(Object, Consumer, long)}.
     *
     * @param callerInstance the script instance (used to derive the class name)
     * @param runnable       the task body
     * @param delayTicks     the delay in server ticks before the task runs
     */
    public static void schedule(@NotNull Object callerInstance, @NotNull Runnable runnable, long delayTicks) {
        String className = callerInstance.getClass().getName();
        BukkitTask[] holder = new BukkitTask[1];
        BukkitTask task = Bukkit.getScheduler().runTaskLater(Lumen.instance(), () -> {
            try {
                runSafely(className, runnable);
            } finally {
                removeUnnamedTask(className, holder[0]);
            }
        }, delayTicks);
        holder[0] = task;
        trackUnnamedTask(className, task);
    }

    /**
     * Schedules a one-shot delayed task without a name, providing a cancel callback.
     *
     * <p>The body receives a {@link Runnable} that, when called, cancels the
     * underlying Bukkit task. This allows the script to cancel the schedule
     * from within its own body.
     *
     * @param callerInstance the script instance
     * @param body           consumer receiving a cancel callback
     * @param delayTicks     the delay in server ticks before the task runs
     */
    public static void schedule(@NotNull Object callerInstance, @NotNull Consumer<Runnable> body, long delayTicks) {
        String className = callerInstance.getClass().getName();
        BukkitTask[] holder = new BukkitTask[1];
        Runnable cancel = () -> {
            BukkitTask t = holder[0];
            if (t != null && !t.isCancelled()) {
                t.cancel();
                removeUnnamedTask(className, t);
            }
        };
        BukkitTask task = Bukkit.getScheduler().runTaskLater(Lumen.instance(), () -> {
            try {
                runSafely(className, () -> body.accept(cancel));
            } finally {
                removeUnnamedTask(className, holder[0]);
            }
        }, delayTicks);
        holder[0] = task;
        trackUnnamedTask(className, task);
    }

    /**
     * Schedules a repeating task without a name.
     *
     * <p>Unnamed tasks are cancelled on reload or unload. They can also be
     * cancelled from within the task body by using the cancellable overload
     * {@link #scheduleRepeating(Object, Consumer, long, long)}.
     *
     * @param callerInstance the script instance (used to derive the class name)
     * @param runnable       the task body
     * @param delayTicks     the initial delay in server ticks
     * @param periodTicks    the interval in server ticks between executions
     */
    public static void scheduleRepeating(@NotNull Object callerInstance, @NotNull Runnable runnable,
                                         long delayTicks, long periodTicks) {
        String className = callerInstance.getClass().getName();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Lumen.instance(),
                () -> runSafely(className, runnable), delayTicks, periodTicks);
        trackUnnamedTask(className, task);
    }

    /**
     * Schedules a repeating task without a name, providing a cancel callback.
     *
     * <p>The body receives a {@link Runnable} that, when called, cancels the
     * underlying Bukkit task. This allows the script to cancel a repeating
     * schedule from within its own body.
     *
     * @param callerInstance the script instance
     * @param body           consumer receiving a cancel callback
     * @param delayTicks     the initial delay in server ticks
     * @param periodTicks    the interval in server ticks between executions
     */
    public static void scheduleRepeating(@NotNull Object callerInstance,
                                         @NotNull Consumer<Runnable> body,
                                         long delayTicks, long periodTicks) {
        String className = callerInstance.getClass().getName();
        BukkitTask[] holder = new BukkitTask[1];
        Runnable cancel = () -> {
            BukkitTask t = holder[0];
            if (t != null && !t.isCancelled()) {
                t.cancel();
                removeUnnamedTask(className, t);
            }
        };
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Lumen.instance(), () -> runSafely(className, () -> body.accept(cancel)), delayTicks, periodTicks);
        holder[0] = task;
        trackUnnamedTask(className, task);
    }

    /**
     * Schedules a one-shot delayed task with a name, supporting hot-reload.
     *
     * <p>If a schedule with the same name already exists for this script and
     * has not yet fired:
     * <ul>
     *   <li>If the existing entry has {@code restartOnReload = true}, the old
     *       task is cancelled and a new one is created from scratch.</li>
     *   <li>Otherwise, the code is hot-swapped: the existing task keeps its
     *       remaining delay but will execute the new code when it fires.</li>
     * </ul>
     *
     * @param callerInstance  the script instance
     * @param name            the schedule name (unique within this script)
     * @param runnable        the task body
     * @param delayTicks      the delay in server ticks
     * @param restartOnReload if {@code true}, restart from scratch on reload
     * @param cancelOnReload  if {@code true}, cancel entirely on reload/unload
     */
    public static void scheduleNamed(@NotNull Object callerInstance, @NotNull String name,
                                     @NotNull Runnable runnable, long delayTicks,
                                     boolean restartOnReload, boolean cancelOnReload) {
        String className = callerInstance.getClass().getName();
        Map<String, ScheduleEntry> scriptSchedules = namedSchedules.computeIfAbsent(
                className, k -> new ConcurrentHashMap<>());

        ScheduleEntry existing = scriptSchedules.get(name);
        if (existing != null && !existing.task.isCancelled()) {
            if (existing.restartOnReload) {
                existing.task.cancel();
            } else {
                existing.code.swap(runnable);
                scriptSchedules.put(name, new ScheduleEntry(
                        name, existing.code, existing.task,
                        existing.delayTicks, existing.periodTicks,
                        restartOnReload, cancelOnReload, false));
                return;
            }
        }

        HotSwappableRunnable wrapper = new HotSwappableRunnable(runnable);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(Lumen.instance(), () -> {
            try {
                runSafely(className, wrapper);
            } finally {
                scriptSchedules.remove(name);
            }
        }, delayTicks);
        scriptSchedules.put(name, new ScheduleEntry(
                name, wrapper, task, delayTicks, 0,
                restartOnReload, cancelOnReload, false));
    }

    /**
     * Schedules a repeating task with a name, supporting hot-reload.
     *
     * <p>If a schedule with the same name already exists for this script:
     * <ul>
     *   <li>If the existing entry has {@code restartOnReload = true}, the old
     *       task is cancelled and a new one is created with fresh timing.</li>
     *   <li>If the interval (period) has not changed, the code is hot-swapped:
     *       the existing timer keeps its current phase but will execute the
     *       new code on the next tick.</li>
     *   <li>If the interval changed, the old task is cancelled and a one-shot
     *       "bridge" task is scheduled for the remaining ticks of the current
     *       cycle. When the bridge fires, it executes the body once and then
     *       starts a new repeating task with the updated interval. If another
     *       reload changes the interval again during the bridge, the target
     *       period is updated in place and the bridge reads the latest value
     *       when it fires.</li>
     * </ul>
     *
     * @param callerInstance  the script instance
     * @param name            the schedule name (unique within this script)
     * @param runnable        the task body
     * @param delayTicks      the initial delay in server ticks
     * @param periodTicks     the interval in server ticks between executions
     * @param restartOnReload if {@code true}, restart from scratch on reload
     * @param cancelOnReload  if {@code true}, cancel entirely on reload/unload
     */
    public static void scheduleRepeatingNamed(@NotNull Object callerInstance, @NotNull String name,
                                              @NotNull Runnable runnable,
                                              long delayTicks, long periodTicks,
                                              boolean restartOnReload, boolean cancelOnReload) {
        String className = callerInstance.getClass().getName();
        Map<String, ScheduleEntry> scriptSchedules = namedSchedules.computeIfAbsent(
                className, k -> new ConcurrentHashMap<>());

        ScheduleEntry existing = scriptSchedules.get(name);
        if (existing != null && !existing.task.isCancelled()) {
            if (existing.restartOnReload) {
                existing.task.cancel();
            } else if (existing.transitioning || periodTicks == existing.periodTicks) {
                existing.code.swap(runnable);
                scriptSchedules.put(name, new ScheduleEntry(
                        name, existing.code, existing.task,
                        existing.delayTicks, periodTicks,
                        restartOnReload, cancelOnReload, existing.transitioning));
                return;
            } else {
                existing.task.cancel();
                existing.code.swap(runnable);
                long remaining = computeRemainingTicks(existing);
                HotSwappableRunnable code = existing.code;
                BukkitTask bridge = Bukkit.getScheduler().runTaskLater(Lumen.instance(), () -> {
                    runSafely(className, code);
                    ScheduleEntry current = scriptSchedules.get(name);
                    if (current == null || !current.transitioning) return;
                    long targetPeriod = current.periodTicks;
                    BukkitTask repeater = Bukkit.getScheduler().runTaskTimer(
                            Lumen.instance(), () -> runSafely(className, code),
                            targetPeriod, targetPeriod);
                    scriptSchedules.put(name, new ScheduleEntry(
                            name, code, repeater,
                            targetPeriod, targetPeriod,
                            current.restartOnReload, current.cancelOnReload, false));
                }, remaining);
                scriptSchedules.put(name, new ScheduleEntry(
                        name, code, bridge,
                        remaining, periodTicks,
                        restartOnReload, cancelOnReload, true));
                return;
            }
        }

        HotSwappableRunnable wrapper = new HotSwappableRunnable(runnable);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                Lumen.instance(), () -> runSafely(className, wrapper), delayTicks, periodTicks);
        scriptSchedules.put(name, new ScheduleEntry(
                name, wrapper, task, delayTicks, periodTicks,
                restartOnReload, cancelOnReload, false));
    }

    /**
     * Computes how many ticks remain in the current cycle for a repeating schedule.
     *
     * <p>Uses the entry's {@link HotSwappableRunnable#lastRunNanos()} to determine
     * elapsed time since the last execution, then subtracts from the period. If the
     * task has not yet executed, falls back to the entry's delay as remaining time.
     *
     * @param entry the schedule entry to compute remaining ticks for
     * @return remaining ticks until the next execution, at least 1
     */
    private static long computeRemainingTicks(@NotNull ScheduleEntry entry) {
        if (!entry.code.hasRun()) {
            return Math.max(1, entry.delayTicks);
        }
        long elapsedNanos = System.nanoTime() - entry.code.lastRunNanos();
        long elapsedTicks = elapsedNanos / 50_000_000L;
        return Math.max(1, entry.periodTicks - elapsedTicks);
    }

    /**
     * Cancels a named schedule belonging to the calling script.
     *
     * <p>This has no effect if no schedule with the given name exists or
     * if it has already been cancelled.
     *
     * @param callerInstance the script instance
     * @param name           the schedule name to cancel
     */
    public static void cancelByName(@NotNull Object callerInstance, @NotNull String name) {
        String className = callerInstance.getClass().getName();
        Map<String, ScheduleEntry> scriptSchedules = namedSchedules.get(className);
        if (scriptSchedules == null) return;
        ScheduleEntry entry = scriptSchedules.remove(name);
        if (entry != null && !entry.task.isCancelled()) {
            entry.task.cancel();
        }
    }

    /**
     * Handles a script being fully unloaded (not coming back).
     *
     * <p>All named and unnamed tasks are cancelled unconditionally.
     *
     * @param className the fully qualified class name of the script
     */
    public static void handleUnload(@NotNull String className) {
        cancelAllNamed(className);
        cancelAllUnnamed(className);
    }

    /**
     * Handles a script being reloaded (new version incoming).
     *
     * <p>Named schedules marked with {@code cancelOnReload} are cancelled and removed.
     * All other named schedules are preserved for hot-swapping when the new
     * script code re-registers them. Unnamed schedules are always cancelled
     * since they cannot be matched across reloads.
     *
     * @param className the fully qualified class name of the script
     */
    public static void handleReload(@NotNull String className) {
        Map<String, ScheduleEntry> scriptSchedules = namedSchedules.get(className);
        if (scriptSchedules != null) {
            scriptSchedules.entrySet().removeIf(entry -> {
                ScheduleEntry se = entry.getValue();
                if (se.cancelOnReload) {
                    if (!se.task.isCancelled()) se.task.cancel();
                    return true;
                }
                se.code.clearDelegate();
                return false;
            });
        }
        cancelAllUnnamed(className);
    }

    /**
     * Cancels all tracked tasks across every script. Called on plugin disable.
     */
    public static void cancelAllGlobal() {
        for (Map<String, ScheduleEntry> map : namedSchedules.values()) {
            for (ScheduleEntry se : map.values()) {
                if (!se.task.isCancelled()) se.task.cancel();
            }
        }
        namedSchedules.clear();

        for (List<BukkitTask> list : unnamedTasks.values()) {
            for (BukkitTask task : list) {
                if (!task.isCancelled()) task.cancel();
            }
        }
        unnamedTasks.clear();
    }

    private static void cancelAllNamed(@NotNull String className) {
        Map<String, ScheduleEntry> scriptSchedules = namedSchedules.remove(className);
        if (scriptSchedules == null) return;
        for (ScheduleEntry entry : scriptSchedules.values()) {
            if (!entry.task.isCancelled()) entry.task.cancel();
        }
    }

    private static void cancelAllUnnamed(@NotNull String className) {
        List<BukkitTask> tasks = unnamedTasks.remove(className);
        if (tasks == null) return;
        for (BukkitTask task : tasks) {
            if (!task.isCancelled()) task.cancel();
        }
    }

    private static void trackUnnamedTask(@NotNull String className, @NotNull BukkitTask task) {
        unnamedTasks.computeIfAbsent(className, k -> new ArrayList<>()).add(task);
    }

    private static void removeUnnamedTask(@NotNull String className, @NotNull BukkitTask task) {
        List<BukkitTask> pending = unnamedTasks.get(className);
        if (pending == null) return;
        synchronized (pending) {
            pending.remove(task);
            if (pending.isEmpty()) {
                unnamedTasks.remove(className);
            }
        }
    }

    /**
     * Executes a task body with error handling, catching and logging script
     * runtime errors cleanly instead of dumping full Java stack traces.
     *
     * @param scriptClassName the fully qualified script class name
     * @param task            the task body to execute
     */
    private static void runSafely(@NotNull String scriptClassName, @NotNull Runnable task) {
        try {
            task.run();
        } catch (LumenRuntimeException lre) {
            logSchedulerException(scriptClassName, lre);
        } catch (Throwable t) {
            Throwable cause = t instanceof RuntimeException && t.getCause() != null ? t.getCause() : t;
            if (cause instanceof LumenRuntimeException lre) {
                logSchedulerException(scriptClassName, lre);
            } else {
                String simpleName = scriptClassName.contains(".")
                        ? scriptClassName.substring(scriptClassName.lastIndexOf('.') + 1)
                        : scriptClassName;
                ScriptSourceMap.ScriptLineMapping mapping = ScriptSourceMap.findFromException(cause);
                if (cause instanceof LumenNullException lne) {
                    LumenLogger.severe("[Script " + simpleName + "] NullPointerException in scheduled task");
                    logSourceMapping(mapping);
                    LumenLogger.severe("  -> '" + lne.scriptVarName() + "' was null. Use 'if " + lne.scriptVarName() + " is set:' to guard it.");
                } else if (cause instanceof NullPointerException npe) {
                    LumenLogger.severe("[Script " + simpleName + "] NullPointerException in scheduled task");
                    logSourceMapping(mapping);
                    LumenLogger.severe("  -> " + ScriptSourceMap.formatNpeHint(npe));
                } else {
                    String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                    LumenLogger.severe("[Script " + simpleName + "] Runtime error in scheduled task: " + message);
                    logSourceMapping(mapping);
                }
            }
        }
    }

    private static void logSchedulerException(@NotNull String scriptClassName, @NotNull LumenRuntimeException lre) {
        String simpleName = scriptClassName.contains(".")
                ? scriptClassName.substring(scriptClassName.lastIndexOf('.') + 1)
                : scriptClassName;
        ScriptSourceMap.ScriptLineMapping mapping = lre.scriptLine() > 0
                ? null
                : ScriptSourceMap.findFromException(lre);
        LumenLogger.severe("[Script " + simpleName + "] " + lre.getMessage());
        logSourceMapping(mapping);
    }

    private static void logSourceMapping(@Nullable ScriptSourceMap.ScriptLineMapping mapping) {
        if (mapping == null) return;
        LumenLogger.severe("  Script line " + mapping.scriptLine() + ": " + mapping.scriptSource());
        LumenLogger.severe("  Java line: " + mapping.javaLine());
    }

    /**
     * Internal entry tracking a single named schedule, its underlying task,
     * and the hot-swappable code wrapper.
     */
    private record ScheduleEntry(@NotNull String name, @NotNull HotSwappableRunnable code, @NotNull BukkitTask task,
                                 long delayTicks, long periodTicks, boolean restartOnReload, boolean cancelOnReload,
                                 boolean transitioning) {
    }

    /**
     * A {@link Runnable} wrapper whose delegate can be atomically swapped.
     * This allows hot-reloading: the {@link BukkitTask} keeps running, but
     * the code it executes is replaced with the newly compiled version.
     */
    private static final class HotSwappableRunnable implements Runnable {

        private volatile @NotNull Runnable delegate;
        private volatile long lastRunNanos;

        private HotSwappableRunnable(@NotNull Runnable delegate) {
            this.delegate = delegate;
        }

        private void swap(@NotNull Runnable newDelegate) {
            this.delegate = newDelegate;
        }

        private void clearDelegate() {
            this.delegate = () -> {};
        }

        private boolean hasRun() {
            return lastRunNanos != 0;
        }

        private long lastRunNanos() {
            return lastRunNanos;
        }

        @Override
        public void run() {
            lastRunNanos = System.nanoTime();
            delegate.run();
        }
    }
}
