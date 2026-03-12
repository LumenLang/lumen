package net.vansencool.lumen.api.placeholder;

import net.vansencool.lumen.api.type.RefTypeHandle;
import org.jetbrains.annotations.NotNull;

/**
 * API registrar for compile-time placeholder properties.
 *
 * <p>Placeholders use the syntax {@code {variable_property}} inside strings. When a
 * placeholder is encountered during compilation, the registry resolves it by looking up
 * the variable's {@link RefTypeHandle} and finding the matching property template.
 *
 * <h2>Template Format</h2>
 * <p>Templates use {@code $} as a stand-in for the Java variable name. For example, the
 * template {@code "$.getName()"} for a PLAYER variable named {@code player} expands to
 * {@code player.getName()}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * PlaceholderRegistrar ph = api.placeholders();
 * ph.property(Types.PLAYER, "name", "$.getName()");
 * ph.property(Types.PLAYER, "health", "$.getHealth()", PlaceholderType.NUMBER);
 * ph.defaultProperty(Types.PLAYER, "name");
 * }</pre>
 *
 * <p>In a script:
 * <pre>{@code
 * message player "Hello {player_name}, your health is {player_health}"
 * var hp = {player_health}
 * var yBelow = {player_y} - 1
 * }</pre>
 *
 * @see RefTypeHandle
 * @see PlaceholderType
 */
public interface PlaceholderRegistrar {

    /**
     * Registers a property placeholder for a given ref type with {@link PlaceholderType#STRING}
     * return type.
     *
     * @param type     the ref type this property belongs to
     * @param property the property name (e.g. "name", "world")
     * @param template the Java expression template where {@code $} is replaced by the variable name
     */
    void property(@NotNull RefTypeHandle type, @NotNull String property, @NotNull String template);

    /**
     * Registers a property placeholder for a given ref type with an explicit return type.
     *
     * <p>Properties with {@link PlaceholderType#NUMBER} can be used in math expressions
     * (e.g. {@code var yBelow = {player_y} - 1}). Properties with {@link PlaceholderType#STRING}
     * can only be used in string contexts.
     *
     * @param type       the ref type this property belongs to
     * @param property   the property name (e.g. "health", "x", "y", "z")
     * @param template   the Java expression template where {@code $} is replaced by the variable name
     * @param returnType the return type of the property
     */
    void property(@NotNull RefTypeHandle type, @NotNull String property, @NotNull String template, @NotNull PlaceholderType returnType);

    /**
     * Sets the default property for a ref type, used when no property is specified
     * (e.g. {@code {player}} instead of {@code {player_name}}).
     *
     * @param type            the ref type
     * @param defaultProperty the property name to use as default
     */
    void defaultProperty(@NotNull RefTypeHandle type, @NotNull String defaultProperty);
}
