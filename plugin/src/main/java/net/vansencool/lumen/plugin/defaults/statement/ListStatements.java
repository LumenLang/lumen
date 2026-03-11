package net.vansencool.lumen.plugin.defaults.statement;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.BindingAccess;
import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import net.vansencool.lumen.api.codegen.JavaOutput;
import net.vansencool.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    @Call
    public void register(@NotNull LumenAPI api) {
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
                    String valJava = ctx.java("val");

                    EnvironmentAccess.VarHandle listRef = listVarHandle(ctx);
                    if (listRef != null && listRef.hasMeta("element_type")) {
                        validateElementType((String) listRef.meta("element_type"), ctx, env);
                    }

                    ctx.codegen().addImport(List.class.getName());
                    out.line("((List<Object>) " + listJava + ").add(" + valJava + ");");
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
                    EnvironmentAccess env = ctx.env();
                    String listJava = ctx.java("list");
                    String valJava = ctx.java("val");
                    ctx.codegen().addImport(List.class.getName());
                    out.line("((List<?>) " + listJava + ").remove(" + valJava + ");");
                    flushIfStored(env, out, listJava, listVarName(ctx));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove [index] %i:INT% from %list:LIST%")
                .description("Removes the element at a specific index from a list.")
                .example("remove index 0 from myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String listJava = ctx.java("list");
                    String iJava = ctx.java("i");
                    ctx.codegen().addImport(List.class.getName());
                    out.line("((List<?>) " + listJava + ").remove(" + iJava + ");");
                    flushIfStored(env, out, listJava, listVarName(ctx));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear %list:LIST%")
                .description("Removes all elements from a list.")
                .example("clear myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String listJava = ctx.java("list");
                    ctx.codegen().addImport(List.class.getName());
                    out.line("((List<?>) " + listJava + ").clear();");
                    flushIfStored(env, out, listJava, listVarName(ctx));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %list:LIST% at [index] %i:INT% to %val:EXPR%")
                .description("Sets the element at a specific index in a list.")
                .example("set myList at index 0 to \"world\"")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String listJava = ctx.java("list");
                    String iJava = ctx.java("i");
                    String valJava = ctx.java("val");
                    ctx.codegen().addImport(List.class.getName());
                    out.line("((List<Object>) " + listJava + ").set(" + iJava + ", " + valJava + ");");
                    flushIfStored(env, out, listJava, listVarName(ctx));
                }));
    }
}
