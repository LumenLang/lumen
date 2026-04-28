package dev.lumenlang.lumen.plugin.defaults.condition.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

/**
 * Registers condition patterns that work on any {@code Entity}.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class GenericEntityConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% (is|is not) on fire")
                .description("Checks if an entity is or is not currently on fire.")
                .examples("if mob is on fire:", "if mob is not on fire:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return ctx.requireVarHandle("e").java() + ".getFireTicks() " + (negated ? "<= 0" : "> 0");
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% (is|is not) on ground")
                .description("Checks if an entity is or is not on the ground.")
                .examples("if mob is on ground:", "if mob is not on ground:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + ctx.requireVarHandle("e").java() + ".isOnGround()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% (is|is not) in water")
                .description("Checks if an entity is or is not in water.")
                .examples("if mob is in water:", "if mob is not in water:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + ctx.requireVarHandle("e").java() + ".isInWater()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY_POSSESSIVE% type (is|is not) %type:ENTITY_TYPE%")
                .description("Checks if an entity's type matches or does not match (prefix syntax).")
                .examples("if mob's type is zombie:", "if mob's type is not zombie:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return ctx.requireVarHandle("e").java() + ".getType() " + (negated ? "!= " : "== ") + ctx.java("type");
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% (is|is not) [a] %type:ENTITY_TYPE%")
                .description("Checks if an entity's type matches or does not match.")
                .examples("if mob is a zombie:", "if mob is not a zombie:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return ctx.requireVarHandle("e").java() + ".getType() " + (negated ? "!= " : "== ") + ctx.java("type");
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% (has|does not have) custom name")
                .description("Checks if an entity has or does not have a custom name set.")
                .examples("if mob has custom name:", "if mob does not have custom name:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("does not have");
                    return ctx.requireVarHandle("e").java() + ".getCustomName() " + (negated ? "== null" : "!= null");
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% (is|is not) valid")
                .description("Checks if an entity reference is still valid (not removed).")
                .examples("if mob is valid:", "if mob is not valid:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + ctx.requireVarHandle("e").java() + ".isValid()";
                }));
    }
}
