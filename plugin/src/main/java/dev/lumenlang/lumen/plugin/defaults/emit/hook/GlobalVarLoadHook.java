package dev.lumenlang.lumen.plugin.defaults.emit.hook;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.emit.BlockEnterHook;
import dev.lumenlang.lumen.api.emit.EmitContext;
import dev.lumenlang.lumen.api.type.CollectionType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.NullableType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.persist.GlobalVars;
import dev.lumenlang.lumen.pipeline.persist.PersistentVars;
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
@SuppressWarnings("unused")
public final class GlobalVarLoadHook implements BlockEnterHook {
    public static final String TAG = "global-var-load";

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().blockEnterHook(this);
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

            LumenType lumenType = g.type();
            String fieldType = lumenType.javaTypeName();
            String storageClass = g.stored() ? "PersistentVars" : "GlobalVars";
            addTypeImports(lumenType, ctx);
            ctx.codegen().addField(fieldType + " " + name + ";");
            boolean needsCast = lumenType instanceof CollectionType || lumenType instanceof NullableType;
            String getExpr = storageClass + ".get(" + keyExpr + ", " + defaultJava + ")";
            if (needsCast) {
                ctx.out().taggedLine(TAG, name + " = (" + fieldType + ") " + getExpr + ";");
            } else {
                ctx.out().taggedLine(TAG, name + " = " + getExpr + ";");
            }

            VarRef varRef = new VarRef(lumenType, name, exprMetadata != null ? exprMetadata : Map.of());
            env.defineVar(name, varRef);
            env.markGlobalField(name);
            if (g.stored()) {
                env.markStored(name, keyExpr, "\"" + className + "." + name + ".\"", null);
            } else {
                env.markRuntimeGlobal(name);
            }
        }
    }

    private static void addTypeImports(@NotNull LumenType type, @NotNull EmitContext ctx) {
        if (type instanceof CollectionType ct) {
            String rawFqn = ct.rawType().javaType();
            if (rawFqn.contains(".") && !rawFqn.startsWith("java.lang.")) ctx.codegen().addImport(rawFqn);
            for (LumenType arg : ct.typeArguments()) addTypeImports(arg, ctx);
        } else if (type instanceof NullableType nt) {
            addTypeImports(nt.inner(), ctx);
        } else if (type instanceof ObjectType ot) {
            String fqn = ot.javaType();
            if (fqn.contains(".") && !fqn.startsWith("java.lang.")) ctx.codegen().addImport(fqn);
        }
    }
}
