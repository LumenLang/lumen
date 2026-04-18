package dev.lumenlang.lumen.pipeline.logger;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized logging facility for Lumen with configurable output levels.
 */
@SuppressWarnings("unused")
public final class LumenLogger {
    private static Logger logger;
    private static boolean fullDebug = false;
    private static boolean logInfo = true;
    private static boolean logWarnings = true;

    /**
     * Returns whether full debug logging is enabled.
     *
     * @return true if full debug mode is active
     */
    public static boolean isFullDebug() {
        return fullDebug;
    }

    /**
     * Initializes the logger with the plugin's logger instance.
     *
     * @param pluginLogger the Bukkit plugin logger
     */
    public static void init(@NotNull Logger pluginLogger) {
        logger = pluginLogger;
    }

    /**
     * Configures the logger's output filtering flags.
     *
     * @param fullDebug   whether full debug logging is enabled
     * @param logInfo     whether informational messages are logged
     * @param logWarnings whether warning messages are logged
     */
    public static void configure(boolean fullDebug, boolean logInfo, boolean logWarnings) {
        LumenLogger.fullDebug = fullDebug;
        LumenLogger.logInfo = logInfo;
        LumenLogger.logWarnings = logWarnings;
    }

    /**
     * Logs a debug message if full debug mode is enabled.
     *
     * @param component the component or subsystem name
     * @param message   the debug message
     */
    public static void debug(String component, @NotNull String message) {
        if (!fullDebug || logger == null) {
            return;
        }
        logger.info("[DEBUG] [" + component + "] " + message);
    }

    /**
     * Logs an informational message if info logging is enabled.
     *
     * @param message the informational message
     */
    public static void info(String message) {
        if (!logInfo || logger == null) {
            return;
        }
        logger.info(message);
    }

    /**
     * Logs a warning message if warning logging is enabled.
     *
     * @param message the warning message
     */
    public static void warning(String message) {
        if (!logWarnings || logger == null) {
            return;
        }
        logger.warning(message);
    }

    /**
     * Logs a severe error message.
     *
     * @param message the error message
     */
    public static void severe(String message) {
        if (logger == null) return;
        logger.severe(message);
    }

    /**
     * Logs a severe error message with an exception.
     *
     * @param message   the error message
     * @param throwable the exception to log
     */
    public static void severe(String message, Throwable throwable) {
        if (logger == null) return;
        logger.log(Level.SEVERE, message, throwable);
    }

    /**
     * Logs a message at the specified level.
     *
     * @param level   the logging level
     * @param message the log message
     */
    public static void log(Level level, String message) {
        if (logger == null) return;
        logger.log(level, message);
    }

    /**
     * Logs a message with an exception at the specified level.
     *
     * @param level     the logging level
     * @param message   the log message
     * @param throwable the exception to log
     */
    public static void log(Level level, String message, Throwable throwable) {
        if (logger == null) return;
        logger.log(level, message, throwable);
    }
}
