package dev.lumenlang.lumen.plugin.defaults.statement.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.defaults.util.AttributeNames;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Registers statement patterns that require a {@code LivingEntity}.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class LivingEntityStatements {

    private static final String LIVING_ENTITY = LivingEntity.class.getName();
    private static final String ATTRIBUTE = Attribute.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen").pattern("(kill|slay) %e:LIVING_ENTITY%")
                .description("Kills a living entity by setting its health to zero.")
                .example("kill mob")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    ctx.codegen().addImport(LIVING_ENTITY);
                    ctx.out().line(ctx.java("e") + ".setHealth(0);");
                }));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:LIVING_ENTITY_POSSESSIVE% health [to] %val:INT%")
                .description("Sets a living entity's health to the specified value.")
                .example("set mob's health to 10")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    ctx.codegen().addImport(LIVING_ENTITY);
                    ctx.out().line(ctx.java("e") + ".setHealth(" + ctx.java("val") + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("damage %e:LIVING_ENTITY% [by] %val:INT%")
                .description("Deals damage to a living entity.")
                .example("damage mob by 5")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    ctx.codegen().addImport(LIVING_ENTITY);
                    ctx.out().line(ctx.java("e") + ".damage(" + ctx.java("val") + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("(heal|restore) [the] %e:LIVING_ENTITY%")
                .description("Fully heals a living entity to its max health.")
                .example("heal mob")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    String attrName = AttributeNames.resolve("max_health");
                    ctx.codegen().addImport(LIVING_ENTITY);
                    ctx.codegen().addImport(ATTRIBUTE);
                    ctx.out().line(ctx.java("e") + ".setHealth(" + ctx.java("e") + ".getAttribute(Attribute." + attrName + ").getValue());");
                }));
    }
}
