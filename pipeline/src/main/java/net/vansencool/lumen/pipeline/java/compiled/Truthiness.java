package net.vansencool.lumen.pipeline.java.compiled;

import org.jetbrains.annotations.Nullable;

/**
 * Runtime utility for evaluating the truthiness of arbitrary values.
 *
 * <p>Used by generated script code when a variable or config value is used directly
 * as a boolean condition (e.g. {@code if myConfigFlag:}).
 *
 * <p>Truthiness rules:
 * <ul>
 *   <li>{@code null} is falsy</li>
 *   <li>{@code Boolean} values are used directly</li>
 *   <li>{@code Number} values are truthy when non-zero</li>
 *   <li>{@code String} "true", "1", "yes", and "on" (case-insensitive) are truthy, everything else is falsy</li>
 *   <li>All other objects are truthy (non-null)</li>
 * </ul>
 */
public final class Truthiness {

    private Truthiness() {}

    /**
     * Evaluates the truthiness of the given value.
     *
     * @param value the value to check
     * @return {@code true} if the value is considered truthy
     */
    public static boolean check(@Nullable Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        String s = String.valueOf(value);
        return s.equalsIgnoreCase("true") || s.equals("1")
                || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("on");
    }
}
