package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpressionMatch;
import dev.lumenlang.lumen.pipeline.language.resolve.ExprResolver;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.typed.Expr;
import dev.lumenlang.lumen.pipeline.language.typed.ExprParser;
import dev.lumenlang.lumen.pipeline.language.validator.VarNameValidator;
import dev.lumenlang.lumen.pipeline.persist.PersistentVars;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Registers statement patterns for inline stored variable loading.
 *
 * <p>Accepts the syntax {@code load name [for scope] with default expr}.
 * This loads a persistent variable into the current scope, creating it with the given
 * default value if it does not yet exist. The variable is backed by {@link PersistentVars}
 * and survives server restarts.
 */
@Registration(order = -1999)
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class LoadStatementForm {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("load %name:IDENT% for %scope:IDENT% with default %val:EXPR%")
                .description("Loads a stored variable scoped to an entity with a default value.")
                .example("load coins for player with default 0")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> emitLoadVar(ctx.tokens("name").get(0), ctx.tokens("scope").get(0), ctx)));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("load %name:IDENT% with default %val:EXPR%")
                .description("Loads a stored variable with a default value.")
                .example("load counter with default 0")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> emitLoadVar(ctx.tokens("name").get(0), null, ctx)));
    }

    private static void emitLoadVar(@NotNull String name, @Nullable String scopeVar, @NotNull HandlerContext ctx) {
        HandlerContextImpl emitCtx = (HandlerContextImpl) ctx;
        TypeEnv env = (TypeEnv) ctx.env();
        List<Token> exprTokens = emitCtx.bound("val").tokens();

        String nameError = VarNameValidator.validate(name);
        if (nameError != null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), nameError);
        }

        if (env.lookupVar(name) != null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Variable '" + name + "' is already defined in this scope");
        }

        RegisteredExpressionMatch exprMatch = PatternRegistry.instance().matchExpression(exprTokens, env);

        String defaultJava;
        ObjectType resolvedObjectType = null;

        if (exprMatch != null) {
            HandlerContextImpl hctx = new HandlerContextImpl(exprMatch.match(), env, emitCtx.codegenContext(), env.blockContext(), null, 0, "");
            ExpressionResult result = exprMatch.reg().handler().handle(hctx);
            defaultJava = result.java();
            resolvedObjectType = result.type() instanceof ObjectType ot ? ot : null;
        } else {
            Expr e = ExprParser.parse(exprTokens, env);
            if (e instanceof Expr.Literal l) {
                defaultJava = l.value() instanceof String s ? PlaceholderExpander.expand(s, env) : l.value().toString();
            } else if (e instanceof Expr.RefExpr r) {
                VarRef ref = env.lookupVar(r.name());
                if (ref == null) {
                    throw new RuntimeException("Variable not found: " + r.name());
                }
                defaultJava = ref.java();
            } else if (e instanceof Expr.MathExpr m) {
                defaultJava = m.java();
            } else {
                Expr.RawExpr raw = (Expr.RawExpr) e;
                throw new LumenScriptException(ctx.line(), ctx.raw(), "Expression not recognized: '" + ExprResolver.joinTokens(raw.tokens()) + "'. Check spelling of variables and expression patterns.", raw.tokens());
            }
        }

        String className = ctx.codegen().className();
        String keyExpr;
        if (scopeVar != null) {
            VarRef scopeRef = env.lookupVar(scopeVar);
            if (scopeRef == null) {
                throw new RuntimeException("Scope variable not found: " + scopeVar);
            }
            String scopeKeyPart;
            if (scopeRef.objectType() != null) {
                scopeKeyPart = scopeRef.objectType().keyExpression(scopeRef.java());
            } else {
                scopeKeyPart = "String.valueOf(" + scopeRef.java() + ")";
            }
            keyExpr = "\"" + className + "." + name + ".\" + " + scopeKeyPart;
        } else {
            keyExpr = "\"" + className + "." + name + "\"";
        }

        ctx.codegen().addImport(PersistentVars.class.getName());
        ctx.out().line("var " + name + " = PersistentVars.get(" + keyExpr + ", " + defaultJava + ");");
        VarRef varRef = new VarRef(name, resolvedObjectType, name);
        env.defineVar(name, varRef);
        env.recordDeclaration(name, ctx.line(), ctx.raw());
        if (env.blockContext().parent() != null) {
            env.blockContext().parent().defineVar(name, varRef);
        }
        String baseKey = "\"" + className + "." + name + ".\"";
        ctx.env().markStored(name, keyExpr, baseKey, scopeVar);
    }
}
