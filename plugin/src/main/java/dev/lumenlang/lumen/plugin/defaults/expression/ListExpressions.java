package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.TypeEnv;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.api.type.CollectionType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.simulator.suggestions.SuggestionDiagnostics;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.type.TypeAnnotationParser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registers built-in expression patterns for list operations.
 */
@Registration
@SuppressWarnings("unused")
public final class ListExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        // TODO: Remove in 1.4.0
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new list")
                .description("Creates a new empty list. WILL ALWAYS THROW AN ERROR, use 'new list of <type>'")
                .example("set myList to new list")
                .deprecated(true)
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    throw new DiagnosticException(LumenDiagnostic.error("Untyped lists are no longer supported")
                            .at(ctx.source().currentLine(), ctx.source().currentRaw())
                            .label("use 'new list of <type>' instead")
                            .help("example: 'set myList to new list of string'")
                            .build());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new list [of] %type:EXPR%")
                .description("Creates a new empty typed list. Elements added to this list will be validated against the declared type.")
                .examples("set arenas to new list of arena", "set scores to new list of number", "set names to new list string")
                .since("1.0.0")
                .deprecated(true)
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(ArrayList.class.getName());
                    HandlerContextImpl hctx = (HandlerContextImpl) ctx;
                    TypeEnvImpl env = (TypeEnvImpl) ctx.env();
                    List<Token> typeTokens = hctx.bound("type").tokens();
                    TypeAnnotationParser.ParseResult result = TypeAnnotationParser.parseDetailed(typeTokens, 0, env::lookupDataSchema);
                    if (result instanceof TypeAnnotationParser.ParseResult.Failure f) {
                        throw new DiagnosticException(SuggestionDiagnostics.buildTypeFailure("Invalid list element type", ctx.source().currentLine(), ctx.source().currentRaw(), typeTokens, f));
                    }
                    TypeAnnotationParser parsed = ((TypeAnnotationParser.ParseResult.Success) result).parser();
                    LumenType elementType = parsed.type();
                    if (parsed.dataSchemaName() != null) return new ExpressionResult("new ArrayList<>()", BuiltinLumenTypes.listOf(elementType), Map.of("element_type", parsed.dataSchemaName()));
                    return new ExpressionResult("new ArrayList<>()", BuiltinLumenTypes.listOf(elementType));
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("size of %list:LIST%")
                .description("Returns the number of elements in a list.")
                .example("set count to size of myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    return new ExpressionResult(
                            "((List<?>) " + ctx.java("list") + ").size()",
                            PrimitiveType.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%list:LIST% size")
                .description("Returns the number of elements in a list (postfix syntax).")
                .example("set count to myList size")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    return new ExpressionResult(
                            "((List<?>) " + ctx.java("list") + ").size()",
                            PrimitiveType.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %list:LIST% at [index] %i:INT%")
                .description("Returns the element at a specific index in a list.")
                .example("set item to get myList at index 0")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    Object listVal = ctx.value("list");
                    LumenType elementType = PrimitiveType.STRING;
                    if (listVal instanceof TypeEnv.VarHandle ref && ref.type() instanceof CollectionType ct && !ct.typeArguments().isEmpty()) {
                        elementType = ct.typeArguments().get(0);
                    }
                    String castType = (elementType instanceof PrimitiveType pt) ? pt.boxedName() : elementType.javaTypeName();
                    return new ExpressionResult(
                            "(" + castType + ") ((List<?>) " + ctx.java("list") + ").get(" + ctx.java("i") + ")",
                            elementType);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%list:LIST% index of %val:EXPR%")
                .description("Returns the index of the first occurrence of a value in a list.")
                .example("set idx to myList index of \"hello\"")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    return new ExpressionResult(
                            "((List<?>) " + ctx.java("list") + ").indexOf(" + ctx.java("val") + ")",
                            PrimitiveType.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("size of %list:LIST% for %scope:VAR%")
                .description("Returns the number of elements in a scoped global list for a specific scope reference.")
                .example("set count to size of todos for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    TypeEnv env = ctx.env();
                    String listVarName = ctx.tokens("list").get(0);
                    TypeEnv.GlobalInfo info = env.getGlobalInfo(listVarName);
                    if (info == null) {
                        throw new DiagnosticException(LumenDiagnostic.error("'" + listVarName + "' is not a global variable")
                                .at(ctx.source().currentLine(), ctx.source().currentRaw())
                                .label("scoped list operations require a global variable")
                                .help("declare it inside a 'global:' block as '" + listVarName + ": list of <type>'")
                                .build());
                    }
                    if (!info.scoped()) {
                        throw new DiagnosticException(LumenDiagnostic.error("'" + listVarName + "' is not a scoped global")
                                .at(ctx.source().currentLine(), ctx.source().currentRaw())
                                .label("the 'for' keyword requires a scoped global variable")
                                .help("declare it inside a 'global:' block with 'scoped to <type> " + listVarName + ": list of <type>' for per-entity access")
                                .build());
                    }
                    String scopeVarName = ctx.java("scope");
                    TypeEnv.VarHandle scopeRef = env.lookupVar(scopeVarName);
                    if (scopeRef == null) {
                        throw new DiagnosticException(LumenDiagnostic.error("Scope variable '" + scopeVarName + "' not found")
                                .at(ctx.source().currentLine(), ctx.source().currentRaw())
                                .label("'" + scopeVarName + "' is not defined in this scope")
                                .help("the scope variable must be a player or entity reference")
                                .build());
                    }
                    LumenType scopeType = scopeRef.type();
                    ctx.codegen().addImport(List.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    return new ExpressionResult("((List<?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") +
                            ".get(" + "\"" + info.className() + "." + listVarName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).size()", PrimitiveType.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%list:LIST% size for %scope:VAR%")
                .description("Returns the number of elements in a scoped global list (postfix syntax).")
                .example("set count to todos size for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    TypeEnv env = ctx.env();
                    String listVarName = ctx.tokens("list").get(0);
                    TypeEnv.GlobalInfo info = env.getGlobalInfo(listVarName);
                    if (info == null) {
                        throw new DiagnosticException(LumenDiagnostic.error("'" + listVarName + "' is not a global variable")
                                .at(ctx.source().currentLine(), ctx.source().currentRaw())
                                .label("scoped list operations require a global variable")
                                .help("declare it inside a 'global:' block as '" + listVarName + ": list of <type>'")
                                .build());
                    }
                    if (!info.scoped()) {
                        throw new DiagnosticException(LumenDiagnostic.error("'" + listVarName + "' is not a scoped global")
                                .at(ctx.source().currentLine(), ctx.source().currentRaw())
                                .label("the 'for' keyword requires a scoped global variable")
                                .help("declare it inside a 'global:' block with 'scoped to <type> " + listVarName + ": list of <type>' for per-entity access")
                                .build());
                    }
                    String scopeVarName = ctx.java("scope");
                    TypeEnv.VarHandle scopeRef = env.lookupVar(scopeVarName);
                    if (scopeRef == null) {
                        throw new DiagnosticException(LumenDiagnostic.error("Scope variable '" + scopeVarName + "' not found")
                                .at(ctx.source().currentLine(), ctx.source().currentRaw())
                                .label("'" + scopeVarName + "' is not defined in this scope")
                                .help("the scope variable must be a player or entity reference")
                                .build());
                    }
                    LumenType scopeType = scopeRef.type();
                    ctx.codegen().addImport(List.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    return new ExpressionResult("((List<?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") +
                            ".get(" + "\"" + info.className() + "." + listVarName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).size()", PrimitiveType.INT);
                }));
    }
}
