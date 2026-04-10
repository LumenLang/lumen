package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
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
                .example("set loc to get mob's location")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getLocation()", MinecraftTypes.LOCATION)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% world")
                .description("Returns the world the entity is in.")
                .example("set w to get mob's world")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getWorld()", MinecraftTypes.WORLD)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% type")
                .description("Returns the entity's EntityType.")
                .example("set t to get mob's type")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getType()", MinecraftTypes.ENTITY_TYPE)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% name")
                .description("Returns the entity's name.")
                .example("set n to get mob's name")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getName()", PrimitiveType.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% custom name")
                .description("Returns the entity's custom name, or null if not set.")
                .example("set cn to get mob's custom name")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getCustomName()", PrimitiveType.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% uuid")
                .description("Returns the entity's UUID as a string.")
                .example("set id to get mob's uuid")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getUniqueId().toString()", PrimitiveType.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% velocity")
                .description("Returns the entity's velocity vector.")
                .example("set vel to get mob's velocity")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getVelocity()", MinecraftTypes.VECTOR)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% fire ticks")
                .description("Returns the number of ticks the entity will remain on fire.")
                .example("set ft to get mob's fire ticks")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getFireTicks()", PrimitiveType.INT)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% passenger count")
                .description("Returns how many passengers are riding the entity.")
                .example("set count to get mob's passenger count")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> new ExpressionResult(ctx.java("e") + ".getPassengers().size()", PrimitiveType.INT)));
    }
}
