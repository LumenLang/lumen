package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.handler.BlockHandler;
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
@SuppressWarnings({"unused", "DataFlowIssue"})
public class ListBlocks {

    /**
     * Resolves the element type from the list variable's {@link CollectionType}.
     *
     * @param ctx the binding access for the current pattern match
     * @return the element type from the list's type arguments
     */
    private static @NotNull LumenType resolveElementType(@NotNull HandlerContext ctx) {
        EnvironmentAccess.VarHandle listRef = (EnvironmentAccess.VarHandle) ctx.value("list");
        CollectionType listType = TypeUtils.asCollection(listRef.type());
        return listType.typeArguments().get(0);
    }

    /**
     * Builds the metadata map for the loop variable based on the list's element type.
     *
     * @param ctx the binding access for the current pattern match
     * @return metadata map containing the {@code data_type} key, or {@code null} if not applicable
     */
    private static @Nullable Map<String, Object> resolveElementMetadata(@NotNull HandlerContext ctx) {
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
                .addVar("var", PrimitiveType.STRING)
                    .varDescription("The current element in the list, named by the user (e.g. 'item' in 'loop item in myList'). The type depends on the list being looped over and is accurate at runtime.")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        if (ctx.block().isRoot()) {
                            throw new DiagnosticException(LumenDiagnostic.error("E502", "A 'loop' block cannot be top level")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("top level loop not allowed")
                                    .help("place 'loop' inside an event, command, or other block")
                                    .build());
                        }
                        ctx.codegen().addImport(List.class.getName());
                        String varName = ctx.java("var");
                        if (ctx.env().lookupVar(varName) != null) {
                            throw new DiagnosticException(LumenDiagnostic.error("E502", "Loop variable '" + varName + "' is already defined")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("'" + varName + "' already exists in this scope")
                                    .help("use a different variable name")
                                    .build());
                        }
                        String listJava = ctx.java("list");
                        LumenType elementType = resolveElementType(ctx);
                        ctx.out().line("for (" + elementType.javaTypeName() + " " + varName + " : (List<" + elementType.javaTypeName() + ">) " + listJava + ") {");

                        Map<String, Object> metadata = resolveElementMetadata(ctx);
                        if (metadata != null) {
                            ctx.env().defineVar(varName, elementType, varName, metadata);
                        } else {
                            ctx.env().defineVar(varName, elementType, varName);
                        }
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        ctx.out().line("}");
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("loop %var:EXPR% in %list:LIST% for %scope:EXPR%")
                .description("Iterates over each element of a scoped global list for a specific scope reference.")
                .example("loop item in todos for player:")
                .since("1.0.0")
                .category(Categories.LIST)
                .addVar("var", PrimitiveType.STRING)
                    .varDescription("The current element in the list")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        if (ctx.block().isRoot()) {
                            throw new DiagnosticException(LumenDiagnostic.error("E502", "A 'loop' block cannot be top level")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("top level loop not allowed")
                                    .help("place 'loop' inside an event, command, or other block")
                                    .build());
                        }
                        EnvironmentAccess env = ctx.env();
                        String varName = ctx.java("var");
                        if (env.lookupVar(varName) != null) {
                            throw new DiagnosticException(LumenDiagnostic.error("E502", "Loop variable '" + varName + "' is already defined")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("'" + varName + "' already exists in this scope")
                                    .help("use a different variable name")
                                    .build());
                        }

                        String listVarName = ctx.tokens("list").get(0);
                        String scopeVarName = ctx.java("scope");
                        EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(listVarName);
                        if (info == null) {
                            throw new DiagnosticException(LumenDiagnostic.error("E500", "'" + listVarName + "' is not a global variable")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("not a global")
                                    .help("declare with 'global " + listVarName + " with default new list'")
                                    .build());
                        }
                        if (!info.scoped()) {
                            throw new DiagnosticException(LumenDiagnostic.error("E502", "'" + listVarName + "' is not a scoped global")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("not scoped")
                                    .help("declare with 'global scoped " + listVarName + "' to use per-entity access")
                                    .build());
                        }
                        EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
                        if (scopeRef == null) {
                            throw new DiagnosticException(LumenDiagnostic.error("E500", "Scope variable '" + scopeVarName + "' not found")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("undefined variable")
                                    .help("make sure the variable is defined before using it")
                                    .build());
                        }
                        LumenType scopeType = scopeRef.type();
                        EnvironmentAccess.VarHandle listRef = env.lookupVar(listVarName);
                        CollectionType listType = TypeUtils.asCollection(listRef.type());
                        LumenType elementType = listType.typeArguments().get(0);
                        ctx.codegen().addImport(List.class.getName());
                        ctx.codegen().addImport(ArrayList.class.getName());
                        ctx.out().line("for (var " + varName + " : (List<" + elementType.javaTypeName() + ">) " + (info.stored() ? "PersistentVars" : "GlobalVars") + ".get(" + "\"" + info.className() + "." + listVarName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")) {");
                        env.defineVar(varName, elementType, varName);
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        ctx.out().line("}");
                    }
                }));
    }
}
