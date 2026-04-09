package dev.lumenlang.lumen.plugin.defaults.emit.hook;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.emit.BlockEnterHook;
import dev.lumenlang.lumen.api.emit.EmitContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.persist.GlobalVars;
import dev.lumenlang.lumen.pipeline.persist.PersistentVars;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Block enter hook that loads server-wide global variables at the start of every block body.
 *
 * <p>For each global declared without {@code scoped}, this hook emits a local variable
 * initialization that reads from {@link GlobalVars} (or {@link PersistentVars} for
 * stored globals). Scoped globals are not loaded here and must be accessed explicitly
 * via {@code get name for scope}.
 */
@Registration(order = -2000)
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class GlobalVarLoadHook implements BlockEnterHook {
    public static final String TAG = "global-var-load";

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().blockEnterHook(this);
    }

    /**
     * Infers the Java field type from the default value expression.
     *
     * <p>Recognizes integer, long, double, boolean, and string literals.
     * Falls back to {@code Object} for any expression that cannot be trivially classified.
     *
     * @param defaultJava the Java default value expression
     * @return a Java type name suitable for a field declaration
     */
    private static @NotNull LumenType inferLumenType(@NotNull String defaultJava) {
        if (defaultJava.equals("true") || defaultJava.equals("false")) return PrimitiveType.BOOLEAN;
        if (defaultJava.startsWith("\"")) return PrimitiveType.STRING;
        if (defaultJava.endsWith("L") || defaultJava.endsWith("l")) {
            String num = defaultJava.substring(0, defaultJava.length() - 1);
            if (isDigits(num)) return PrimitiveType.LONG;
        }
        if (defaultJava.contains(".")) {
            try {
                Double.parseDouble(defaultJava);
                return PrimitiveType.DOUBLE;
            } catch (NumberFormatException ignored) {
            }
        }
        if (isDigits(defaultJava)) return PrimitiveType.INT;
        return PrimitiveType.STRING;
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
            if (env.lookupVar(name) != null) continue;
            if (g.scoped()) continue;

            String defaultJava = g.defaultJava();
            String className = g.className();
            Map<String, Object> exprMetadata = g.exprMetadata();
            String keyExpr = "\"" + className + "." + name + "\"";

            // TODO: Completely rewrite global var type tracking
            LumenType lumenType = inferLumenType(defaultJava);
            String fieldType = lumenType.javaTypeName();
            String storageClass = g.stored() ? "PersistentVars" : "GlobalVars";
            ctx.codegen().addField(fieldType + " " + name + ";");
            ctx.out().taggedLine(TAG, name + " = " + storageClass + ".get(" + keyExpr + ", " + defaultJava + ");");

            VarRef varRef = new VarRef(lumenType, name, exprMetadata);
            env.defineVar(name, varRef);
            env.markGlobalField(name);
            if (g.stored()) {
                env.markStored(name, keyExpr, "\"" + className + "." + name + ".\"", null);
            } else {
                env.markRuntimeGlobal(name);
            }
        }
    }
}
