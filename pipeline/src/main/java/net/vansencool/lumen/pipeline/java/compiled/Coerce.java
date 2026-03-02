package net.vansencool.lumen.pipeline.java.compiled;

/**
 * Provides runtime type coercion for generated script classes.
 *
 * <p>
 * Used by the {@code set x to y} statement to allow reassignment across
 * compatible types at runtime.
 */
@SuppressWarnings("unused") // Used inside of generated code
public final class Coerce {

    private Coerce() {
    }

    /**
     * Coerces a value to match the type of the current variable value at runtime.
     *
     * <p>
     * This is used by the {@code set x to y} statement to allow reassignment
     * across compatible types. For example, setting a {@code String} variable to an
     * {@code int} will automatically convert via {@link String#valueOf(Object)}.
     *
     * @param value   the new value to assign
     * @param current the current value of the variable (used for type inference)
     * @param <T>     the type of the current variable
     * @return the coerced value matching the type of {@code current}
     */
    @SuppressWarnings("unchecked")
    public static <T> T coerce(Object value, T current) {
        if (value == null || current == null)
            return (T) value;
        if (current.getClass().isInstance(value))
            return (T) value;

        if (current instanceof String)
            return (T) String.valueOf(value);

        if (current instanceof Number) {
            Number num;
            if (value instanceof Number n) {
                num = n;
            } else {
                try {
                    num = Double.parseDouble(String.valueOf(value));
                } catch (NumberFormatException e) {
                    return (T) value;
                }
            }
            if (current instanceof Integer)
                return (T) Integer.valueOf(num.intValue());
            if (current instanceof Long)
                return (T) Long.valueOf(num.longValue());
            if (current instanceof Double)
                return (T) Double.valueOf(num.doubleValue());
            if (current instanceof Float)
                return (T) Float.valueOf(num.floatValue());
            if (current instanceof Short)
                return (T) Short.valueOf(num.shortValue());
            if (current instanceof Byte)
                return (T) Byte.valueOf(num.byteValue());
        }

        return (T) value;
    }

    /**
     * Converts an arbitrary value to a double.
     *
     * <p>If the value is already a {@link Number}, its double value is returned.
     * Otherwise, the value is converted to a string and parsed. Returns {@code 0.0}
     * if parsing fails.
     *
     * @param value the value to convert
     * @return the double representation, or {@code 0.0} if not parseable
     */
    public static double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Converts an arbitrary value to an integer.
     *
     * <p>If the value is already a {@link Number}, its int value is returned.
     * Otherwise, the value is converted to a string and parsed. Returns {@code 0}
     * if parsing fails.
     *
     * @param value the value to convert
     * @return the int representation, or {@code 0} if not parseable
     */
    public static int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        try {
            return (int) Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
