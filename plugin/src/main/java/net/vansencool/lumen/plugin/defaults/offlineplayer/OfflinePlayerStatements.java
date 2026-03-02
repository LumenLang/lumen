package net.vansencool.lumen.plugin.defaults.offlineplayer;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import org.bukkit.BanList;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all built-in offline-player-related statement patterns.
 *
 * <p>
 * OfflinePlayer operations in Bukkit are synchronous once you have the
 * reference.
 * The expensive part (resolving by name) is handled in the expression layer via
 * async scheduling. Statements here operate on an already-resolved
 * {@code OfflinePlayer} reference.
 */
@Registration
@Description("Registers offline player statements: ban, unban, whitelist, set op")
@SuppressWarnings("unused")
public final class OfflinePlayerStatements {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("ban %who:OFFLINE_PLAYER%")
                .description("Bans an offline player with no reason.")
                .example("ban offlinePlayer")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".banPlayer(null);")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("ban %who:OFFLINE_PLAYER% [for] %reason:STRING%")
                .description("Bans an offline player with a reason.")
                .example("ban offlinePlayer for \"Cheating\"")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((line, ctx, out) -> out
                        .line(ctx.java("who") + ".banPlayer(" + ctx.java("reason") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("unban %who:OFFLINE_PLAYER%")
                .description("Unbans an offline player (legacy, whitelists then bans with null).")
                .example("unban offlinePlayer")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".setWhitelisted(true); "
                        + ctx.java("who") + ".banPlayer(null);")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(pardon|unban) %who:OFFLINE_PLAYER%")
                .description("Pardons (unbans) an offline player from the server ban list.")
                .example("pardon offlinePlayer")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(BanList.class.getName());
                    out.line("{");
                    out.line("BanList banList = Bukkit.getBanList(BanList.Type.NAME);");
                    out.line("banList.pardon(" + ctx.java("who") + ".getName());");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(whitelist|allowlist) %who:OFFLINE_PLAYER%")
                .description("Adds an offline player to the server whitelist.")
                .example("whitelist offlinePlayer")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".setWhitelisted(true);")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(unwhitelist|dewhitelist|remove from whitelist) %who:OFFLINE_PLAYER%")
                .description("Removes an offline player from the server whitelist.")
                .example("unwhitelist offlinePlayer")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".setWhitelisted(false);")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %who:OFFLINE_PLAYER_POSSESSIVE% op [to] %val:BOOLEAN%")
                .description("Sets the operator status of an offline player.")
                .example("set offlinePlayer's op to true")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((line, ctx, out) -> out
                        .line(ctx.java("who") + ".setOp(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("op %who:OFFLINE_PLAYER%")
                .description("Grants operator status to an offline player.")
                .example("op offlinePlayer")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".setOp(true);")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("deop %who:OFFLINE_PLAYER%")
                .description("Removes operator status from an offline player.")
                .example("deop offlinePlayer")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".setOp(false);")));
    }
}
