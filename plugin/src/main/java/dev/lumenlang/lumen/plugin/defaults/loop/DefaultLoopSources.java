package dev.lumenlang.lumen.plugin.defaults.loop;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.LoopHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the built-in loop sources that can be used with
 * {@code loop X in <source>:} blocks.
 */
@Registration(order = 11)
@SuppressWarnings("unused")
public final class DefaultLoopSources {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().loop(b -> b
                .by("Lumen")
                .patterns("all players", "all online players")
                .description("Iterates over every online player on the server.")
                .example("loop p in all players:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    ctx.codegen().addImport(Bukkit.class.getName());
                    return new LoopHandler.LoopResult("Bukkit.getOnlinePlayers()", MinecraftTypes.PLAYER);
                }));

        api.patterns().loop(b -> b
                .by("Lumen")
                .pattern("all entities in %world:WORLD%")
                .description("Iterates over every entity in the given world.")
                .example("loop e in all entities in world:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    String worldJava = ctx.java("world");
                    return new LoopHandler.LoopResult(worldJava + ".getEntities()", MinecraftTypes.ENTITY);
                }));

        api.patterns().loop(b -> b
                .by("Lumen")
                .pattern("all worlds")
                .description("Iterates over every loaded world on the server.")
                .example("loop w in all worlds:")
                .since("1.0.0")
                .category(Categories.WORLD)
                .handler(ctx -> {
                    ctx.codegen().addImport(Bukkit.class.getName());
                    return new LoopHandler.LoopResult("Bukkit.getWorlds()", MinecraftTypes.WORLD);
                }));
    }
}
