package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.inject.index.IndexedParam;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Emits a static method onto the script class and returns a call expression
 * to it. Used when a handler body has more than a single statement so the
 * runtime body cannot be inlined as a value.
 */
public final class MethodEmitter {

    private static final String METHOD_PREFIX = "__lumen_inj_";

    private MethodEmitter() {
    }

    /**
     * @param ctx        active handler context
     * @param entry      preserved sidecar source for the handler
     * @param params     {@code @Inject} param descriptors in declaration order
     * @param returnType Java return type of the handler (e.g. {@code "boolean"}, {@code "org.bukkit.Location"}, {@code "void"})
     * @return a call expression like {@code __lumen_inj_setFire_3(player)} that
     * invokes the emitted method with each placeholder bound through {@code ctx.java(name)}
     */
    public static @NotNull String emit(@NotNull HandlerContext ctx, @NotNull SidecarEntry entry, @NotNull List<IndexedParam> params, @NotNull String returnType) {
        SidecarBindings.addImports(ctx, entry);
        addParamImports(ctx, params);
        addImportFor(ctx, returnType);

        String methodName = METHOD_PREFIX + entry.method() + "_" + ctx.codegen().nextMethodId();
        ctx.codegen().addMethod(buildMethodSource(methodName, entry, params, simpleNameOf(returnType)));

        StringBuilder call = new StringBuilder();
        call.append(methodName).append('(');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) call.append(", ");
            call.append(ctx.java(params.get(i).name()));
        }
        call.append(')');
        return call.toString();
    }

    private static @NotNull String buildMethodSource(@NotNull String name, @NotNull SidecarEntry entry, @NotNull List<IndexedParam> params, @NotNull String returnTypeSimple) {
        StringBuilder sb = new StringBuilder();
        sb.append("private static ").append(returnTypeSimple).append(' ').append(name).append('(');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(Descriptors.simpleNameOf(params.get(i).descriptor())).append(' ').append(params.get(i).name());
        }
        sb.append(") {\n");
        for (String line : entry.runtimeBodyLines()) sb.append("    ").append(line).append('\n');
        sb.append('}');
        return sb.toString();
    }

    private static void addParamImports(@NotNull HandlerContext ctx, @NotNull List<IndexedParam> params) {
        for (IndexedParam p : params) {
            String javaType = Descriptors.javaTypeOf(p.descriptor());
            if (javaType.contains(".") && !javaType.startsWith("java.lang.")) {
                ctx.codegen().addImport(javaType);
            }
        }
    }

    private static void addImportFor(@NotNull HandlerContext ctx, @NotNull String javaType) {
        if (javaType.contains(".") && !javaType.startsWith("java.lang.")) {
            ctx.codegen().addImport(javaType);
        }
    }

    private static @NotNull String simpleNameOf(@NotNull String javaType) {
        int dot = javaType.lastIndexOf('.');
        return dot < 0 ? javaType : javaType.substring(dot + 1);
    }
}
