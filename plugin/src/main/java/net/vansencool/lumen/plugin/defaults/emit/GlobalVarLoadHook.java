package net.vansencool.lumen.plugin.defaults.emit;

import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import net.vansencool.lumen.api.emit.BlockEnterHook;
import net.vansencool.lumen.api.emit.EmitContext;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.persist.GlobalVars;
import net.vansencool.lumen.pipeline.persist.PersistentVars;
import net.vansencool.lumen.pipeline.var.RefType;
import net.vansencool.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Block enter hook that loads all registered global variables at the start of every block body.
 *
 * <p>For each global declared with {@code global [stored] var}, this hook emits a local
 * variable initialization that reads from either {@link PersistentVars} (stored) or
 * {@link GlobalVars} (in-memory). When the global has a ref type, the storage key is scoped
 * to the block's default variable for that type. If no default variable is available in
 * the current scope, the global is silently skipped.
 */
@SuppressWarnings("DataFlowIssue")
public final class GlobalVarLoadHook implements BlockEnterHook {

    /**
     * Infers the Java field type from the default value expression.
     *
     * <p>Recognizes integer, long, double, boolean, and string literals.
     * Falls back to {@code Object} for any expression that cannot be trivially classified.
     *
     * @param defaultJava the Java default value expression (e.g. {@code "5"}, {@code "\"hello\""})
     * @return a Java type name suitable for a field declaration
     */
    private static @NotNull String inferFieldType(@NotNull String defaultJava) {
        if (defaultJava.equals("true") || defaultJava.equals("false")) return "boolean";
        if (defaultJava.startsWith("\"")) return "String";
        if (defaultJava.endsWith("L") || defaultJava.endsWith("l")) {
            String num = defaultJava.substring(0, defaultJava.length() - 1);
            if (isDigits(num)) return "long";
        }
        if (defaultJava.contains(".")) {
            try {
                Double.parseDouble(defaultJava);
                return "double";
            } catch (NumberFormatException ignored) {
            }
        }
        if (isDigits(defaultJava)) return "int";
        return "Object";
    }

    private static boolean isDigits(@NotNull String s) {
        if (s.isEmpty()) return false;
        int start = s.charAt(0) == '-' ? 1 : 0;
        if (start >= s.length()) return false;
        for (int i = start; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    @Override
    public void onBlockEnter(@NotNull EmitContext ctx) {
        TypeEnv env = (TypeEnv) ctx.env();
        List<? extends EnvironmentAccess.GlobalInfo> globals = env.allGlobals();

        for (EnvironmentAccess.GlobalInfo g : globals) {
            String name = g.name();

            if (env.lookupVar(name) != null) {
                continue;
            }

            String defaultJava = g.defaultJava();
            String className = g.className();
            String refTypeName = g.refTypeName();
            boolean stored = g.stored();
            String exprRefTypeId = g.exprRefTypeId();
            Map<String, Object> exprMetadata = g.exprMetadata();

            String keyExpr;
            String scopeVarName = null;
            RefType exprRefType = exprRefTypeId != null ? RefType.byId(exprRefTypeId) : null;

            if (refTypeName != null) {
                RefType refType = RefType.byId(refTypeName);
                if (refType == null) {
                    continue;
                }
                VarRef scopeRef = env.lookupDefault(refType);
                if (scopeRef == null) {
                    continue;
                }
                scopeVarName = scopeRef.java();
                String scopeKeyPart = refType.keyExpression(scopeRef.java());
                keyExpr = "\"" + className + "." + name + ".\" + " + scopeKeyPart;
            } else {
                keyExpr = "\"" + className + "." + name + "\"";
            }

            String storageClass;
            if (stored) {
                ctx.codegen().addImport(PersistentVars.class.getName());
                storageClass = PersistentVars.class.getSimpleName();
            } else {
                ctx.codegen().addImport(GlobalVars.class.getName());
                storageClass = GlobalVars.class.getSimpleName();
            }

            String fieldType;
            if (exprRefType != null) {
                String fqn = exprRefType.javaType();
                ctx.codegen().addImport(fqn);
                fieldType = fqn.substring(fqn.lastIndexOf('.') + 1);
            } else {
                fieldType = inferFieldType(defaultJava);
            }
            ctx.codegen().addField(fieldType + " " + name + ";");
            ctx.out().line(name + " = " + storageClass + ".get(" + keyExpr + ", " + defaultJava + ");");

            VarRef varRef = new VarRef(exprRefType, name, exprMetadata);
            env.defineVar(name, varRef);
            env.markGlobalField(name);

            String baseKey = "\"" + className + "." + name + ".\"";
            if (stored) {
                env.markStored(name, keyExpr, baseKey, scopeVarName);
            } else {
                env.markRuntimeGlobal(name);
                env.markStored(name, keyExpr, baseKey, scopeVarName);
            }
        }
    }
}
