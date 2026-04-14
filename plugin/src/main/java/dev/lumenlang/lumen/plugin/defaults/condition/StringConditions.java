package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.pipeline.java.compiled.Truthiness;
import org.jetbrains.annotations.NotNull;

/**
 * Registers built-in condition patterns for string comparison and inspection.
 */
@Registration(order = 200)
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class StringConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% (is|equals|is not|does not equal) %b:QSTRING%")
                .description("Checks if two strings are equal or not equal (case-insensitive).")
                .examples("if myVar is \"hello\":", "if myVar is not \"hello\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> {
                    String choice = ctx.choice(0);
                    boolean negated = choice.equals("is not") || choice.equals("does not equal");
                    return (negated ? "!" : "") + "String.valueOf(" + ctx.java("a") + ").equalsIgnoreCase(String.valueOf(" + ctx.java("b") + "))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% (is|equals) exactly %b:QSTRING%")
                .description("Checks if two strings are exactly equal (case-sensitive).")
                .example("if myVar is exactly \"Hello\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx ->
                        "String.valueOf(" + ctx.java("a") + ").equals(String.valueOf("
                                + ctx.java("b") + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% (contains|does not contain) %b:QSTRING%")
                .description("Checks if a string contains or does not contain another string.")
                .examples("if myVar contains \"ell\":", "if myVar does not contain \"bad\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("does not contain");
                    return (negated ? "!" : "") + "String.valueOf(" + ctx.java("a") + ").contains(String.valueOf(" + ctx.java("b") + "))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% starts with %b:QSTRING%")
                .description("Checks if a string starts with a given prefix.")
                .example("if myVar starts with \"he\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx ->
                        "String.valueOf(" + ctx.java("a") + ").startsWith(String.valueOf("
                                + ctx.java("b") + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% ends with %b:QSTRING%")
                .description("Checks if a string ends with a given suffix.")
                .example("if myVar ends with \"lo\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx ->
                        "String.valueOf(" + ctx.java("a") + ").endsWith(String.valueOf("
                                + ctx.java("b") + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%s:STRING% (is|is not) empty")
                .description("Checks if a string is or is not empty.")
                .examples("if myVar is empty:", "if myVar is not empty:")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + "String.valueOf(" + ctx.java("s") + ").isEmpty()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("length of %s:STRING% %op:OP% %n:INT%")
                .description("Checks if the length of a string satisfies a comparison.")
                .example("if length of myVar >= 5:")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx ->
                        "String.valueOf(" + ctx.java("s") + ").length() "
                                + ctx.java("op") + " " + ctx.java("n")));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% matches %b:QSTRING%")
                .description("Checks if a string matches a regular expression pattern.")
                .example("if myVar matches \"[0-9]+\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx ->
                        "String.valueOf(" + ctx.java("a") + ").matches(String.valueOf("
                                + ctx.java("b") + "))"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% (is|equals) true")
                .description("Checks if a string is truthy (\"true\", \"on\"), \"yes\" or \"1\").")
                .example("if \"{enabled}\" is true:")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> {
                    ctx.codegen().addImport(Truthiness.class.getName());
                    return "Truthiness.check("
                            + ctx.java("a") + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% (is|equals) false")
                .description("Checks if a string is falsy.")
                .example("if \"{enabled}\" is false:")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> {
                    ctx.codegen().addImport(Truthiness.class.getName());
                    return "!Truthiness.check("
                            + ctx.java("a") + ")";
                }));
    }
}
