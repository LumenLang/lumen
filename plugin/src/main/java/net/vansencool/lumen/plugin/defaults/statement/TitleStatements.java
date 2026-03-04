package net.vansencool.lumen.plugin.defaults.statement;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.plugin.text.LumenText;
import org.jetbrains.annotations.NotNull;

/**
 * Registers title and action bar statement patterns.
 *
 * <p>Provides statements for sending titles (with optional subtitles) and
 * action bar messages to players. All text is processed through
 * {@link LumenText} for color and MiniMessage support.
 */
@Registration
@SuppressWarnings("unused")
public final class TitleStatements {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("send title %title:STRING% to %who:PLAYER%")
                .description("Sends a title to a player with default timing (10 tick fade-in, 70 tick stay, 20 tick fade-out).")
                .example("send title \"&6Welcome!\" to player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(LumenText.class.getName());
                    out.line("LumenText.sendTitle(" + ctx.java("who") + ", " + ctx.java("title") + ", null, 10, 70, 20);");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("send title %title:STRING% with subtitle %sub:STRING% to %who:PLAYER%")
                .description("Sends a title with subtitle to a player with default timing.")
                .example("send title \"&6Welcome!\" with subtitle \"&7Enjoy your stay\" to player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(LumenText.class.getName());
                    out.line("LumenText.sendTitle(" + ctx.java("who") + ", " + ctx.java("title") + ", " + ctx.java("sub") + ", 10, 70, 20);");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("send title %title:STRING% with subtitle %sub:STRING% to %who:PLAYER% with fade in %fi:INT% stay %st:INT% fade out %fo:INT%")
                .description("Sends a title with subtitle and custom timing (all in ticks) to a player.")
                .example("send title \"&6Welcome!\" with subtitle \"&7Enjoy\" to player with fade in 5 stay 40 fade out 10")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(LumenText.class.getName());
                    out.line("LumenText.sendTitle(" + ctx.java("who") + ", " + ctx.java("title") + ", " + ctx.java("sub") + ", " + ctx.java("fi") + ", " + ctx.java("st") + ", " + ctx.java("fo") + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("send actionbar %text:STRING% to %who:PLAYER%")
                .description("Sends an action bar message to a player.")
                .example("send actionbar \"&aHealth: 20\" to player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(LumenText.class.getName());
                    out.line("LumenText.sendActionBar(" + ctx.java("who") + ", " + ctx.java("text") + ");");
                }));
    }
}
