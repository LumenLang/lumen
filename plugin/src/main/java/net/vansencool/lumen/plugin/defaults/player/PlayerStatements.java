package net.vansencool.lumen.plugin.defaults.player;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.plugin.defaults.attributes.AttributeNames;
import net.vansencool.lumen.plugin.text.LumenText;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Registers all built-in player-related statement patterns.
 */
@Registration
@Description("Registers player statements: message, health, teleport, heal, kill, food, level, gamemode, kick, fly")
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
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(LumenText.class.getName());
                    out.line("LumenText.send(" + ctx.java("who") + ", " + ctx.java("text") + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %who:PLAYER_POSSESSIVE% health [to] %val:INT%")
                .description("Sets a player's health to the given value.")
                .example("set player's health to 20")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> out
                        .line(ctx.java("who") + ".setHealth(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .patterns("(teleport|tp) %who:PLAYER% [to] %target:PLAYER%")
                .description("Teleports a player to another player.")
                .example("teleport player to target")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> out
                        .line(ctx.java("who") + ".teleport(" + ctx.java("target") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(teleport|tp) %who:PLAYER% [to] %loc:LOCATION%")
                .description("Teleports a player to a location.")
                .example("teleport player to loc")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> out
                        .line(ctx.java("who") + ".teleport(" + ctx.java("loc") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(heal|restore) %who:PLAYER%")
                .description("Fully heals a player to their maximum health.")
                .example("heal player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> {
                    String attrName = AttributeNames.resolve("max_health");
                    ctx.codegen().addImport(Attribute.class.getName());
                    out.line(ctx.java("who") + ".setHealth(" + ctx.java("who")
                            + ".getAttribute(Attribute." + attrName + ").getValue());");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(feed|saturate) %who:PLAYER%")
                .description("Fully feeds a player, setting their food level to 20.")
                .example("feed player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".setFoodLevel(20);")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(kill|slay) %who:PLAYER%")
                .description("Kills a player by setting their health to 0.")
                .example("kill player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".setHealth(0);")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %who:PLAYER_POSSESSIVE% food [level] [to] %val:INT%")
                .description("Sets a player's food level to the given value.")
                .example("set player's food level to 10")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> out
                        .line(ctx.java("who") + ".setFoodLevel(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %who:PLAYER_POSSESSIVE% (xp level|level) [to] %val:INT%")
                .description("Sets a player's experience level.")
                .example("set player's xp level to 30")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> out
                        .line(ctx.java("who") + ".setLevel(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %who:PLAYER_POSSESSIVE% (gamemode|gm) [to] %mode:EXPR%")
                .description("Sets a player's gamemode.")
                .example("set player's gamemode to \"creative\"")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(GameMode.class.getName());
                    String mode = ctx.java("mode").replace("\"", "").toUpperCase(Locale.ROOT);
                    out.line(ctx.java("who") + ".setGameMode(GameMode." + mode + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(kick|disconnect) %who:PLAYER% %reason:STRING%")
                .description("Kicks a player from the server with a reason message.")
                .example("kick player \"You have been kicked!\"")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> out
                        .line(ctx.java("who") + ".kickPlayer(" + ctx.java("reason") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(kick|disconnect) %who:PLAYER%")
                .description("Kicks a player from the server without a reason.")
                .example("kick player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".kickPlayer(\"\");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %who:PLAYER_POSSESSIVE% (fly|flight) [mode] [to] %val:BOOLEAN%")
                .description("Enables or disables flight for a player.")
                .example("set player's fly to true")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> {
                    out.line(ctx.java("who") + ".setAllowFlight(" + ctx.java("val") + ");");
                    out.line(ctx.java("who") + ".setFlying(" + ctx.java("val") + ");");
                }));
    }
}
