package net.vansencool.lumen.plugin.defaults.condition;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.CodegenAccess;
import net.vansencool.lumen.api.pattern.Categories;
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

    private static void potionImports(@NotNull CodegenAccess c) {
        c.addImport(POTION_EFFECT);
        c.addImport(POTION_EFFECT_TYPE);
    }

    private static void entityPotionImports(@NotNull CodegenAccess c) {
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
                .handler((match, env, ctx) -> {
                    ctx.addImport(POTION_EFFECT_TYPE);
                    return "(" + match.ref("p").java() + ".hasPotionEffect(PotionEffectType.getByName((" + match.java("effect", ctx, env) + ").toUpperCase().replace(\" \", \"_\"))))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% does not have [effect] %effect:STRING%")
                .description("Checks if a player does not have a specific active potion effect.")
                .example("if player does not have effect \"speed\":")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> {
                    ctx.addImport(POTION_EFFECT_TYPE);
                    return "!(" + match.ref("p").java() + ".hasPotionEffect(PotionEffectType.getByName((" + match.java("effect", ctx, env) + ").toUpperCase().replace(\" \", \"_\"))))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%e:ENTITY% has [effect] %effect:STRING%")
                .description("Checks if a living entity has a specific active potion effect.")
                .example("if entity has effect \"poison\":")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> {
                    entityPotionImports(ctx);
                    return "(" + match.ref("e").java() + " instanceof LivingEntity __le && __le.hasPotionEffect(PotionEffectType.getByName((" + match.java("effect", ctx, env) + ").toUpperCase().replace(\" \", \"_\"))))";
                }));
    }
}
