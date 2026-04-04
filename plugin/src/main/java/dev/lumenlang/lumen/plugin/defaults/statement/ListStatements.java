package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.RefTypeHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Registers built-in statement patterns for list manipulation.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class ListStatements {

    private static @Nullable String listVarName(@NotNull BindingAccess ctx) {
        Object val = ctx.value("list");
        if (val instanceof EnvironmentAccess.VarHandle ref) {
            return ref.java();
        }
        return null;
    }

    private static @Nullable EnvironmentAccess.VarHandle listVarHandle(@NotNull BindingAccess ctx) {
        Object val = ctx.value("list");
        if (val instanceof EnvironmentAccess.VarHandle ref) {
            return ref;
        }
        return null;
    }

    private static void validateElementType(@NotNull String elementType,
                                            @NotNull BindingAccess ctx,
                                            @NotNull EnvironmentAccess env) {
        List<String> valTokens = ctx.tokens("val");
        if (valTokens.size() == 1) {
            EnvironmentAccess.VarHandle valRef = env.lookupVar(valTokens.get(0));
            if (valRef != null && valRef.hasMeta("data_type")) {
                String valDataType = (String) valRef.meta("data_type");
                if (valDataType == null) {
                    throw new RuntimeException(
                            "Cannot determine data type of value for list element type validation, expected '" + elementType + "'");
                }
                if (!valDataType.equalsIgnoreCase(elementType)) {
                    throw new RuntimeException(
                            "Type mismatch: list expects '" + elementType
                                    + "' elements, but got '" + valDataType + "'");
                }
            }
        }
    }

    private static void flushIfStored(
            @NotNull EnvironmentAccess env,
            @NotNull JavaOutput out,
            @NotNull String listJava,
            @Nullable String varName) {
        if (varName != null && env.isStored(varName)) {
            out.line(env.storedClassName(varName) + ".set(" + env.getStoredKey(varName) + ", "
                    + listJava + ");");
        }
    }

    private static @NotNull String buildScopedKey(@NotNull EnvironmentAccess env,
                                                  @NotNull String varName,
                                                  @NotNull String scopeVarName,
                                                  @NotNull EnvironmentAccess.GlobalInfo info) {
        EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
        if (scopeRef == null) throw new RuntimeException("Scope variable not found: " + scopeVarName);
        RefTypeHandle refType = scopeRef.type();
        if (refType == null) throw new RuntimeException("Scope variable '" + scopeVarName + "' has no ref type.");
        return "\"" + info.className() + "." + varName + ".\" + " + refType.keyExpression(scopeRef.java());
    }

    private static void emitScopedMutation(@NotNull BindingAccess ctx, @NotNull JavaOutput out, @NotNull String listVarName, @NotNull String scopeVarName, @NotNull Function<String, String> mutation) {
        EnvironmentAccess.GlobalInfo info = ctx.env().getGlobalInfo(listVarName);
        if (info == null) throw new RuntimeException("'" + listVarName + "' is not a global variable.");
        String storageClass = info.stored() ? "PersistentVars" : "GlobalVars";
        String scopeKey = buildScopedKey(ctx.env(), listVarName, scopeVarName, info);
        String tmp = "__scoped_" + listVarName + "_" + out.lineNum();
        ctx.codegen().addImport(List.class.getName());
        ctx.codegen().addImport(ArrayList.class.getName());
        out.line("var " + tmp + " = " + storageClass + ".get(" + scopeKey + ", " + info.defaultJava() + ");");
        out.line(mutation.apply(tmp));
        out.line(storageClass + ".set(" + scopeKey + ", " + tmp + ");");
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add %val:EXPR% to %list:LIST% for %scope:EXPR%")
                .description("Adds a value to the end of a scoped global list for a specific scope reference.")
                .example("add task to todos for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> emitScopedMutation(ctx, out, ctx.tokens("list").get(0), ctx.java("scope"), tmp -> "((List<Object>) " + tmp + ").add(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove %val:EXPR% from %list:LIST% for %scope:EXPR%")
                .description("Removes the first occurrence of a value from a scoped global list for a specific scope reference.")
                .example("remove task from todos for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> emitScopedMutation(ctx, out, ctx.tokens("list").get(0), ctx.java("scope"), tmp -> "((List<?>) " + tmp + ").remove(" + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove [index] %i:INT% from %list:LIST% for %scope:EXPR%")
                .description("Removes the element at a specific index from a scoped global list for a specific scope reference.")
                .example("remove index 0 from todos for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> emitScopedMutation(ctx, out, ctx.tokens("list").get(0), ctx.java("scope"), tmp -> "((List<?>) " + tmp + ").remove(" + ctx.java("i") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear %list:LIST% for %scope:EXPR%")
                .description("Removes all elements from a scoped global list for a specific scope reference.")
                .example("clear todos for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> emitScopedMutation(ctx, out, ctx.tokens("list").get(0), ctx.java("scope"), tmp -> "((List<?>) " + tmp + ").clear();")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add %val:EXPR% to %list:LIST%")
                .description("Adds a value to the end of a list. If the list is typed, the value must be compatible.")
                .example("add \"hello\" to myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String listJava = ctx.java("list");

                    EnvironmentAccess.VarHandle listRef = listVarHandle(ctx);
                    if (listRef != null && listRef.hasMeta("element_type")) {
                        validateElementType((String) listRef.meta("element_type"), ctx, env);
                    }

                    ctx.codegen().addImport(List.class.getName());
                    out.line("((List<Object>) " + listJava + ").add(" + ctx.java("val") + ");");
                    flushIfStored(env, out, listJava, listVarName(ctx));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove %val:EXPR% from %list:LIST%")
                .description("Removes the first occurrence of a value from a list.")
                .example("remove \"hello\" from myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> {
                    String listJava = ctx.java("list");
                    ctx.codegen().addImport(List.class.getName());
                    out.line("((List<?>) " + listJava + ").remove(" + ctx.java("val") + ");");
                    flushIfStored(ctx.env(), out, listJava, listVarName(ctx));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove [index] %i:INT% from %list:LIST%")
                .description("Removes the element at a specific index from a list.")
                .example("remove index 0 from myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> {
                    String listJava = ctx.java("list");
                    ctx.codegen().addImport(List.class.getName());
                    out.line("((List<?>) " + listJava + ").remove(" + ctx.java("i") + ");");
                    flushIfStored(ctx.env(), out, listJava, listVarName(ctx));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear %list:LIST%")
                .description("Removes all elements from a list.")
                .example("clear myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> {
                    String listJava = ctx.java("list");
                    ctx.codegen().addImport(List.class.getName());
                    out.line("((List<?>) " + listJava + ").clear();");
                    flushIfStored(ctx.env(), out, listJava, listVarName(ctx));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %list:LIST% at [index] %i:INT% to %val:EXPR%")
                .description("Sets the element at a specific index in a list.")
                .example("set myList at index 0 to \"world\"")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> {
                    String listJava = ctx.java("list");
                    ctx.codegen().addImport(List.class.getName());
                    out.line("((List<Object>) " + listJava + ").set(" + ctx.java("i") + ", " + ctx.java("val") + ");");
                    flushIfStored(ctx.env(), out, listJava, listVarName(ctx));
                }));
    }
}
