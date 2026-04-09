package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.Types;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registers built-in expression patterns for string manipulation.
 */
@Registration
@SuppressWarnings("unused")
public final class StringExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("length of %s:STRING%")
                .description("Returns the length of a string.")
                .example("set len to length of \"hello\"")
                .since("1.0.0")
                .category(Categories.TEXT)
                .returnType(Types.INT)
                .handler(ctx -> new ExpressionResult("String.valueOf(" + ctx.java("s") + ").length()", Types.INT)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:STRING% to lowercase")
                .pattern("%s:STRING% to lower")
                .description("Converts a string to lowercase.")
                .example("set lower to myVar to lowercase")
                .since("1.0.0")
                .category(Categories.TEXT)
                .returnType(Types.STRING)
                .handler(ctx -> new ExpressionResult("String.valueOf(" + ctx.java("s") + ").toLowerCase()", Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:STRING% to uppercase")
                .pattern("%s:STRING% to upper")
                .description("Converts a string to uppercase.")
                .example("set upper to myVar to uppercase")
                .since("1.0.0")
                .category(Categories.TEXT)
                .returnType(Types.STRING)
                .handler(ctx -> new ExpressionResult("String.valueOf(" + ctx.java("s") + ").toUpperCase()", Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:STRING% trimmed")
                .description("Returns the string with leading and trailing whitespace removed.")
                .example("set clean to myVar trimmed")
                .since("1.0.0")
                .category(Categories.TEXT)
                .returnType(Types.STRING)
                .handler(ctx -> new ExpressionResult("String.valueOf(" + ctx.java("s") + ").trim()", Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("substring of %s:STRING% from %start:INT% to %end:INT%")
                .description("Returns a substring from the start index (inclusive) to the end index (exclusive).")
                .example("set sub to substring of \"hello\" from 1 to 3")
                .since("1.0.0")
                .category(Categories.TEXT)
                .returnType(Types.STRING)
                .handler(ctx -> new ExpressionResult(
                        "String.valueOf(" + ctx.java("s") + ").substring(" + ctx.java("start") + ", " + ctx.java("end") + ")", Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("substring of %s:STRING% from %start:INT%")
                .description("Returns a substring from the start index to the end of the string.")
                .example("set sub to substring of \"hello\" from 2")
                .since("1.0.0")
                .category(Categories.TEXT)
                .returnType(Types.STRING)
                .handler(ctx -> new ExpressionResult(
                        "String.valueOf(" + ctx.java("s") + ").substring(" + ctx.java("start") + ")", Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:STRING% replaced %old:STRING% with %new:STRING%")
                .description("Returns a copy of the string with all occurrences of a substring replaced.")
                .example("set fixed to myVar replaced \"bad\" with \"good\"")
                .since("1.0.0")
                .category(Categories.TEXT)
                .returnType(Types.STRING)
                .handler(ctx -> new ExpressionResult(
                        "String.valueOf(" + ctx.java("s") + ").replace(String.valueOf(" + ctx.java("old") + "), String.valueOf(" + ctx.java("new") + "))", Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:STRING% split by %delim:STRING%")
                .description("Splits a string by a delimiter and returns a list of parts.")
                .example("set parts to myVar split by \",\"")
                .since("1.0.0")
                .category(Categories.TEXT)
                .returnType(BuiltinLumenTypes.LIST.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Arrays.class.getName());
                    return new ExpressionResult(
                            "Arrays.asList(String.valueOf(" + ctx.java("s") + ").split(String.valueOf(" + ctx.java("delim") + ")))",
                            BuiltinLumenTypes.LIST.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("index of %needle:STRING% in %haystack:STRING%")
                .description("Returns the index of the first occurrence of a substring, or -1 if not found.")
                .example("set idx to index of \"l\" in \"hello\"")
                .since("1.0.0")
                .category(Categories.TEXT)
                .returnType(Types.INT)
                .handler(ctx -> new ExpressionResult("String.valueOf(" + ctx.java("haystack") + ").indexOf(String.valueOf(" + ctx.java("needle") + "))", Types.INT)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%s:LIST% joined with %delim:STRING%")
                .pattern("%s:STRING% joined with %delim:STRING%")
                .description("Joins a list of values into a single string with a delimiter.")
                .example("set result to myList joined with \", \"")
                .since("1.0.0")
                .category(Categories.TEXT)
                .returnType(Types.STRING)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    ctx.codegen().addImport(Collectors.class.getName());
                    String listJava = ctx.java("s");
                    String delimJava = ctx.java("delim");
                    return new ExpressionResult(
                            "((List<?>) " + listJava + ").stream()"
                                    + ".map(String::valueOf)"
                                    + ".collect(Collectors.joining(" + delimJava + "))",
                            Types.STRING);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .patterns("%s:STRING% as number", "%s:EXPR% as number")
                .description("Parses a string value into a double. Returns 0 if the value cannot be parsed.")
                .examples("set n to get args at index 0 as number", "set n to \"42.5\" as number")
                .since("1.0.0")
                .category(Categories.MATH)
                .returnType(Types.DOUBLE)
                .handler(ctx -> {
                    String val = ctx.java("s");
                    return new ExpressionResult(
                            "Double.parseDouble(String.valueOf(" + val + "))",
                            Types.DOUBLE);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .patterns("%s:STRING% as integer", "%s:EXPR% as integer")
                .description("Parses a string value into an integer. Returns 0 if the value cannot be parsed.")
                .examples("set n to get args at index 0 as integer", "set n to \"42\" as integer")
                .since("1.0.0")
                .category(Categories.MATH)
                .returnType(Types.INT)
                .handler(ctx -> {
                    String val = ctx.java("s");
                    return new ExpressionResult(
                            "Integer.parseInt(String.valueOf(" + val + "))",
                            Types.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("combined string [of] %s1:STRING% and %s2:STRING%")
                .description("Combines two strings into a single string.")
                .example("set combined to combined string of \"hello\" and \"world\"")
                .since("1.0.0")
                .category(Categories.TEXT)
                .returnType(Types.STRING)
                .handler(ctx -> new ExpressionResult("String.valueOf(" + ctx.java("s1") + ") + String.valueOf(" + ctx.java("s2") + ")", Types.STRING)));
    }
}
