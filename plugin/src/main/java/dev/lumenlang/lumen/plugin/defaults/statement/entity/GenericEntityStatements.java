package dev.lumenlang.lumen.plugin.defaults.statement.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.util.LaunchHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Registers statement patterns that work on any {@code Entity}.
 */
@Registration
@SuppressWarnings("unused")
public final class GenericEntityStatements {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen").pattern("remove %e:ENTITY%")
                .description("Removes an entity from the world.")
                .example("remove mob")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(ctx.java("e") + ".remove();")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("spawn %type:ENTITY_TYPE% at %who:PLAYER%")
                .description("Spawns an entity at a player's location.")
                .example("spawn zombie at player")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(
                        ctx.java("who") + ".getLocation().getWorld().spawnEntity("
                                + ctx.java("who") + ".getLocation(), " + ctx.java("type") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("spawn %type:ENTITY_TYPE% [at] %loc:LOCATION%")
                .description("Spawns an entity at a location.")
                .example("spawn zombie at loc")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(
                        ctx.java("loc") + ".getWorld().spawnEntity("
                                + ctx.java("loc") + ", " + ctx.java("type") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("(teleport|tp) %e:ENTITY% [to] %loc:LOCATION%")
                .description("Teleports an entity to a location.")
                .example("teleport mob to loc")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(ctx.java("e") + ".teleport(" + ctx.java("loc") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("(teleport|tp) %e:ENTITY% [to] %target:ENTITY%")
                .description("Teleports an entity to another entity.")
                .example("teleport mob to target")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(ctx.java("e") + ".teleport(" + ctx.java("target") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("(teleport|tp) %e:ENTITY% [to] %target:PLAYER%")
                .description("Teleports an entity to a player.")
                .example("teleport mob to player")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(ctx.java("e") + ".teleport(" + ctx.java("target") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY% on fire [for] %ticks:INT% [ticks]")
                .description("Sets an entity on fire for the specified number of ticks.")
                .example("set mob on fire for 100 ticks")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(ctx.java("e") + ".setFireTicks(" + ctx.java("ticks") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("(extinguish|put out) %e:ENTITY%")
                .description("Extinguishes a burning entity.")
                .example("extinguish mob").since("1.0.0").category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(ctx.java("e") + ".setFireTicks(0);")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY_POSSESSIVE% custom name [to] %name:STRING%")
                .description("Sets an entity's custom name and makes it visible.")
                .example("set mob's custom name to \"Boss\"")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    ctx.out().line(ctx.java("e") + ".setCustomName(" + ctx.java("name") + ");");
                    ctx.out().line(ctx.java("e") + ".setCustomNameVisible(true);");
                }));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("eject %e:ENTITY%")
                .description("Ejects any passengers from an entity.")
                .example("eject mob")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(ctx.java("e") + ".eject();")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY_POSSESSIVE% gravity [to] %val:BOOLEAN%")
                .description("Enables or disables gravity for an entity.")
                .example("set mob's gravity to true")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(
                        ctx.java("e") + ".setGravity(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY_POSSESSIVE% invulnerable [to] %val:BOOLEAN%")
                .description("Sets whether an entity is invulnerable.")
                .example("set mob's invulnerable to true")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(
                        ctx.java("e") + ".setInvulnerable(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY_POSSESSIVE% silent [to] %val:BOOLEAN%")
                .description("Sets whether an entity is silent (produces no sounds).")
                .example("set mob's silent to true")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(
                        ctx.java("e") + ".setSilent(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY_POSSESSIVE% glowing [to] %val:BOOLEAN%")
                .description("Sets whether an entity has the glowing outline effect.")
                .example("set mob's glowing to true")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> ctx.out().line(
                        ctx.java("e") + ".setGlowing(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("launch %e:ENTITY% toward %loc:LOCATION%")
                .description("Launches an entity toward a location with the speed of 1.")
                .example("launch player toward hook_loc")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    ctx.codegen().addImport(LaunchHelper.class.getName());
                    ctx.out().line("LaunchHelper.launch(" + ctx.java("e") + ", " + ctx.java("loc") + ", 1);");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("launch %e:ENTITY% toward %loc:LOCATION% with speed %spd:EXPR%")
                .description("Launches an entity toward a location with the given speed.")
                .example("launch player toward hook_loc with speed 2")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    ctx.codegen().addImport(LaunchHelper.class.getName());
                    ctx.out().line("LaunchHelper.launch(" + ctx.java("e") + ", "
                            + ctx.java("loc") + ", ((double) " + ctx.java("spd") + "));");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %e:ENTITY_POSSESSIVE% velocity [to] %x:EXPR% %y:EXPR% %z:EXPR%")
                .description("Sets an entity's velocity to the given x, y, z components.")
                .example("set mob's velocity to 0 1 0")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    ctx.codegen().addImport("org.bukkit.util.Vector");
                    ctx.out().line(ctx.java("e") + ".setVelocity(new Vector(((double) "
                            + ctx.java("x") + "), ((double) " + ctx.java("y")
                            + "), ((double) " + ctx.java("z") + ")));");
                }));
    }
}
