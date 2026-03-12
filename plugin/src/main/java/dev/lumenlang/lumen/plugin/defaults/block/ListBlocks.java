package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.RefTypeHandle;
import dev.lumenlang.lumen.api.type.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static dev.lumenlang.lumen.api.pattern.LumaExample.of;
import static dev.lumenlang.lumen.api.pattern.LumaExample.secondly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.top;

/**
 * Registers the list loop block pattern.
 *
 * <p>When iterating over a typed list (one declared with {@code new list of <type>}),
 * the loop variable inherits the element type so that downstream expressions
 * (such as field access) can resolve it without requiring explicit type annotations.
 */
@Registration
@SuppressWarnings("unused")
public class ListBlocks {

    /**
     * Resolves the element ref type from the list variable's {@code element_type} metadata.
     *
     * <p>If the list was declared with {@code new list of <type>} and that type corresponds
     * to a registered data schema, the returned handle will be the DATA ref type. Otherwise,
     * returns {@code null} so the loop variable remains untyped.
     *
     * @param ctx the binding access for the current pattern match
     * @return the element ref type, or {@code null} if unknown
     */
    private static @Nullable RefTypeHandle resolveElementType(@NotNull BindingAccess ctx) {
        Object listValue = ctx.value("list");
        if (!(listValue instanceof EnvironmentAccess.VarHandle listRef)) return null;
        if (!listRef.hasMeta("element_type")) return null;

        String elementType = String.valueOf(listRef.meta("element_type"));
        Object schema = ctx.env().get("data_schema_" + elementType);
        if (schema != null) {
            return Types.DATA;
        }
        return null;
    }

    /**
     * Builds the metadata map for the loop variable based on the list's element type.
     *
     * @param ctx the binding access for the current pattern match
     * @return metadata map containing the {@code data_type} key, or {@code null} if not applicable
     */
    private static @Nullable Map<String, Object> resolveElementMetadata(@NotNull BindingAccess ctx) {
        Object listValue = ctx.value("list");
        if (!(listValue instanceof EnvironmentAccess.VarHandle listRef)) return null;
        if (!listRef.hasMeta("element_type")) return null;

        String elementType = String.valueOf(listRef.meta("element_type"));
        Object schema = ctx.env().get("data_schema_" + elementType);
        if (schema != null) {
            return Map.of("data_type", elementType);
        }
        return null;
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("loop %var:EXPR% in %list:LIST%")
                .description("Iterates over each element in a list.")
                .example(of(
                        top("loop item in myList:"),
                        secondly("broadcast item")))
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        if (ctx.block().isRoot()) {
                            throw new RuntimeException("A 'loop' block cannot be top-level");
                        }
                        ctx.codegen().addImport(List.class.getName());
                        String varName = ctx.java("var");
                        if (ctx.env().lookupVar(varName) != null) {
                            throw new RuntimeException(
                                    "Loop variable '" + varName + "' is already defined in this scope.");
                        }
                        String listJava = ctx.java("list");
                        out.line("for (var " + varName + " : (List<?>) " + listJava + ") {");

                        RefTypeHandle elementType = resolveElementType(ctx);
                        Map<String, Object> metadata = resolveElementMetadata(ctx);
                        if (metadata != null) {
                            ctx.env().defineVar(varName, elementType, varName, metadata);
                        } else {
                            ctx.env().defineVar(varName, elementType, varName);
                        }
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }
}
