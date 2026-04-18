package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Registers world-related expression patterns.
 */
@Registration
@SuppressWarnings("unused")
public final class WorldExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get world %name:STRING%")
                .description("Looks up a world by name. Returns null if no world with that name exists.")
                .example("set w to get world \"world_nether\"")
                .since("1.0.0")
                .category(Categories.WORLD)
                .handler(ctx -> new ExpressionResult(
                        "Bukkit.getWorld(" + ctx.java("name") + ")",
                        MinecraftTypes.WORLD.wrapAsNullable())));
    }
}