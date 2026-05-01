package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenContext;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

/**
 * Registers condition patterns for checking active potion effects
 * on players and living entities.
 */
@Registration
@SuppressWarnings("unused")
public final class PotionEffectConditions {

    private static final String POTION_EFFECT = PotionEffect.class.getName();
    private static final String POTION_EFFECT_TYPE = PotionEffectType.class.getName();
    private static final String LIVING_ENTITY = LivingEntity.class.getName();

    private static void potionImports(@NotNull CodegenContext c) {
        c.addImport(POTION_EFFECT);
        c.addImport(POTION_EFFECT_TYPE);
    }

    private static void entityPotionImports(@NotNull CodegenContext c) {
        potionImports(c);
        c.addImport(LIVING_ENTITY);
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% has [effect] %effect:STRING%")
                .description("Checks if a player has a specific active potion effect.")
                .example("if player has effect \"speed\":")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    ctx.codegen().addImport(POTION_EFFECT_TYPE);
                    return "(" + ctx.requireVarHandle("p").java() + ".hasPotionEffect(PotionEffectType.getByName((" + ctx.java("effect") + ").toUpperCase().replace(\" \", \"_\"))))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% does not have [effect] %effect:STRING%")
                .description("Checks if a player does not have a specific active potion effect.")
                .example("if player does not have effect \"speed\":")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    ctx.codegen().addImport(POTION_EFFECT_TYPE);
                    return "!(" + ctx.requireVarHandle("p").java() + ".hasPotionEffect(PotionEffectType.getByName((" + ctx.java("effect") + ").toUpperCase().replace(\" \", \"_\"))))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%e:ENTITY% has [effect] %effect:STRING%")
                .description("Checks if a living entity has a specific active potion effect.")
                .example("if entity has effect \"poison\":")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    entityPotionImports(ctx.codegen());
                    return "(" + ctx.requireVarHandle("e").java() + " instanceof LivingEntity __le && __le.hasPotionEffect(PotionEffectType.getByName((" + ctx.java("effect") + ").toUpperCase().replace(\" \", \"_\"))))";
                }));
    }
}
