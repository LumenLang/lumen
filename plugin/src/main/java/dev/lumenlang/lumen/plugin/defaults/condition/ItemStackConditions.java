package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

/**
 * Registers condition patterns for checking item stack properties: display name,
 * lore, enchantments, and unbreakable status.
 */
@Registration
@SuppressWarnings("unused")
public final class ItemStackConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% (has|does not have|has no) (display name|name)")
                .description("Checks if an item stack has or does not have a custom display name.")
                .examples("if item has display name:", "if item does not have display name:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) -> {
                    boolean negated = !match.choice(0).equals("has");
                    String expr = match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().hasDisplayName()";
                    return negated ? "(!(" + expr + "))" : "(" + expr + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% (has|does not have|has no) lore")
                .description("Checks if an item stack has or does not have lore set.")
                .examples("if item has lore:", "if item does not have lore:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) -> {
                    boolean negated = !match.choice(0).equals("has");
                    String expr = match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().hasLore()";
                    return negated ? "(!(" + expr + "))" : "(" + expr + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% (has|does not have|has no) enchantments")
                .description("Checks if an item stack has or does not have any enchantments.")
                .examples("if item has enchantments:", "if item does not have enchantments:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) -> {
                    boolean negated = !match.choice(0).equals("has");
                    return (negated ? "" : "!") + match.ref("i").java() + ".getEnchantments().isEmpty()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% (is|is not) unbreakable")
                .description("Checks if an item stack is or is not unbreakable.")
                .examples("if item is unbreakable:", "if item is not unbreakable:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) -> {
                    boolean negated = match.choice(0).equals("is not");
                    String expr = match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().isUnbreakable()";
                    return negated ? "(!(" + expr + "))" : "(" + expr + ")";
                }));
    }
}
