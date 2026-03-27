package dev.lumenlang.lumen.api.inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides fake variable accessors for use inside injectable body lambdas.
 *
 * <p>These methods exist only so the IDE can resolve types and provide code completion.
 * They are never executed at runtime. During bytecode extraction, calls to these
 * methods are detected and replaced with the real binding variables.
 *
 * <p>The parameter passed to each fake method is the binding name from the pattern,
 * for example {@code "who"} for a pattern binding {@code %who:PLAYER%}.
 *
 * <p>Example:
 * <pre>{@code
 * Player player = Fakes.fake("who");
 * player.sendMessage("Hello!");
 * }</pre>
 */
public final class Fakes {

    private Fakes() {
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    public static <T> @NotNull T fake(@NotNull String bindingName) {
        return (T) (Object) null;
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable T fakeNullable(@NotNull String bindingName) {
        return (T) (Object) null;
    }

    public static @NotNull String fakeString(@NotNull String bindingName) {
        return "";
    }

    public static int fakeInt(@NotNull String bindingName) {
        return 0;
    }

    public static long fakeLong(@NotNull String bindingName) {
        return 0L;
    }

    public static double fakeDouble(@NotNull String bindingName) {
        return 0.0;
    }

    public static float fakeFloat(@NotNull String bindingName) {
        return 0.0f;
    }

    public static boolean fakeBoolean(@NotNull String bindingName) {
        return false;
    }
}
