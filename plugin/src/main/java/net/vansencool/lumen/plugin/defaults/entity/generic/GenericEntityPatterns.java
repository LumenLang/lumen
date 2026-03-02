package net.vansencool.lumen.plugin.defaults.entity.generic;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.RefTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Registers statements, conditions, and expressions that work on any {@code Entity}.
 *
 * <p>These patterns do not require an {@code instanceof} guard because they operate
 * solely on methods available on the base {@code org.bukkit.entity.Entity} interface.
 */
@Registration
@Description("Registers generic entity patterns that work on any Entity.")
@SuppressWarnings("unused")
public final class GenericEntityPatterns {

    @Call
    public void register(@NotNull LumenAPI api) {
        registerStatements(api);
        registerConditions(api);
        registerExpressions(api);
    }

    private void registerStatements(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen").pattern("remove %e:ENTITY%")
                .description("Removes an entity from the world.")
                .example("remove mob")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(ctx.java("e") + ".remove();")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("spawn %type:ENTITY_TYPE% at %who:PLAYER%")
                .description("Spawns an entity at a player's location.")
                .example("spawn zombie at player")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(
                        ctx.java("who") + ".getLocation().getWorld().spawnEntity("
                                + ctx.java("who") + ".getLocation(), " + ctx.java("type") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("spawn %type:ENTITY_TYPE% [at] %loc:LOCATION%")
                .description("Spawns an entity at a location.")
                .example("spawn zombie at loc")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(
                        ctx.java("loc") + ".getWorld().spawnEntity("
                                + ctx.java("loc") + ", " + ctx.java("type") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("(teleport|tp) %e:ENTITY% [to] %loc:LOCATION%")
                .description("Teleports an entity to a location.")
                .example("teleport mob to loc")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(ctx.java("e") + ".teleport(" + ctx.java("loc") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("(teleport|tp) %e:ENTITY% [to] %target:ENTITY%")
                .description("Teleports an entity to another entity.")
                .example("teleport mob to target")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(ctx.java("e") + ".teleport(" + ctx.java("target") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("(teleport|tp) %e:ENTITY% [to] %target:PLAYER%")
                .description("Teleports an entity to a player.")
                .example("teleport mob to player")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(ctx.java("e") + ".teleport(" + ctx.java("target") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY% on fire [for] %ticks:INT% [ticks]")
                .description("Sets an entity on fire for the specified number of ticks.")
                .example("set mob on fire for 100 ticks")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(ctx.java("e") + ".setFireTicks(" + ctx.java("ticks") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("(extinguish|put out) %e:ENTITY%")
                .description("Extinguishes a burning entity.")
                .example("extinguish mob").since("1.0.0").category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(ctx.java("e") + ".setFireTicks(0);")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY_POSSESSIVE% custom name [to] %name:STRING%")
                .description("Sets an entity's custom name and makes it visible.")
                .example("set mob's custom name to \"Boss\"")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> {
                    out.line(ctx.java("e") + ".setCustomName(" + ctx.java("name") + ");");
                    out.line(ctx.java("e") + ".setCustomNameVisible(true);");
                }));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("eject %e:ENTITY%")
                .description("Ejects any passengers from an entity.")
                .example("eject mob")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(ctx.java("e") + ".eject();")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY_POSSESSIVE% gravity [to] %val:BOOLEAN%")
                .description("Enables or disables gravity for an entity.")
                .example("set mob's gravity to true")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(
                        ctx.java("e") + ".setGravity(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY_POSSESSIVE% invulnerable [to] %val:BOOLEAN%")
                .description("Sets whether an entity is invulnerable.")
                .example("set mob's invulnerable to true")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(
                        ctx.java("e") + ".setInvulnerable(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY_POSSESSIVE% silent [to] %val:BOOLEAN%")
                .description("Sets whether an entity is silent (produces no sounds).")
                .example("set mob's silent to true")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(
                        ctx.java("e") + ".setSilent(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY_POSSESSIVE% glowing [to] %val:BOOLEAN%")
                .description("Sets whether an entity has the glowing outline effect.")
                .example("set mob's glowing to true")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(
                        ctx.java("e") + ".setGlowing(" + ctx.java("val") + ");")));
    }

    private void registerConditions(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is on fire")
                .description("Checks if an entity is currently on fire.")
                .example("if mob is on fire:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getFireTicks() > 0"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is not on fire")
                .description("Checks if an entity is not on fire.")
                .example("if mob is not on fire:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getFireTicks() <= 0"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is on ground")
                .description("Checks if an entity is on the ground.")
                .example("if mob is on ground:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".isOnGround()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is not on ground")
                .description("Checks if an entity is not on the ground.")
                .example("if mob is not on ground:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> "!" + match.ref("e").java() + ".isOnGround()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is in water")
                .description("Checks if an entity is in water.")
                .example("if mob is in water:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".isInWater()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is not in water")
                .description("Checks if an entity is not in water.")
                .example("if mob is not in water:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> "!" + match.ref("e").java() + ".isInWater()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY_POSSESSIVE% type is %type:ENTITY_TYPE%")
                .description("Checks if an entity's type matches (prefix syntax).")
                .example("if mob's type is zombie:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getType() == " + match.java("type", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY_POSSESSIVE% type is not %type:ENTITY_TYPE%")
                .description("Checks if an entity's type does not match (prefix syntax).")
                .example("if mob's type is not zombie:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getType() != " + match.java("type", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is [a] %type:ENTITY_TYPE%")
                .description("Checks if an entity's type matches.")
                .example("if mob is a zombie:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getType() == " + match.java("type", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is not [a] %type:ENTITY_TYPE%")
                .description("Checks if an entity's type does not match.")
                .example("if mob is not a zombie:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getType() != " + match.java("type", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% has custom name")
                .description("Checks if an entity has a custom name set.")
                .example("if mob has custom name:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getCustomName() != null"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is valid")
                .description("Checks if an entity reference is still valid (not removed).")
                .example("if mob is valid:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".isValid()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is not valid")
                .description("Checks if an entity reference is no longer valid.")
                .example("if mob is not valid:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> "!" + match.ref("e").java() + ".isValid()"));
    }

    private void registerExpressions(@NotNull LumenAPI api) {
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
