package net.vansencool.lumen.plugin.util;

import net.vansencool.lumen.plugin.defaults.util.AttributeNames;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * Runtime validation helpers for Bukkit enum types, used by generated script code to
 * check whether a string is a valid enum name before attempting conversion.
 */
@SuppressWarnings("unused")
public final class EnumValidation {

    private EnumValidation() {
    }

    /**
     * Checks if the given string is a valid {@link EntityType} name.
     *
     * @param name the entity type name to check
     * @return {@code true} if the name corresponds to a valid entity type
     */
    public static boolean isValidEntityType(@NotNull String name) {
        try {
            EntityType.valueOf(name.toUpperCase().replace(' ', '_').replace('-', '_'));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks if the given string is a valid attribute name, using {@link AttributeNames} for
     * version-aware resolution that handles both the pre-1.21.2 and post-1.21.2 enum names.
     *
     * @param name the attribute name to check
     * @return {@code true} if the name corresponds to a valid attribute on the current server version
     */
    public static boolean isValidAttribute(@NotNull String name) {
        return AttributeNames.resolve(name) != null;
    }
}
