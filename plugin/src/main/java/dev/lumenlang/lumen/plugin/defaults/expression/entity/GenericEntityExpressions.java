package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.Types;
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
                .example("var loc = get mob's location")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnRefTypeId(Types.LOCATION.id())
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getLocation()", Types.LOCATION.id())));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% world")
                .description("Returns the world the entity is in.")
                .example("var w = get mob's world")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnRefTypeId(Types.WORLD.id())
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getWorld()", Types.WORLD.id())));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% type")
                .description("Returns the entity's EntityType.")
                .example("var t = get mob's type")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getType()", null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% name")
                .description("Returns the entity's name.")
                .example("var n = get mob's name")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnJavaType(Types.STRING)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getName()", null, Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% custom name")
                .description("Returns the entity's custom name, or null if not set.")
                .example("var cn = get mob's custom name")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnJavaType(Types.STRING)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getCustomName()", null, Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% uuid")
                .description("Returns the entity's UUID as a string.")
                .example("var id = get mob's uuid")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnJavaType(Types.STRING)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getUniqueId().toString()", null, Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% velocity")
                .description("Returns the entity's velocity vector.")
                .example("var vel = get mob's velocity")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getVelocity()", null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% fire ticks")
                .description("Returns the number of ticks the entity will remain on fire.")
                .example("var ft = get mob's fire ticks")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnJavaType(Types.INT)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getFireTicks()", null, Types.INT)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% passenger count")
                .description("Returns how many passengers are riding the entity.")
                .example("var count = get mob's passenger count")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnJavaType(Types.INT)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getPassengers().size()", null, Types.INT)));
    }
}
