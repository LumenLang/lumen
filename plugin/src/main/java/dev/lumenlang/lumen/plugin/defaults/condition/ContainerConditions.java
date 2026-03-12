package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.bukkit.block.Container;
import org.jetbrains.annotations.NotNull;

/**
 * Registers condition patterns for checking container block inventories
 * (chests, barrels, dispensers, hoppers, etc.).
 */
@Registration
@SuppressWarnings("unused")
public final class ContainerConditions {

    private static final String CONTAINER = Container.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%b:BLOCK% inventory is empty")
                .description("Checks if a container block's inventory is empty.")
                .example("if block inventory is empty:")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((match, env, ctx) -> {
                    ctx.addImport(CONTAINER);
                    String block = match.java("b", ctx, env);
                    return "(" + block + ".getState() instanceof Container __ct && __ct.getInventory().isEmpty())";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%b:BLOCK% inventory is not empty")
                .description("Checks if a container block's inventory is not empty.")
                .example("if block inventory is not empty:")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((match, env, ctx) -> {
                    ctx.addImport(CONTAINER);
                    String block = match.java("b", ctx, env);
                    return "(" + block + ".getState() instanceof Container __ct && !__ct.getInventory().isEmpty())";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%b:BLOCK% is [a] container")
                .description("Checks if a block is a container (chest, barrel, hopper, etc.).")
                .example("if block is a container:")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((match, env, ctx) -> {
                    ctx.addImport(CONTAINER);
                    String block = match.java("b", ctx, env);
                    return block + ".getState() instanceof Container";
                }));
    }
}
