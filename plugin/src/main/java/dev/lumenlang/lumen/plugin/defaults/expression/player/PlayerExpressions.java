package dev.lumenlang.lumen.plugin.defaults.expression.player;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Registers player-related expression patterns.
 */
@Registration
@SuppressWarnings("unused")
public final class PlayerExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get player (by|from) (name|username) %name:STRING%")
                .description("Looks up an online player by name. Returns null if the player is not online.")
                .example("set p to get player by name \"Notch\"")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(
                        "Bukkit.getPlayer(" + ctx.java("name") + ")",
                        MinecraftTypes.PLAYER.wrapAsNullable())));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get player (by|from) (uuid|unique id) %uuid:STRING%")
                .description("Looks up an online player by UUID string. Returns null if the player is not online.")
                .example("set p to get player by uuid \"069a79f4-44e9-4726-a5be-fca90e38aaf5\"")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    ctx.codegen().addImport(UUID.class.getName());
                    return new ExpressionResult(
                            "Bukkit.getPlayer(UUID.fromString(" + ctx.java("uuid") + "))",
                            MinecraftTypes.PLAYER.wrapAsNullable());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% name")
                .description("Returns an online player's display name.")
                .example("set name to get player's name")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getName()", PrimitiveType.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% location")
                .description("Returns the current location of a player.")
                .example("set loc to get player's location")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLocation()",
                        MinecraftTypes.LOCATION)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% world")
                .description("Returns the world a player is currently in.")
                .example("set w to get player's world")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getWorld()",
                        MinecraftTypes.WORLD)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% x")
                .description("Returns the player's current X coordinate as a double.")
                .example("set px to get player's x")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLocation().getX()", PrimitiveType.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% y")
                .description("Returns the player's current Y coordinate as a double.")
                .example("set py to get player's y")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLocation().getY()", PrimitiveType.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% z")
                .description("Returns the player's current Z coordinate as a double.")
                .example("set pz to get player's z")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLocation().getZ()", PrimitiveType.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% uuid")
                .description("Returns the player's UUID as a string.")
                .example("set id to get player's uuid")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getUniqueId().toString()",
                        PrimitiveType.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% (xp level|level)")
                .description("Returns a player's experience level as an integer.")
                .example("set lv to get player's xp level")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLevel()",
                        PrimitiveType.INT)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% eye location")
                .description("Returns the location of a player's eyes.")
                .example("set loc to get player's eye location")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getEyeLocation()",
                        MinecraftTypes.LOCATION)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% direction x")
                .description("Returns the X component of the direction a player is looking.")
                .example("set dx to get player's direction x")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("who") + ".getLocation().getDirection().getX()", PrimitiveType.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% direction y")
                .description("Returns the Y component of the direction a player is looking.")
                .example("set dy to get player's direction y")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("who") + ".getLocation().getDirection().getY()", PrimitiveType.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% direction z")
                .description("Returns the Z component of the direction a player is looking.")
                .example("set dz to get player's direction z")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("who") + ".getLocation().getDirection().getZ()", PrimitiveType.DOUBLE)));
    }
}
