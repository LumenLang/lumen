package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.util.SoundHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Registers built-in server-related statement patterns.
 */
@Registration
@SuppressWarnings("unused")
public final class ServerStatements {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(use|import) %clazz:STRING%")
                .description("Adds a Java import to the compiled script.")
                .example("use org.bukkit.Material")
                .since("1.0.0")
                .category(Categories.SERVER)
                .handler((line, ctx, out) -> ctx.codegen().addImport(ctx.java("clazz"))));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("spawn %type:ENTITY_TYPE% [at] %loc:LOCATION%")
                .description("Spawns an entity of the given type at a location.")
                .example("spawn zombie at loc")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> out.line(ctx.java("loc") + ".getWorld().spawnEntity("
                        + ctx.java("loc") + ", " + ctx.java("type") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(broadcast|announce) %text:STRING%")
                .description("Broadcasts a formatted message to all online players.")
                .example("broadcast \"Server restarting in 5 minutes!\"")
                .since("1.0.0")
                .category(Categories.SERVER)
                .handler((line, ctx, out) -> out.line("LumenText.broadcast(" + ctx.java("text") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set time [of] %w:WORLD% [to] %val:INT%")
                .description("Sets the time of a world.")
                .example("set time of world to 6000")
                .since("1.0.0")
                .category(Categories.WORLD)
                .handler((line, ctx, out) -> out
                        .line(ctx.java("w") + ".setTime(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(execute|run|perform) [command] %cmd:STRING% as %who:PLAYER%")
                .description("Executes a command as a player.")
                .example("execute \"give @s diamond\" as player")
                .since("1.0.0")
                .category(Categories.SERVER)
                .handler((line, ctx, out) -> out.line("Bukkit.dispatchCommand("
                        + ctx.java("who") + ", " + ctx.java("cmd") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(execute|run|perform) console [command] %cmd:STRING%")
                .description("Executes a command as the console.")
                .example("execute console \"say Hello\"")
                .since("1.0.0")
                .category(Categories.SERVER)
                .handler((line, ctx, out) -> out.line("Bukkit.dispatchCommand(Bukkit.getConsoleSender(), " + ctx.java("cmd") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("play sound %sound:STRING% [at|to] %who:PLAYER%")
                .description("Plays a sound to a player at their location. Accepts Bukkit Sound enum names or custom sound strings. Dots are converted to underscores for enum lookup.")
                .example("play sound \"entity_experience_orb_pickup\" to player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(SoundHelper.class.getName());
                    out.line(SoundHelper.class.getSimpleName() + ".playSound(" + ctx.java("who") + ", " + ctx.java("sound") + ");");
                }));
    }
}
