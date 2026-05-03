package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.codegen.TypeEnv;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.CollectionType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.api.type.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Registers built-in statement patterns for list manipulation.
 */
@Registration
@SuppressWarnings("unused")
public final class ListStatements {

    private static @Nullable String listVarName(@NotNull HandlerContext ctx) {
        Object val = ctx.value("list");
        if (val instanceof TypeEnv.VarHandle ref) {
            return ref.java();
        }
        return null;
    }

    private static @NotNull LumenType listElementType(@NotNull HandlerContext ctx) {
        TypeEnv.VarHandle listRef = ctx.varHandle("list");
        if (listRef != null) {
            CollectionType ct = TypeUtils.asCollection(listRef.type());
            if (ct != null && !ct.typeArguments().isEmpty()) return ct.typeArguments().get(0);
        }
        LumenType actual = ctx.resolvedType("val");
        return actual != null ? actual : PrimitiveType.STRING;
    }

    private static void validateElementType(@NotNull HandlerContext ctx, @NotNull String paramName) {
        TypeEnv.VarHandle listRef = ctx.varHandle("list");
        if (listRef == null) return;
        CollectionType ct = TypeUtils.asCollection(listRef.type());
        if (ct == null || ct.typeArguments().isEmpty()) return;
        LumenType expected = ct.typeArguments().get(0);
        LumenType actual = ctx.resolvedType(paramName);
        if (actual == null || expected.assignableFrom(actual)) return;
        throw new DiagnosticException(LumenDiagnostic.error("List element type mismatch")
                .at(ctx.source().currentLine(), ctx.source().currentRaw())
                .label("expected '" + expected.displayName() + "', got '" + actual.displayName() + "'")
                .help("this list only accepts '" + expected.displayName() + "' elements")
                .build());
    }

    private static @NotNull String buildScopedKey(@NotNull HandlerContext ctx, @NotNull String varName, @NotNull String scopeVarName, @NotNull TypeEnv.GlobalInfo info) {
        TypeEnv env = ctx.env();
        TypeEnv.VarHandle scopeRef = env.lookupVar(scopeVarName);
        if (scopeRef == null) {
            throw new DiagnosticException(LumenDiagnostic.error("Scope variable '" + scopeVarName + "' not found")
                    .at(ctx.source().currentLine(), ctx.source().currentRaw())
                    .label("undefined variable")
                    .help("make sure the variable is defined before using it")
                    .build());
        }
        LumenType scopeType = scopeRef.type();
        return "\"" + info.className() + "." + varName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java());
    }

    private static void emitScopedMutation(@NotNull HandlerContext ctx, @NotNull String listVarName, @NotNull String scopeVarName, @NotNull Function<String, String> mutation) {
        TypeEnv.GlobalInfo info = ctx.env().getGlobalInfo(listVarName);
        if (info == null) {
            throw new DiagnosticException(LumenDiagnostic.error("'" + listVarName + "' is not a global variable")
                    .at(ctx.source().currentLine(), ctx.source().currentRaw())
                    .label("not a global")
                    .help("declare it inside a 'global:' block as '" + listVarName + ": list of <type>'")
                    .build());
        }
        String storageClass = info.stored() ? "PersistentVars" : "GlobalVars";
        String scopeKey = buildScopedKey(ctx, listVarName, scopeVarName, info);
        String tmp = "__scoped_" + listVarName + "_" + ctx.out().lineNum();
        ctx.codegen().addImport(List.class.getName());
        ctx.codegen().addImport(ArrayList.class.getName());
        ctx.out().line("var " + tmp + " = " + storageClass + ".get(" + scopeKey + ", " + info.defaultJava() + ");");
        ctx.out().line(mutation.apply(tmp));
        ctx.out().line(storageClass + ".set(" + scopeKey + ", " + tmp + ");");
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add %val:EXPR% to %list:LIST% for %scope:VAR%")
                .description("Adds a value to the end of a scoped global list for a specific scope reference.")
                .example("add task to todos for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    validateElementType(ctx, "val");
                    String val = ctx.java("val", listElementType(ctx));
                    emitScopedMutation(ctx, ctx.tokens("list").get(0), ctx.java("scope"), tmp -> "((List) " + tmp + ").add(" + val + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove %val:EXPR% from %list:LIST% for %scope:VAR%")
                .description("Removes the first occurrence of a value from a scoped global list for a specific scope reference.")
                .example("remove task from todos for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    validateElementType(ctx, "val");
                    emitScopedMutation(ctx, ctx.tokens("list").get(0), ctx.java("scope"), tmp -> "((List<?>) " + tmp + ").remove(" + ctx.java("val") + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove [index] %i:INT% from %list:LIST% for %scope:VAR%")
                .description("Removes the element at a specific index from a scoped global list for a specific scope reference.")
                .example("remove index 0 from todos for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> emitScopedMutation(ctx, ctx.tokens("list").get(0), ctx.java("scope"), tmp -> "((List<?>) " + tmp + ").remove(" + ctx.java("i") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear %list:LIST% for %scope:VAR%")
                .description("Removes all elements from a scoped global list for a specific scope reference.")
                .example("clear todos for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> emitScopedMutation(ctx, ctx.tokens("list").get(0), ctx.java("scope"), tmp -> "((List<?>) " + tmp + ").clear();")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add %val:EXPR% to %list:LIST%")
                .description("Adds a value to the end of a list. If the list is typed, the value must be compatible.")
                .example("add \"hello\" to myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    String listJava = ctx.java("list");
                    validateElementType(ctx, "val");
                    ctx.codegen().addImport(List.class.getName());
                    ctx.out().line("((List) " + listJava + ").add(" + ctx.java("val", listElementType(ctx)) + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove %val:EXPR% from %list:LIST%")
                .description("Removes the first occurrence of a value from a list.")
                .example("remove \"hello\" from myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    String listJava = ctx.java("list");
                    validateElementType(ctx, "val");
                    ctx.codegen().addImport(List.class.getName());
                    ctx.out().line("((List<?>) " + listJava + ").remove(" + ctx.java("val") + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove [index] %i:INT% from %list:LIST%")
                .description("Removes the element at a specific index from a list.")
                .example("remove index 0 from myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    String listJava = ctx.java("list");
                    ctx.codegen().addImport(List.class.getName());
                    ctx.out().line("((List<?>) " + listJava + ").remove(" + ctx.java("i") + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear %list:LIST%")
                .description("Removes all elements from a list.")
                .example("clear myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    String listJava = ctx.java("list");
                    ctx.codegen().addImport(List.class.getName());
                    ctx.out().line("((List<?>) " + listJava + ").clear();");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %list:LIST% at [index] %i:INT% to %val:EXPR%")
                .description("Sets the element at a specific index in a list.")
                .example("set myList at index 0 to \"world\"")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    String listJava = ctx.java("list");
                    validateElementType(ctx, "val");
                    ctx.codegen().addImport(List.class.getName());
                    ctx.out().line("((List) " + listJava + ").set(" + ctx.java("i") + ", " + ctx.java("val") + ");");
                }));
    }
}
