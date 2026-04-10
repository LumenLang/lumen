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
                .pattern("%op:OFFLINE_PLAYER% (is|is not) banned")
                .description("Checks if an offline player is or is not banned.")
                .examples("if offlinePlayer is banned:", "if offlinePlayer is not banned:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> {
                    boolean negated = match.choice(0).equals("is not");
                    return (negated ? "!" : "") + match.ref("op").java() + ".isBanned()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% (is|is not) whitelisted")
                .description("Checks if an offline player is or is not whitelisted.")
                .examples("if offlinePlayer is whitelisted:", "if offlinePlayer is not whitelisted:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> {
                    boolean negated = match.choice(0).equals("is not");
                    return (negated ? "!" : "") + match.ref("op").java() + ".isWhitelisted()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% (is|is not) op")
                .description("Checks if an offline player has or does not have operator status.")
                .examples("if offlinePlayer is op:", "if offlinePlayer is not op:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> {
                    boolean negated = match.choice(0).equals("is not");
                    return (negated ? "!" : "") + match.ref("op").java() + ".isOp()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% (has|has not) played before")
                .description("Checks if an offline player has or has not played on the server before.")
                .examples("if offlinePlayer has played before:", "if offlinePlayer has not played before:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> {
                    boolean negated = match.choice(0).equals("has not");
                    return (negated ? "!" : "") + match.ref("op").java() + ".hasPlayedBefore()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%op:OFFLINE_PLAYER% (is online|is offline)")
                .description("Checks if an offline player is currently online or offline.")
                .examples("if offlinePlayer is online:", "if offlinePlayer is offline:")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((match, env, ctx) -> {
                    boolean negated = match.choice(0).equals("is offline");
                    return (negated ? "!" : "") + match.ref("op").java() + ".isOnline()";
                }));
    }
}
