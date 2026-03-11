package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.RefTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Registers expression patterns that work on any {@code Entity}.
 */
@Registration
@SuppressWarnings("unused")
public final class GenericEntityExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% location")
                .description("Returns the entity's current location.")
                .example("var loc = mob's location")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getLocation()", RefTypes.LOCATION.id())));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% world")
                .description("Returns the world the entity is in.")
                .example("var w = mob's world")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getWorld()", RefTypes.WORLD.id())));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% type")
                .description("Returns the entity's EntityType.")
                .example("var t = mob's type")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getType()", null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% name")
                .description("Returns the entity's name.")
                .example("var n = mob's name")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getName()", null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% custom name")
                .description("Returns the entity's custom name, or null if not set.")
                .example("var cn = mob's custom name")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getCustomName()", null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% uuid")
                .description("Returns the entity's UUID as a string.")
                .example("var id = mob's uuid")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getUniqueId().toString()", null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% velocity")
                .description("Returns the entity's velocity vector.")
                .example("var vel = mob's velocity")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getVelocity()", null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% fire ticks")
                .description("Returns the number of ticks the entity will remain on fire.")
                .example("var ft = mob's fire ticks")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getFireTicks()", null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% passenger count")
                .description("Returns how many passengers are riding the entity.")
                .example("var count = mob's passenger count")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getPassengers().size()", null)));
    }
}
