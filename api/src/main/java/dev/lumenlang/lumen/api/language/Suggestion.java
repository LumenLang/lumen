package dev.lumenlang.lumen.api.language;

import dev.lumenlang.lumen.api.codegen.TypeEnv;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One completion entry a {@code TypeBinding} or {@code BlockFormHandler} can
 * contribute.
 *
 * <p>Bindings build suggestions through the per-flavour static factories.
 * Editors read fields back through the public accessors. Internal shape is
 * opaque.
 *
 * <h2>Picking a factory</h2>
 *
 * <ul>
 *   <li>{@link #literal} for a closed-set value the user types verbatim,
 *       such as a material name or the string {@code "true"}.</li>
 *   <li>{@link #variable} for an in-scope variable. The type display is read
 *       from the handle.</li>
 *   <li>{@link #event} for an event the user can subscribe to.</li>
 *   <li>{@link #statement}, {@link #block}, {@link #expression},
 *       {@link #condition} for a registered pattern. Pass the raw pattern
 *       text.</li>
 * </ul>
 */
public final class Suggestion {

    private final @NotNull String insertText;
    private final @NotNull String detail;
    private final @NotNull SemanticKind kind;

    private Suggestion(@NotNull String insertText, @NotNull String detail, @NotNull SemanticKind kind) {
        this.insertText = insertText;
        this.detail = detail;
        this.kind = kind;
    }

    /**
     * Literal value the user types as-is, such as a material name or the
     * string {@code "true"}.
     *
     * @param text     the text to insert and display
     * @param category one-word hint shown beside the entry, e.g.
     *                 {@code "material"}
     * @param kind     editor category for icon selection
     * @return the suggestion
     */
    public static @NotNull Suggestion literal(@NotNull String text, @NotNull String category, @NotNull SemanticKind kind) {
        return new Suggestion(text, category, kind);
    }

    /**
     * In-scope variable. The detail column shows the variable's declared
     * type display name.
     *
     * @param handle the variable handle pulled from the live environment
     * @return the suggestion
     */
    public static @NotNull Suggestion variable(@NotNull TypeEnv.VarHandle handle) {
        return new Suggestion(handle.name(), handle.type().displayName(), SemanticKind.VARIABLE);
    }

    /**
     * Registered event the user can subscribe to via {@code on <name>:}.
     *
     * @param name the event name as written in script syntax
     * @return the suggestion
     */
    public static @NotNull Suggestion event(@NotNull String name) {
        return new Suggestion(name, "event", SemanticKind.EVENT);
    }

    /**
     * Registered top-level statement.
     *
     * @param raw the statement's raw pattern form
     * @return the suggestion
     */
    public static @NotNull Suggestion statement(@NotNull String raw) {
        return new Suggestion(raw, "statement", SemanticKind.KEYWORD);
    }

    /**
     * Registered block header.
     *
     * @param raw the block's raw pattern form
     * @return the suggestion
     */
    public static @NotNull Suggestion block(@NotNull String raw) {
        return new Suggestion(raw, "block", SemanticKind.KEYWORD);
    }

    /**
     * Registered expression.
     *
     * @param raw the expression's raw pattern form
     * @return the suggestion
     */
    public static @NotNull Suggestion expression(@NotNull String raw) {
        return new Suggestion(raw, "expression", SemanticKind.PASSTHROUGH);
    }

    /**
     * Registered condition.
     *
     * @param raw the condition's raw pattern form
     * @return the suggestion
     */
    public static @NotNull Suggestion condition(@NotNull String raw) {
        return new Suggestion(raw, "condition", SemanticKind.KEYWORD);
    }

    /**
     * The text to insert when the user accepts this suggestion. Also used as
     * the popup label.
     */
    public @NotNull String insertText() {
        return insertText;
    }

    /**
     * Short type or category hint shown alongside the entry.
     */
    public @NotNull String detail() {
        return detail;
    }

    /**
     * Editor category, used for icon selection.
     */
    public @NotNull SemanticKind kind() {
        return kind;
    }

    /**
     * Returns whether two suggestions would render identically.
     */
    @SuppressWarnings("unused")
    public boolean sameAs(@Nullable Suggestion other) {
        if (other == null) return false;
        return insertText.equals(other.insertText) && detail.equals(other.detail) && kind == other.kind;
    }
}
