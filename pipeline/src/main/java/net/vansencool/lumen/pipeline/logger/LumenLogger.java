package net.vansencool.lumen.pipeline.logger;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized logging facility for Lumen with configurable output levels.
 *
 * <p>LumenLogger provides a unified interface for all Lumen logging with support for
 * configuration-based filtering. This allows users to control the verbosity of Lumen's
 * output through the config file.
 *
 * <p>Call {@link #init(Logger)} during plugin startup, then {@link #configure(boolean, boolean, boolean)}
 * once configuration is loaded.
 */
@SuppressWarnings("unused")
public final class LumenLogger {
    private static Logger logger;
    private static boolean fullDebug = false;
    private static boolean logInfo = true;
    private static boolean logWarnings = true;

    /**
     * Initializes the logger with the plugin's logger instance.
     *
     * <p>This must be called during plugin initialization before any logging occurs.
     *
     * @param pluginLogger the Bukkit plugin logger
     */
    public static void init(@NotNull Logger pluginLogger) {
        logger = pluginLogger;
    }

    /**
     * Configures the logger's output filtering flags.
     *
     * <p>Call this after loading the plugin configuration so that debug/info/warning
     * messages respect user settings.
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
     * <p>Debug messages are only output when {@code debug.full-debug = true} in the config.
     * These messages provide detailed information about Lumen's internal operations including:
     * <ul>
     *   <li>Pattern matching attempts and results</li>
     *   <li>Token consumption decisions</li>
     *   <li>Type binding parse operations</li>
     *   <li>Java code generation</li>
     * </ul>
     *
     * <p><b>Performance Note:</b> Debug messages are cheap when disabled - the check happens
     * before any string concatenation or formatting.
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
     * <p>Info messages are only output when {@code debug.log-info = true} in the config.
     * These messages include:
     * <ul>
     *   <li>Plugin initialization and startup time</li>
     *   <li>Script compilation statistics</li>
     *   <li>Feature availability warnings</li>
     *   <li>Configuration changes</li>
     * </ul>
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
     * <p>Warnings indicate potential issues that don't prevent operation but may cause
     * unexpected behavior. Output is suppressed when {@code debug.log-warnings = false}.
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
     * <p>Severe messages are always logged and indicate critical errors that prevent
     * normal operation.
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
     * <p>Severe messages are always logged and indicate critical errors that prevent
     * normal operation. The exception stack trace is included in the log.
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
