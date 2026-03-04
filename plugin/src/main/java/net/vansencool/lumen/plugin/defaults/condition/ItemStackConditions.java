package net.vansencool.lumen.plugin.defaults.condition;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
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
                .by("Lumen").pattern("%i:ITEMSTACK% has (display name|name)")
                .description("Checks if an item stack has a custom display name.")
                .example("if item has display name:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().hasDisplayName())"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% has lore")
                .description("Checks if an item stack has lore set.")
                .example("if item has lore:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().hasLore())"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% has enchantments")
                .description("Checks if an item stack has any enchantments.")
                .example("if item has enchantments:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "!" + match.ref("i").java() + ".getEnchantments().isEmpty()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% (does not have|has no) (display name|name)")
                .description("Checks if an item stack does not have a custom display name.")
                .example("if item does not have display name:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(!(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().hasDisplayName()))"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% (does not have|has no) lore")
                .description("Checks if an item stack does not have lore.")
                .example("if item does not have lore:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(!(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().hasLore()))"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% is unbreakable")
                .description("Checks if an item stack is unbreakable.")
                .example("if item is unbreakable:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().isUnbreakable())"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% is not unbreakable")
                .description("Checks if an item stack is not unbreakable.")
                .example("if item is not unbreakable:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(!(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().isUnbreakable()))"));
    }
}
