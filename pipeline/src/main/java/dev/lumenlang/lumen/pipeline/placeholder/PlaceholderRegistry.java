package dev.lumenlang.lumen.pipeline.placeholder;

import dev.lumenlang.lumen.api.placeholder.PlaceholderType;
import dev.lumenlang.lumen.api.type.ObjectType;
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
 *   <li>Looking up the variable in the current scope to find its {@link ObjectType}</li>
 *   <li>Looking up the property template for that type</li>
 *   <li>Expanding the template with the variable's Java name</li>
 * </ol>
 *
 * <p>Each property also carries a {@link PlaceholderType} so the compiler knows whether
 * it is safe to use in math expressions ({@link PlaceholderType#NUMBER}) or only in string
 * contexts ({@link PlaceholderType#STRING}).
 *
 * <p>Templates use {@code $} as a stand-in for the Java variable name.
 */
public final class PlaceholderRegistry {

    private static final Map<String, Map<String, PropertyEntry>> PROPERTIES = new ConcurrentHashMap<>();
    private static final Map<String, String> DEFAULTS = new ConcurrentHashMap<>();

    public static void registerProperty(@NotNull ObjectType type, @NotNull String property, @NotNull String template, @NotNull PlaceholderType returnType) {
        PROPERTIES.computeIfAbsent(type.id(), k -> new ConcurrentHashMap<>())
                .put(property, new PropertyEntry(template, returnType));
    }

    public static void registerDefault(@NotNull ObjectType type, @NotNull String defaultProperty) {
        DEFAULTS.put(type.id(), defaultProperty);
    }

    public static @Nullable String getProperty(@NotNull ObjectType type, @NotNull String property) {
        Map<String, PropertyEntry> props = PROPERTIES.get(type.id());
        if (props == null) return null;
        PropertyEntry entry = props.get(property);
        return entry != null ? entry.template : null;
    }

    public static @Nullable PlaceholderType getPropertyType(@NotNull ObjectType type, @NotNull String property) {
        Map<String, PropertyEntry> props = PROPERTIES.get(type.id());
        if (props == null) return null;
        PropertyEntry entry = props.get(property);
        return entry != null ? entry.returnType : null;
    }

    public static @Nullable String getDefault(@NotNull ObjectType type) {
        return DEFAULTS.get(type.id());
    }

    public static @NotNull String expand(@NotNull String template, @NotNull String javaVar) {
        return template.replace("$", javaVar);
    }

    private record PropertyEntry(@NotNull String template, @NotNull PlaceholderType returnType) {
    }
}
