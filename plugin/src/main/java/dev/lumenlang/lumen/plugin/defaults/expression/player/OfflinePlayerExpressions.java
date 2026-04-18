package dev.lumenlang.lumen.plugin.defaults.expression.player;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Registers offline player expression patterns.
 */
@Registration
@SuppressWarnings("unused")
public final class OfflinePlayerExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get offline player (by|from) (name|username) %name:STRING%")
                .description("Looks up an offline player by name.")
                .example("set op to get offline player by name \"Notch\"")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler(ctx -> {
                    String nameJava = ctx.java("name");
                    ctx.codegen().addImport(OfflinePlayer.class.getName());
                    return new ExpressionResult(
                            "Bukkit.getOfflinePlayer(" + nameJava + ")",
                            MinecraftTypes.OFFLINE_PLAYER);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get offline player (by|from) (uuid|unique id) %uuid:STRING%")
                .description("Looks up an offline player by UUID string.")
                .example("set op to get offline player by uuid \"069a79f4-44e9-4726-a5be-fca90e38aaf5\"")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler(ctx -> {
                    String uuidJava = ctx.java("uuid");
                    ctx.codegen().addImport(OfflinePlayer.class.getName());
                    ctx.codegen().addImport(UUID.class.getName());
                    return new ExpressionResult(
                            "Bukkit.getOfflinePlayer(UUID.fromString(" + uuidJava + "))",
                            MinecraftTypes.OFFLINE_PLAYER);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %op:OFFLINE_PLAYER_POSSESSIVE% name")
                .description("Returns an offline player's name.")
                .example("set name to get offlinePlayer's name")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("op") + ".getName()", PrimitiveType.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %op:OFFLINE_PLAYER_POSSESSIVE% uuid")
                .description("Returns an offline player's UUID as a string.")
                .example("set id to get offlinePlayer's uuid")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("op") + ".getUniqueId().toString()",
                        PrimitiveType.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %op:OFFLINE_PLAYER_POSSESSIVE% first played")
                .description("Returns the timestamp of when the offline player first joined.")
                .example("set time to get offlinePlayer's first played")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("op") + ".getFirstPlayed()", PrimitiveType.LONG)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %op:OFFLINE_PLAYER_POSSESSIVE% last played")
                .description("Returns the timestamp of when the offline player last joined.")
                .example("set time to get offlinePlayer's last played")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("op") + ".getLastPlayed()", PrimitiveType.LONG)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %op:OFFLINE_PLAYER_POSSESSIVE% bed spawn location")
                .description("Returns the offline player's bed spawn location, or null if not set.")
                .example("set loc to get offlinePlayer's bed spawn location")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler(ctx -> new ExpressionResult(ctx.java("op") + ".getBedSpawnLocation()",
                        MinecraftTypes.LOCATION.wrapAsNullable())));
    }
}