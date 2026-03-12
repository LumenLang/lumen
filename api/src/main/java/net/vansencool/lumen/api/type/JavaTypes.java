package net.vansencool.lumen.api.type;

import net.vansencool.lumen.api.event.EventBuilder;
import net.vansencool.lumen.api.handler.ExpressionHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Constants for common Java types used in event variable definitions and expression results.
 *
 * <p>Use these instead of raw type strings when registering event variables or returning
 * expression results. This avoids typos and keeps definitions clean.
 *
 * <p>For object/reference types, use {@link RefTypes} or register custom types via
 * {@link RefTypeRegistrar}. The constants here cover only primitives and {@code String}.
 *
 * <h2>Event Variable Usage</h2>
 * <pre>{@code
 * api.events().register(api.events().builder("entity_damage")
 *     .className("org.bukkit.event.entity.EntityDamageEvent")
 *     .addVar("entity", RefTypes.ENTITY, "event.getEntity()")
 *     .addVar("damage", JavaTypes.DOUBLE, "event.getDamage()")
 *     .build()
 * );
 * }</pre>
 *
 * <h2>Expression Result Usage</h2>
 * <pre>{@code
 * new ExpressionResult(ctx.java("loc") + ".getX()", null, JavaTypes.DOUBLE)
 * }</pre>
 *
 * @see RefTypes
 * @see RefTypeRegistrar
 * @see EventBuilder
 * @see ExpressionHandler.ExpressionResult
 * @deprecated Use {@link Types} instead. This class will be removed in a future release.
 */
@Deprecated
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
