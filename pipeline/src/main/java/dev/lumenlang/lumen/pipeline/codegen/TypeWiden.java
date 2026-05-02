package dev.lumenlang.lumen.pipeline.codegen;

import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Centralized numeric widening for handler-emitted Java expressions.
 */
public final class TypeWiden {

    private TypeWiden() {}

    /**
     * Returns {@code raw} cast to {@code target} when a primitive widening is required, otherwise unchanged.
     */
    public static @NotNull String widen(@NotNull String raw, @Nullable LumenType actual, @NotNull LumenType target) {
        if (actual == null || actual == target) return raw;
        if (target == PrimitiveType.LONG && actual == PrimitiveType.INT) {
            if (isIntLiteral(raw)) return raw + "L";
            return "(long) (" + raw + ")";
        }
        if (target == PrimitiveType.DOUBLE && (actual == PrimitiveType.INT || actual == PrimitiveType.LONG || actual == PrimitiveType.FLOAT)) {
            if (isIntLiteral(raw)) return raw + ".0";
            return "(double) (" + raw + ")";
        }
        if (target == PrimitiveType.FLOAT && (actual == PrimitiveType.INT || actual == PrimitiveType.LONG)) {
            if (isIntLiteral(raw)) return raw + ".0f";
            return "(float) (" + raw + ")";
        }
        return raw;
    }

    private static boolean isIntLiteral(@NotNull String s) {
        if (s.isEmpty()) return false;
        int i = s.charAt(0) == '-' ? 1 : 0;
        if (i >= s.length()) return false;
        for (; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }
}
