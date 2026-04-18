package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Registers entity spawning and projectile launching expression patterns.
 */
@Registration
@SuppressWarnings("unused")
public final class EntityExpressions {

    /**
     * Maps an {@code EntityType} constant to its corresponding Bukkit entity
     * class name and returns it as a metadata map with a {@code "javaClass"}
     * key. Falls back to {@code LivingEntity} when the concrete class cannot
     * be resolved.
     *
     * @param typeEnum the Java expression for the EntityType constant (e.g. {@code "org.bukkit.entity.EntityType.ZOMBIE"})
     * @return metadata map containing the javaClass, or empty if unresolvable
     */
    private static @NotNull Map<String, Object> resolveEntityMeta(@NotNull String typeEnum) {
        String name = typeEnum.replace("EntityType.", "");
        try {
            EntityType et = EntityType.valueOf(name);
            Class<?> cls = et.getEntityClass();
            if (cls != null) {
                return Map.of("javaClass", cls.getName());
            }
        } catch (IllegalArgumentException ignored) {
        }
        return Map.of();
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("spawn %type:ENTITY_TYPE% at %who:PLAYER%")
                .description("Spawns an entity at a player's location and returns it.")
                .example("set mob to spawn zombie at player")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    ctx.codegen().addImport(Entity.class.getName());
                    String typeEnum = ctx.java("type");
                    String playerJava = ctx.java("who");
                    Map<String, Object> meta = resolveEntityMeta(typeEnum);
                    return new ExpressionResult(
                            playerJava + ".getLocation().getWorld().spawnEntity("
                                    + playerJava + ".getLocation(), " + typeEnum
                                    + ")",
                            MinecraftTypes.ENTITY,
                            meta);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("spawn %type:ENTITY_TYPE% [at] %loc:LOCATION%")
                .description("Spawns an entity at a location and returns it.")
                .example("set mob to spawn zombie at myLoc")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    ctx.codegen().addImport(Entity.class.getName());
                    String typeEnum = ctx.java("type");
                    Map<String, Object> meta = resolveEntityMeta(typeEnum);
                    return new ExpressionResult(
                            ctx.java("loc") + ".getWorld().spawnEntity(" + ctx.java("loc")
                                    + ", " + typeEnum + ")",
                            MinecraftTypes.ENTITY,
                            meta);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("launch %type:ENTITY_TYPE% from %who:PLAYER%")
                .description("Launches a projectile from a player in the direction they are looking and returns it.")
                .example("set proj to launch snowball from player")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    ctx.codegen().addImport(Entity.class.getName());
                    ctx.codegen().addImport(Projectile.class.getName());
                    String type = ctx.java("type");
                    String player = ctx.java("who");
                    return new ExpressionResult(
                            "(Entity) " + player + ".launchProjectile((Class) " + type + ".getEntityClass())",
                            MinecraftTypes.ENTITY);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("launch %type:ENTITY_TYPE% from %loc:LOCATION%")
                .description("Spawns a projectile at a location and returns it. The projectile spawns with no initial velocity.")
                .example("set proj to launch snowball from myLoc")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    ctx.codegen().addImport(Entity.class.getName());
                    String type = ctx.java("type");
                    String loc = ctx.java("loc");
                    return new ExpressionResult(
                            loc + ".getWorld().spawnEntity(" + loc + ", " + type + ")",
                            MinecraftTypes.ENTITY);
                }));
    }
}