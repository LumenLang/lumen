package dev.lumenlang.lumen.plugin.defaults.type;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.exceptions.ParseFailureException;
import dev.lumenlang.lumen.api.type.AddonTypeBinding;
import dev.lumenlang.lumen.api.type.TypeBindingMeta;
import dev.lumenlang.lumen.plugin.defaults.util.AttributeNames;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registers the {@code ATTRIBUTE} type binding.
 */
@Registration(order = -900)
@SuppressWarnings("unused")
public final class AttributeBinding {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "ATTRIBUTE";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "Resolves an attribute name (e.g. max_health, attack_damage) into the correct "
                                + "Bukkit Attribute enum constant for the current server version. "
                                + "Handles the renames across versions automatically.",
                        Attribute.class.getName(),
                        List.of("set entity max_health to 40", "get entity attack_damage"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
                String candidate = tokens.get(0);
                String resolved = AttributeNames.resolve(candidate);
                if (resolved != null)
                    return 1;

                if (tokens.size() >= 2) {
                    String twoWord = tokens.get(0) + "_" + tokens.get(1);
                    String resolved2 = AttributeNames.resolve(twoWord);
                    if (resolved2 != null)
                        return 2;
                }
                if (tokens.size() >= 3) {
                    String threeWord = tokens.get(0) + "_" + tokens.get(1) + "_" + tokens.get(2);
                    String resolved3 = AttributeNames.resolve(threeWord);
                    if (resolved3 != null)
                        return 3;
                }

                throw new ParseFailureException("Unknown attribute: " + candidate
                        + ". Known attributes: " + String.join(", ", AttributeNames.knownNames()));
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("Expected attribute name");

                if (tokens.size() >= 3) {
                    String threeWord = tokens.get(0) + "_" + tokens.get(1) + "_" + tokens.get(2);
                    String resolved3 = AttributeNames.resolve(threeWord);
                    if (resolved3 != null)
                        return resolved3;
                }
                if (tokens.size() >= 2) {
                    String twoWord = tokens.get(0) + "_" + tokens.get(1);
                    String resolved2 = AttributeNames.resolve(twoWord);
                    if (resolved2 != null)
                        return resolved2;
                }

                String resolved = AttributeNames.resolve(tokens.get(0));
                if (resolved != null)
                    return resolved;

                throw new ParseFailureException("Unknown attribute: " + tokens.get(0));
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenAccess ctx,
                                          @NotNull EnvironmentAccess env) {
                ctx.addImport(Attribute.class.getName());
                return "Attribute." + value;
            }
        });
    }
}
