package net.vansencool.lumen.api.type;

import net.vansencool.lumen.api.event.EventBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Constants for common Java types that can be used in event variable definitions.
 *
 * <p>Use these instead of raw type strings when registering event variables that do not
 * have a dedicated {@link RefTypeHandle}. This keeps event definitions clean and
 * avoids typos in fully-qualified class names or primitive type names.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * api.events().register(api.events().builder("entity_damage")
 *     .className("org.bukkit.event.entity.EntityDamageEvent")
 *     .addVar("entity", RefTypes.ENTITY, "event.getEntity()")
 *     .addVar("damage", JavaTypes.DOUBLE, "event.getDamage()")
 *     .build()
 * );
 * }</pre>
 *
 * @see RefTypes
 * @see EventBuilder
 */
@SuppressWarnings("unused")
public final class JavaTypes {

    public static final @NotNull String BOOLEAN = "boolean";
    public static final @NotNull String INT = "int";
    public static final @NotNull String LONG = "long";
    public static final @NotNull String DOUBLE = "double";
    public static final @NotNull String FLOAT = "float";
    public static final @NotNull String STRING = "String";

    private JavaTypes() {
    }
}
