package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Registers variable-related expression patterns including typed null values.
 */
@Registration
@SuppressWarnings("unused")
public final class VariableExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("no location")
                .description("Represents a null location value. The variable will carry the LOCATION type at compile time.")
                .example("global scoped pos1 with default no location")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    ctx.codegen().addImport(Location.class.getName());
                    return new ExpressionResult("(Location) null", MinecraftTypes.LOCATION.wrapAsNullable());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("no player")
                .description("Represents a null player value. The variable will carry the PLAYER type at compile time.")
                .example("global scoped target with default no player")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> new ExpressionResult("(Player) null", MinecraftTypes.PLAYER.wrapAsNullable())));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("no entity")
                .description("Represents a null entity value. The variable will carry the ENTITY type at compile time.")
                .example("global scoped target with default no entity")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    ctx.codegen().addImport(Entity.class.getName());
                    return new ExpressionResult("(Entity) null", MinecraftTypes.ENTITY.wrapAsNullable());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("no world")
                .description("Represents a null world value. The variable will carry the WORLD type at compile time.")
                .example("global scoped w with default no world")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    ctx.codegen().addImport(World.class.getName());
                    return new ExpressionResult("(World) null", MinecraftTypes.WORLD.wrapAsNullable());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("no block")
                .description("Represents a null block value. The variable will carry the BLOCK type at compile time.")
                .example("global scoped target_block with default no block")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    ctx.codegen().addImport(Block.class.getName());
                    return new ExpressionResult("(Block) null", MinecraftTypes.BLOCK.wrapAsNullable());
                }));
    }
}