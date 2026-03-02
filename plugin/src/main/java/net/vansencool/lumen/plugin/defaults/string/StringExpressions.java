package net.vansencool.lumen.plugin.defaults.string;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.RefTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registers built-in expression patterns for string manipulation.
 */
@Registration
@Description("Registers string expression patterns: length, substring, replace, uppercase, lowercase, trim, split")
@SuppressWarnings("unused")
public final class StringExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("length of %s:STRING%")
                .description("Returns the length of a string.")
                .example("var len = length of \"hello\"")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> new ExpressionResult(
                        "String.valueOf(" + ctx.java("s") + ").length()", null)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:STRING% to lowercase")
                .pattern("%s:STRING% to lower")
                .description("Converts a string to lowercase.")
                .example("var lower = myVar to lowercase")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> new ExpressionResult(
                        "String.valueOf(" + ctx.java("s") + ").toLowerCase()", null)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:STRING% to uppercase")
                .pattern("%s:STRING% to upper")
                .description("Converts a string to uppercase.")
                .example("var upper = myVar to uppercase")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> new ExpressionResult(
                        "String.valueOf(" + ctx.java("s") + ").toUpperCase()", null)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:STRING% trimmed")
                .description("Returns the string with leading and trailing whitespace removed.")
                .example("var clean = myVar trimmed")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> new ExpressionResult(
                        "String.valueOf(" + ctx.java("s") + ").trim()", null)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("substring of %s:STRING% from %start:INT% to %end:INT%")
                .description("Returns a substring from the start index (inclusive) to the end index (exclusive).")
                .example("var sub = substring of \"hello\" from 1 to 3")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> new ExpressionResult(
                        "String.valueOf(" + ctx.java("s") + ").substring("
                                + ctx.java("start") + ", " + ctx.java("end") + ")", null)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("substring of %s:STRING% from %start:INT%")
                .description("Returns a substring from the start index to the end of the string.")
                .example("var sub = substring of \"hello\" from 2")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> new ExpressionResult(
                        "String.valueOf(" + ctx.java("s") + ").substring("
                                + ctx.java("start") + ")", null)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:STRING% replaced %old:STRING% with %new:STRING%")
                .description("Returns a copy of the string with all occurrences of a substring replaced.")
                .example("var fixed = myVar replaced \"bad\" with \"good\"")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> new ExpressionResult(
                        "String.valueOf(" + ctx.java("s") + ").replace(String.valueOf("
                                + ctx.java("old") + "), String.valueOf(" + ctx.java("new") + "))", null)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:STRING% split by %delim:STRING%")
                .description("Splits a string by a delimiter and returns a list of parts.")
                .example("var parts = myVar split by \",\"")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> {
                    ctx.codegen().addImport(Arrays.class.getName());
                    return new ExpressionResult(
                            "Arrays.asList(String.valueOf(" + ctx.java("s")
                                    + ").split(String.valueOf(" + ctx.java("delim") + ")))",
                            RefTypes.LIST.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("index of %needle:STRING% in %haystack:STRING%")
                .description("Returns the index of the first occurrence of a substring, or -1 if not found.")
                .example("var idx = index of \"l\" in \"hello\"")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> new ExpressionResult(
                        "String.valueOf(" + ctx.java("haystack") + ").indexOf(String.valueOf("
                                + ctx.java("needle") + "))", null)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:LIST% joined with %delim:STRING%")
                .pattern("%s:STRING% joined with %delim:STRING%")
                .description("Joins a list of values into a single string with a delimiter.")
                .example("var result = myList joined with \", \"")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    ctx.codegen().addImport(Collectors.class.getName());
                    String listJava = ctx.java("s");
                    String delimJava = ctx.java("delim");
                    return new ExpressionResult(
                            "((List<?>) " + listJava + ").stream()"
                                    + ".map(String::valueOf)"
                                    + ".collect(Collectors.joining(" + delimJava + "))",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .patterns("%s:STRING% as number", "%s:EXPR% as number")
                .description("Parses a string value into a double. Returns 0 if the value cannot be parsed.")
                .examples("var n = get args at index 0 as number", "var n = \"42.5\" as number")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> {
                    String val = ctx.java("s");
                    return new ExpressionResult(
                            "Coerce.toDouble(" + val + ")",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .patterns("%s:STRING% as integer", "%s:EXPR% as integer")
                .description("Parses a string value into an integer. Returns 0 if the value cannot be parsed.")
                .examples("var n = get args at index 0 as integer", "var n = \"42\" as integer")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> {
                    String val = ctx.java("s");
                    return new ExpressionResult(
                            "Coerce.toInt(" + val + ")",
                            null);
                }));
    }
}
