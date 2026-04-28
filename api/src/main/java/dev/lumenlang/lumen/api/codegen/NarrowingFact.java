package dev.lumenlang.lumen.api.codegen;

import org.jetbrains.annotations.NotNull;

/**
 * A compile-time assertion that a condition proves about a variable at its point
 * of evaluation. Conditions push facts via {@link EnvironmentAccess#pushNarrowing}
 * and block handlers consume them to narrow variable state in the enclosed body.
 */
public record NarrowingFact(@NotNull Kind kind, @NotNull String javaName) {

    public enum Kind {
        /**
         * The variable is proven non-null in the branch where the condition holds.
         */
        NON_NULL,
        /**
         * The variable is treated as nullable in the branch where the condition holds.
         * Uses of the variable in this branch receive the same warnings as any other
         * nullable access.
         */
        NULLABLE
    }

    public static @NotNull NarrowingFact nonNull(@NotNull String javaName) {
        return new NarrowingFact(Kind.NON_NULL, javaName);
    }

    public static @NotNull NarrowingFact nullable(@NotNull String javaName) {
        return new NarrowingFact(Kind.NULLABLE, javaName);
    }

    /**
     * Returns a fact that describes the complementary branch.
     *
     * @return the inverted fact
     */
    public @NotNull NarrowingFact inverted() {
        return switch (kind) {
            case NON_NULL -> nullable(javaName);
            case NULLABLE -> nonNull(javaName);
        };
    }
}
