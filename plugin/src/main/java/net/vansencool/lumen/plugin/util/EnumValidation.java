package net.vansencool.lumen.plugin.util;

import org.bukkit.attribute.Attribute;
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
     * Checks if the given string is a valid {@link Attribute} name.
     *
     * @param name the attribute name to check
     * @return {@code true} if the name corresponds to a valid attribute
     */
    public static boolean isValidAttribute(@NotNull String name) {
        try {
            Attribute.valueOf(name.toUpperCase().replace(' ', '_').replace('-', '_'));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
