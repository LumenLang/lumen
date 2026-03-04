package net.vansencool.lumen.plugin.defaults.condition;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.plugin.util.EnumValidation;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * Registers runtime validation conditions for enum-backed types such as
 * {@link Material}, {@link EntityType}, and {@link Attribute}.
 */
@Registration
@SuppressWarnings("unused")
public final class ValidationConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        registerMaterial(api);
        registerEntityType(api);
        registerAttribute(api);
    }

    private void registerMaterial(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%val:EXPR% is [a] valid material")
                .description("Checks if a string value is a valid Bukkit Material name.")
                .example("if mat is a valid material:")
                .since("1.0.0")
                .category(Categories.createOrGet("Validation"))
                .handler((match, env, ctx) -> {
                    ctx.addImport(Material.class.getName());
                    return "(Material.matchMaterial(String.valueOf(" + match.java("val", ctx, env)
                            + ").toUpperCase()) != null)";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%val:EXPR% is not [a] valid material")
                .description("Checks if a string value is not a valid Bukkit Material name.")
                .example("if mat is not a valid material:")
                .since("1.0.0")
                .category(Categories.createOrGet("Validation"))
                .handler((match, env, ctx) -> {
                    ctx.addImport(Material.class.getName());
                    return "(Material.matchMaterial(String.valueOf(" + match.java("val", ctx, env)
                            + ").toUpperCase()) == null)";
                }));
    }

    private void registerEntityType(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%val:EXPR% is [a] valid entity type")
                .description("Checks if a string value is a valid Bukkit EntityType name.")
                .example("if type is a valid entity type:")
                .since("1.0.0")
                .category(Categories.createOrGet("Validation"))
                .handler((match, env, ctx) -> {
                    ctx.addImport(EnumValidation.class.getName());
                    return "EnumValidation.isValidEntityType(String.valueOf(" + match.java("val", ctx, env) + "))"; 
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%val:EXPR% is not [a] valid entity type")
                .description("Checks if a string value is not a valid Bukkit EntityType name.")
                .example("if type is not a valid entity type:")
                .since("1.0.0")
                .category(Categories.createOrGet("Validation"))
                .handler((match, env, ctx) -> {
                    ctx.addImport(EnumValidation.class.getName());
                    return "!EnumValidation.isValidEntityType(String.valueOf(" + match.java("val", ctx, env) + "))"; 
                }));
    }

    private void registerAttribute(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%val:EXPR% is [a] valid attribute")
                .description("Checks if a string value is a valid Bukkit Attribute name.")
                .example("if attr is a valid attribute:")
                .since("1.0.0")
                .category(Categories.createOrGet("Validation"))
                .handler((match, env, ctx) -> {
                    ctx.addImport(EnumValidation.class.getName());
                    return "EnumValidation.isValidAttribute(String.valueOf(" + match.java("val", ctx, env) + "))"; 
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%val:EXPR% is not [a] valid attribute")
                .description("Checks if a string value is not a valid Bukkit Attribute name.")
                .example("if attr is not a valid attribute:")
                .since("1.0.0")
                .category(Categories.createOrGet("Validation"))
                .handler((match, env, ctx) -> {
                    ctx.addImport(EnumValidation.class.getName());
                    return "!EnumValidation.isValidAttribute(String.valueOf(" + match.java("val", ctx, env) + "))"; 
                }));
    }
}
