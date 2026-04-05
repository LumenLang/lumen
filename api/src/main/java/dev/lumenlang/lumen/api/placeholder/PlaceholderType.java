package dev.lumenlang.lumen.api.placeholder;

/**
 * Describes the return type of a placeholder property, used by the compiler
 * to decide how a placeholder value is used in expressions and string contexts.
 *
 * <p>For example, {@code {player_y}} returns a {@link #NUMBER} and can be used
 * in {@code set yBelow to {player_y} - 1}, while {@code {player_name}} returns
 * a {@link #STRING} and cannot.
 */
public enum PlaceholderType {

    /**
     * The property evaluates to a string value (e.g. a player name, world name, UUID).
     */
    STRING,

    /**
     * The property evaluates to a numeric value (e.g. coordinates, health, food level).
     * Numeric placeholders can participate in math expressions.
     */
    NUMBER,

    /**
     * The property evaluates to a boolean value (e.g. whether a player is flying, sneaking).
     * Boolean placeholders display as {@code "true"} or {@code "false"} in string contexts.
     */
    BOOLEAN
}
