package net.vansencool.lumen.plugin.defaults.condition;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.pipeline.java.compiled.Truthiness;
import org.jetbrains.annotations.NotNull;

/**
 * Registers built-in condition patterns for string comparison and inspection.
 */
@Registration(order = 200)
@SuppressWarnings("unused")
public final class StringConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% (is|equals) %b:QSTRING%")
                .description("Checks if two strings are equal (case-insensitive).")
                .example("if myVar is \"hello\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) ->
                        "String.valueOf(" + match.java("a", ctx, env) + ").equalsIgnoreCase(String.valueOf("
                                + match.java("b", ctx, env) + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% (is not|does not equal) %b:QSTRING%")
                .description("Checks if two strings are not equal (case-insensitive).")
                .example("if myVar is not \"hello\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) ->
                        "!String.valueOf(" + match.java("a", ctx, env) + ").equalsIgnoreCase(String.valueOf("
                                + match.java("b", ctx, env) + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% (is|equals) exactly %b:QSTRING%")
                .description("Checks if two strings are exactly equal (case-sensitive).")
                .example("if myVar is exactly \"Hello\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) ->
                        "String.valueOf(" + match.java("a", ctx, env) + ").equals(String.valueOf("
                                + match.java("b", ctx, env) + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% contains %b:QSTRING%")
                .description("Checks if a string contains another string.")
                .example("if myVar contains \"ell\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) ->
                        "String.valueOf(" + match.java("a", ctx, env) + ").contains(String.valueOf("
                                + match.java("b", ctx, env) + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% does not contain %b:QSTRING%")
                .description("Checks if a string does not contain another string.")
                .example("if myVar does not contain \"bad\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) ->
                        "!String.valueOf(" + match.java("a", ctx, env) + ").contains(String.valueOf("
                                + match.java("b", ctx, env) + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% starts with %b:QSTRING%")
                .description("Checks if a string starts with a given prefix.")
                .example("if myVar starts with \"he\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) ->
                        "String.valueOf(" + match.java("a", ctx, env) + ").startsWith(String.valueOf("
                                + match.java("b", ctx, env) + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% ends with %b:QSTRING%")
                .description("Checks if a string ends with a given suffix.")
                .example("if myVar ends with \"lo\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) ->
                        "String.valueOf(" + match.java("a", ctx, env) + ").endsWith(String.valueOf("
                                + match.java("b", ctx, env) + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%s:STRING% is empty")
                .description("Checks if a string is empty.")
                .example("if myVar is empty:")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) ->
                        "String.valueOf(" + match.java("s", ctx, env) + ").isEmpty()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%s:STRING% is not empty")
                .description("Checks if a string is not empty.")
                .example("if myVar is not empty:")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) ->
                        "!String.valueOf(" + match.java("s", ctx, env) + ").isEmpty()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("length of %s:STRING% %op:OP% %n:INT%")
                .description("Checks if the length of a string satisfies a comparison.")
                .example("if length of myVar >= 5:")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) ->
                        "String.valueOf(" + match.java("s", ctx, env) + ").length() "
                                + match.java("op", ctx, env) + " " + match.java("n", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% matches %b:QSTRING%")
                .description("Checks if a string matches a regular expression pattern.")
                .example("if myVar matches \"[0-9]+\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) ->
                        "String.valueOf(" + match.java("a", ctx, env) + ").matches(String.valueOf("
                                + match.java("b", ctx, env) + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% (is|equals) true")
                .description("Checks if a string or config value is truthy (\"true\", \"on\"), \"yes\" or \"1\").")
                .example("if \"{myFlag}\" is true:")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) -> {
                    ctx.addImport(Truthiness.class.getName());
                    return "Truthiness.check("
                            + match.java("a", ctx, env) + ")";
                    }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% (is|equals) false")
                .description("Checks if a string or config value is falsy.")
                .example("if \"{myFlag}\" is false:")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler((match, env, ctx) -> {
                    ctx.addImport(Truthiness.class.getName());
                    return "!Truthiness.check("
                            + match.java("a", ctx, env) + ")";
                }));
    }
}
