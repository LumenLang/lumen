package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.Lumen;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all persistent data container condition patterns for entities, items, and blocks.
 */
@Registration
@SuppressWarnings("unused")
public final class PersistentDataConditions {

    private static final String NAMESPACED_KEY = NamespacedKey.class.getName();
    private static final String PDC_TYPE = PersistentDataType.class.getName();
    private static final String LUMEN = Lumen.class.getName();

    private static void pdcImports(@NotNull CodegenAccess c) {
        c.addImport(NAMESPACED_KEY);
        c.addImport(PDC_TYPE);
        c.addImport(LUMEN);
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% has pdc %key:STRING%")
                .description("Checks if an entity's PersistentDataContainer has a given key.")
                .example("if entity has pdc \"myKey\":")
                .since("1.0.0").category(Categories.ENTITY)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    return ctx.requireVarHandle("e").java() + ".getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + ctx.java("key") + "), PersistentDataType.STRING)";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% (does not have|has no) pdc %key:STRING%")
                .description("Checks if an entity's PersistentDataContainer does not have a given key.")
                .example("if entity does not have pdc \"myKey\":")
                .since("1.0.0").category(Categories.ENTITY)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    return "!" + ctx.requireVarHandle("e").java() + ".getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + ctx.java("key") + "), PersistentDataType.STRING)";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% has pdc %key:STRING%")
                .description("Checks if an item stack's PersistentDataContainer has a given key.")
                .example("if item has pdc \"custom_id\":")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    return "(" + ctx.requireVarHandle("i").java() + ".hasItemMeta() && " + ctx.requireVarHandle("i").java()
                            + ".getItemMeta().getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + ctx.java("key") + "), PersistentDataType.STRING))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% (does not have|has no) pdc %key:STRING%")
                .description("Checks if an item stack's PersistentDataContainer does not have a given key.")
                .example("if item does not have pdc \"custom_id\":")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    return "(!(" + ctx.requireVarHandle("i").java() + ".hasItemMeta() && " + ctx.requireVarHandle("i").java()
                            + ".getItemMeta().getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + ctx.java("key") + "), PersistentDataType.STRING)))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%bl:BLOCK% has pdc %key:STRING%")
                .description("Checks if a block's PersistentDataContainer has a given key.")
                .example("if block has pdc \"owner\":")
                .since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    return ctx.requireVarHandle("bl").java() + ".getState().getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + ctx.java("key") + "), PersistentDataType.STRING)";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%bl:BLOCK% (does not have|has no) pdc %key:STRING%")
                .description("Checks if a block's PersistentDataContainer does not have a given key.")
                .example("if block does not have pdc \"owner\":")
                .since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    return "!" + ctx.requireVarHandle("bl").java() + ".getState().getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + ctx.java("key") + "), PersistentDataType.STRING)";
                }));
    }
}
