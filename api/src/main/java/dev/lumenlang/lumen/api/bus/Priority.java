package dev.lumenlang.lumen.api.bus;

/**
 * Execution priority for event subscribers, mirroring Bukkit's
 * {@code EventPriority} ordering.
 *
 * <p>Subscribers are invoked from {@link #LOWEST} to {@link #MONITOR}.
 * {@link #MONITOR} subscribers should only observe the final outcome
 * and must not modify or cancel the event.
 */
public enum Priority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    MONITOR
}
