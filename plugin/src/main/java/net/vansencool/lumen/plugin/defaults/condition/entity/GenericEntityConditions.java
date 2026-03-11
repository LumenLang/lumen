package net.vansencool.lumen.plugin.defaults.condition.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

/**
 * Registers condition patterns that work on any {@code Entity}.
 */
@Registration
@SuppressWarnings("unused")
public final class GenericEntityConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is on fire")
                .description("Checks if an entity is currently on fire.")
                .example("if mob is on fire:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getFireTicks() > 0"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is not on fire")
                .description("Checks if an entity is not on fire.")
                .example("if mob is not on fire:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getFireTicks() <= 0"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is on ground")
                .description("Checks if an entity is on the ground.")
                .example("if mob is on ground:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".isOnGround()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is not on ground")
                .description("Checks if an entity is not on the ground.")
                .example("if mob is not on ground:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> "!" + match.ref("e").java() + ".isOnGround()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is in water")
                .description("Checks if an entity is in water.")
                .example("if mob is in water:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".isInWater()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is not in water")
                .description("Checks if an entity is not in water.")
                .example("if mob is not in water:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> "!" + match.ref("e").java() + ".isInWater()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY_POSSESSIVE% type is %type:ENTITY_TYPE%")
                .description("Checks if an entity's type matches (prefix syntax).")
                .example("if mob's type is zombie:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getType() == " + match.java("type", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY_POSSESSIVE% type is not %type:ENTITY_TYPE%")
                .description("Checks if an entity's type does not match (prefix syntax).")
                .example("if mob's type is not zombie:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getType() != " + match.java("type", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is [a] %type:ENTITY_TYPE%")
                .description("Checks if an entity's type matches.")
                .example("if mob is a zombie:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getType() == " + match.java("type", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is not [a] %type:ENTITY_TYPE%")
                .description("Checks if an entity's type does not match.")
                .example("if mob is not a zombie:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getType() != " + match.java("type", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% has custom name")
                .description("Checks if an entity has a custom name set.")
                .example("if mob has custom name:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".getCustomName() != null"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is valid")
                .description("Checks if an entity reference is still valid (not removed).")
                .example("if mob is valid:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> match.ref("e").java() + ".isValid()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is not valid")
                .description("Checks if an entity reference is no longer valid.")
                .example("if mob is not valid:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> "!" + match.ref("e").java() + ".isValid()"));
    }
}
