package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

/**
 * Registers offline player related condition patterns.
 */
@Registration
@SuppressWarnings("unused")
public final class OfflinePlayerConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% is banned")
                .description("Checks if an offline player is banned.")
                .example("if offlinePlayer is banned:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> match.ref("op").java() + ".isBanned()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% is not banned")
                .description("Checks if an offline player is not banned.")
                .example("if offlinePlayer is not banned:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> "!" + match.ref("op").java() + ".isBanned()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% is whitelisted")
                .description("Checks if an offline player is whitelisted.")
                .example("if offlinePlayer is whitelisted:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> match.ref("op").java() + ".isWhitelisted()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% is not whitelisted")
                .description("Checks if an offline player is not whitelisted.")
                .example("if offlinePlayer is not whitelisted:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> "!" + match.ref("op").java() + ".isWhitelisted()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% is op")
                .description("Checks if an offline player has operator status.")
                .example("if offlinePlayer is op:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> match.ref("op").java() + ".isOp()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% is not op")
                .description("Checks if an offline player does not have operator status.")
                .example("if offlinePlayer is not op:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> "!" + match.ref("op").java() + ".isOp()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% has played before")
                .description("Checks if an offline player has played on the server before.")
                .example("if offlinePlayer has played before:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> match.ref("op").java() + ".hasPlayedBefore()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% has not played before")
                .description("Checks if an offline player has never played on the server.")
                .example("if offlinePlayer has not played before:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> "!" + match.ref("op").java() + ".hasPlayedBefore()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% is online")
                .description("Checks if an offline player is currently online.")
                .example("if offlinePlayer is online:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> match.ref("op").java() + ".isOnline()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% is offline")
                .description("Checks if an offline player is currently offline.")
                .example("if offlinePlayer is offline:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> "!" + match.ref("op").java() + ".isOnline()"));
    }
}
