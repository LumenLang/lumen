package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.defaults.util.AttributeNames;
import dev.lumenlang.lumen.plugin.text.LumenText;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all built-in player-related statement patterns.
 */
@Registration
@SuppressWarnings("unused")
public final class PlayerStatements {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("message %who:PLAYER% %text:STRING%")
                .description("Sends a formatted message to a player.")
                .example("message player \"Hello, world!\"")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    ctx.codegen().addImport(LumenText.class.getName());
                    ctx.out().line("LumenText.send(" + ctx.java("who") + ", " + ctx.java("text") + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %who:PLAYER_POSSESSIVE% health [to] %val:INT%")
                .description("Sets a player's health to the given value.")
                .example("set player's health to 20")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.out()
                        .line(ctx.java("who") + ".setHealth(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .patterns("(teleport|tp) %who:PLAYER% [to] %target:PLAYER%")
                .description("Teleports a player to another player.")
                .example("teleport player to target")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.out()
                        .line(ctx.java("who") + ".teleport(" + ctx.java("target") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(teleport|tp) %who:PLAYER% [to] %loc:LOCATION%")
                .description("Teleports a player to a location.")
                .example("teleport player to loc")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.out()
                        .line(ctx.java("who") + ".teleport(" + ctx.java("loc") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(heal|restore) [the] %who:PLAYER%")
                .description("Fully heals a player to their maximum health.")
                .example("heal player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    String attrName = AttributeNames.resolve("max_health");
                    ctx.codegen().addImport(Attribute.class.getName());
                    ctx.out().line(ctx.java("who") + ".setHealth(" + ctx.java("who")
                            + ".getAttribute(Attribute." + attrName + ").getValue());");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(feed|saturate) %who:PLAYER%")
                .description("Fully feeds a player, setting their food level to 20.")
                .example("feed player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.out().line(ctx.java("who") + ".setFoodLevel(20);")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(kill|slay) %who:PLAYER%")
                .description("Kills a player by setting their health to 0.")
                .example("kill player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.out().line(ctx.java("who") + ".setHealth(0);")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %who:PLAYER_POSSESSIVE% food [level] [to] %val:INT%")
                .description("Sets a player's food level to the given value.")
                .example("set player's food level to 10")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.out()
                        .line(ctx.java("who") + ".setFoodLevel(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %who:PLAYER_POSSESSIVE% (xp level|level) [to] %val:INT%")
                .description("Sets a player's experience level.")
                .example("set player's xp level to 30")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.out()
                        .line(ctx.java("who") + ".setLevel(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %who:PLAYER_POSSESSIVE% (gamemode|gm) [to] %mode:GAME_MODE%")
                .description("Sets a player's gamemode.")
                .example("set player's gamemode to \"creative\"")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.out().line(ctx.java("who") + ".setGameMode(" + ctx.java("mode") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(kick|disconnect) %who:PLAYER% %reason:STRING%")
                .description("Kicks a player from the server with a reason message.")
                .example("kick player \"You have been kicked!\"")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.out()
                        .line(ctx.java("who") + ".kickPlayer(" + ctx.java("reason") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(kick|disconnect) %who:PLAYER%")
                .description("Kicks a player from the server without a reason.")
                .example("kick player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.out().line(ctx.java("who") + ".kickPlayer(\"\");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %who:PLAYER_POSSESSIVE% (fly|flight) [mode] [to] %val:BOOLEAN%")
                .description("Enables or disables flight for a player.")
                .example("set player's fly to true")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    ctx.out().line(ctx.java("who") + ".setAllowFlight(" + ctx.java("val") + ");");
                    ctx.out().line(ctx.java("who") + ".setFlying(" + ctx.java("val") + ");");
                }));
    }
}
