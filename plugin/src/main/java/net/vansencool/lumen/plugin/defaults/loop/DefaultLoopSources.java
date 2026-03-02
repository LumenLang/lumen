package net.vansencool.lumen.plugin.defaults.loop;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.handler.LoopHandler;
import net.vansencool.lumen.api.pattern.Categories;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the built-in loop sources that can be used with
 * {@code loop X in <source>:} blocks.
 */
@Registration(order = 11)
@Description("Registers the built-in loop sources: all players, all entities, all worlds.")
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
                .handler(allPlayers()));

        api.patterns().loop(b -> b
                .by("Lumen")
                .pattern("all entities in %world:WORLD%")
                .description("Iterates over every entity in the given world.")
                .example("loop e in all entities in world:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(allEntitiesInWorld()));

        api.patterns().loop(b -> b
                .by("Lumen")
                .pattern("all worlds")
                .description("Iterates over every loaded world on the server.")
                .example("loop w in all worlds:")
                .since("1.0.0")
                .category(Categories.WORLD)
                .handler(allWorlds()));
    }

    private @NotNull LoopHandler allPlayers() {
        return ctx -> {
            ctx.codegen().addImport(Bukkit.class.getName());
            return new LoopHandler.LoopResult("Bukkit.getOnlinePlayers()", "PLAYER");
        };
    }

    private @NotNull LoopHandler allEntitiesInWorld() {
        return ctx -> {
            String worldJava = ctx.java("world");
            return new LoopHandler.LoopResult(worldJava + ".getEntities()", "ENTITY");
        };
    }

    private @NotNull LoopHandler allWorlds() {
        return ctx -> {
            ctx.codegen().addImport(Bukkit.class.getName());
            return new LoopHandler.LoopResult("Bukkit.getWorlds()", "WORLD");
        };
    }
}
