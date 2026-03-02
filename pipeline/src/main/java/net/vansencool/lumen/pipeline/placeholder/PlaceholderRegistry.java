package net.vansencool.lumen.pipeline.placeholder;

import net.vansencool.lumen.api.placeholder.PlaceholderType;
import net.vansencool.lumen.pipeline.var.RefType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for compile-time placeholders that can be embedded in strings and expressions.
 *
 * <p>Placeholders use the syntax {@code {variable_property}} inside strings and var
 * expressions. When a placeholder is encountered during compilation, the registry resolves
 * it by:
 * <ol>
 *   <li>Splitting the placeholder into a variable name and a property name</li>
 *   <li>Looking up the variable in the current scope to find its {@link RefType}</li>
 *   <li>Looking up the property template for that RefType</li>
 *   <li>Expanding the template with the variable's Java name</li>
 * </ol>
 *
 * <p>Each property also carries a {@link PlaceholderType} so the compiler knows whether
 * it is safe to use in math expressions ({@link PlaceholderType#NUMBER}) or only in string
 * contexts ({@link PlaceholderType#STRING}).
 *
 * <h2>Template Format</h2>
 * <p>Templates use {@code $} as a stand-in for the Java variable name. For example, the
 * template {@code "$.getName()"} for a PLAYER variable named {@code player} expands to
 * {@code player.getName()}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * PlaceholderRegistry.registerProperty(RefType.PLAYER, "name", "$.getName()", PlaceholderType.STRING);
 * PlaceholderRegistry.registerProperty(RefType.PLAYER, "health", "$.getHealth()", PlaceholderType.NUMBER);
 * PlaceholderRegistry.registerDefault(RefType.PLAYER, "name");
 * }</pre>
 *
 * <p>In a script:
 * <pre>{@code
 * message player "Hello {player_name}, your health is {player_health}"
 * var hp = {player_health}
 * var yBelow = {player_y} - 1
 * }</pre>
 */
public final class PlaceholderRegistry {

    private static final Map<RefType, Map<String, PropertyEntry>> PROPERTIES = new ConcurrentHashMap<>();
    private static final Map<RefType, String> DEFAULTS = new ConcurrentHashMap<>();

    /**
     * Registers a property placeholder for a given ref type with an explicit return type.
     *
     * @param type       the ref type this property belongs to
     * @param property   the property name (e.g. "name", "health", "world")
     * @param template   the Java expression template where {@code $} is replaced by the variable name
     * @param returnType the return type of this property
     */
    public static void registerProperty(@NotNull RefType type, @NotNull String property,
                                        @NotNull String template, @NotNull PlaceholderType returnType) {
        PROPERTIES.computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                .put(property, new PropertyEntry(template, returnType));
    }

    /**
     * Sets the default property for a ref type, used when no property is specified
     * (e.g. {@code {player}} instead of {@code {player_name}}).
     *
     * @param type            the ref type
     * @param defaultProperty the property name to use as default
     */
    public static void registerDefault(@NotNull RefType type, @NotNull String defaultProperty) {
        DEFAULTS.put(type, defaultProperty);
    }

    /**
     * Looks up the template for a specific property on a ref type.
     *
     * @param type     the ref type
     * @param property the property name
     * @return the template string, or {@code null} if not registered
     */
    public static @Nullable String getProperty(@NotNull RefType type, @NotNull String property) {
        Map<String, PropertyEntry> props = PROPERTIES.get(type);
        if (props == null) return null;
        PropertyEntry entry = props.get(property);
        return entry != null ? entry.template : null;
    }

    /**
     * Looks up the return type for a specific property on a ref type.
     *
     * @param type     the ref type
     * @param property the property name
     * @return the return type, or {@code null} if the property is not registered
     */
    public static @Nullable PlaceholderType getPropertyType(@NotNull RefType type, @NotNull String property) {
        Map<String, PropertyEntry> props = PROPERTIES.get(type);
        if (props == null) return null;
        PropertyEntry entry = props.get(property);
        return entry != null ? entry.returnType : null;
    }

    /**
     * Returns the default property name for a ref type.
     *
     * @param type the ref type
     * @return the default property name, or {@code null} if none set
     */
    public static @Nullable String getDefault(@NotNull RefType type) {
        return DEFAULTS.get(type);
    }

    /**
     * Expands a template by replacing {@code $} with the actual Java variable name.
     *
     * @param template the template (e.g. {@code "$.getName()"})
     * @param javaVar  the Java variable name (e.g. {@code "player"})
     * @return the expanded expression (e.g. {@code "player.getName()"})
     */
    public static @NotNull String expand(@NotNull String template, @NotNull String javaVar) {
        return template.replace("$", javaVar);
    }

    private record PropertyEntry(@NotNull String template, @NotNull PlaceholderType returnType) {
    }
}
