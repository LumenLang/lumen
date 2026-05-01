package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenContext;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Registers statement patterns for applying, removing, and clearing potion effects
 * on players and living entities.
 */
@Registration
@SuppressWarnings("unused")
public final class PotionEffectStatements {

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
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("apply %effect:STRING% %level:INT% to %who:PLAYER% for %duration:INT% ticks")
                .description("Applies a potion effect with the given level and duration (in ticks) to a player.")
                .example("apply \"speed\" 2 to player for 200 ticks")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    potionImports(ctx.codegen());
                    String effect = ctx.java("effect");
                    String level = ctx.java("level");
                    String who = ctx.java("who");
                    String duration = ctx.java("duration");
                    ctx.out().line("{ PotionEffectType __pt = PotionEffectType.getByName((" + effect + ").toUpperCase().replace(\" \", \"_\"));");
                    ctx.out().line("if (__pt != null) " + who + ".addPotionEffect(new PotionEffect(__pt, " + duration + ", (" + level + ") - 1)); }");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("apply %effect:STRING% %level:INT% to %who:ENTITY% for %duration:INT% ticks")
                .description("Applies a potion effect with the given level and duration (in ticks) to a living entity.")
                .example("apply \"regeneration\" 1 to entity for 100 ticks")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    entityPotionImports(ctx.codegen());
                    String effect = ctx.java("effect");
                    String level = ctx.java("level");
                    String who = ctx.java("who");
                    String duration = ctx.java("duration");
                    ctx.out().line("{ PotionEffectType __pt = PotionEffectType.getByName((" + effect + ").toUpperCase().replace(\" \", \"_\"));");
                    ctx.out().line("if (__pt != null && " + who + " instanceof LivingEntity __le) __le.addPotionEffect(new PotionEffect(__pt, " + duration + ", (" + level + ") - 1)); }");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove [effect] %effect:STRING% from %who:PLAYER%")
                .description("Removes a specific potion effect from a player.")
                .example("remove effect \"speed\" from player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    ctx.codegen().addImport(POTION_EFFECT_TYPE);
                    String effect = ctx.java("effect");
                    String who = ctx.java("who");
                    ctx.out().line("{ PotionEffectType __pt = PotionEffectType.getByName((" + effect + ").toUpperCase().replace(\" \", \"_\"));");
                    ctx.out().line("if (__pt != null) " + who + ".removePotionEffect(__pt); }");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove [effect] %effect:STRING% from %who:ENTITY%")
                .description("Removes a specific potion effect from a living entity.")
                .example("remove effect \"speed\" from entity")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    entityPotionImports(ctx.codegen());
                    String effect = ctx.java("effect");
                    String who = ctx.java("who");
                    ctx.out().line("{ PotionEffectType __pt = PotionEffectType.getByName((" + effect + ").toUpperCase().replace(\" \", \"_\"));");
                    ctx.out().line("if (__pt != null && " + who + " instanceof LivingEntity __le) __le.removePotionEffect(__pt); }");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear [all] effects from %who:PLAYER%")
                .description("Removes all active potion effects from a player.")
                .example("clear all effects from player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    ctx.codegen().addImport(POTION_EFFECT);
                    ctx.codegen().addImport(ArrayList.class.getName());
                    String who = ctx.java("who");
                    ctx.out().line("for (PotionEffect __pe : new ArrayList<>(" + who + ".getActivePotionEffects())) " + who + ".removePotionEffect(__pe.getType());");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear [all] effects from %who:ENTITY%")
                .description("Removes all active potion effects from a living entity.")
                .example("clear all effects from entity")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    entityPotionImports(ctx.codegen());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    String who = ctx.java("who");
                    ctx.out().line("if (" + who + " instanceof LivingEntity __le) for (PotionEffect __pe : new ArrayList<>(__le.getActivePotionEffects())) __le.removePotionEffect(__pe.getType());");
                }));
    }
}
