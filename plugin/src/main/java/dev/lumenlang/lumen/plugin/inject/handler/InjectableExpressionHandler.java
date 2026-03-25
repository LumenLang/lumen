package dev.lumenlang.lumen.plugin.inject.handler;

import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.inject.body.InjectableExpression;
import dev.lumenlang.lumen.plugin.inject.bytecode.BytecodeExtractor;
import dev.lumenlang.lumen.plugin.inject.bytecode.ExtractedBody;
import dev.lumenlang.lumen.pipeline.var.RefType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An {@link ExpressionHandler} that extracts bytecode from an {@link InjectableExpression}
 * and generates a bridge method call that returns a value.
 */
public final class InjectableExpressionHandler implements ExpressionHandler {

    private final InjectableHandlerSupport support;
    private final @Nullable String refTypeId;
    private final @Nullable String javaType;

    /**
     * Creates a handler from the given injectable expression.
     *
     * @param expression the injectable expression whose bytecode will be extracted and injected
     * @param refTypeId the ref type id for the return value (e.g. "PLAYER"), or null
     * @param javaType the Java type for primitive returns (e.g. "int"), or null
     */
    public InjectableExpressionHandler(@NotNull InjectableExpression expression, @Nullable String refTypeId, @Nullable String javaType) {
        ExtractedBody body = BytecodeExtractor.extract(expression);
        String returnTypeJava = resolveReturnType(body, refTypeId, javaType);
        this.support = new InjectableHandlerSupport(body, returnTypeJava);
        this.refTypeId = refTypeId;
        this.javaType = javaType;
    }

    /**
     * Creates a handler from a named static method in the given class.
     *
     * @param clazz the class containing the method
     * @param methodName the name of the static method
     * @param refTypeId the ref type id for the return value, or null
     * @param javaType the Java type for primitive returns, or null
     */
    public InjectableExpressionHandler(@NotNull Class<?> clazz, @NotNull String methodName, @Nullable String refTypeId, @Nullable String javaType) {
        ExtractedBody body = BytecodeExtractor.extractMethod(clazz, methodName);
        String returnTypeJava = resolveReturnType(body, refTypeId, javaType);
        this.support = new InjectableHandlerSupport(body, returnTypeJava);
        this.refTypeId = refTypeId;
        this.javaType = javaType;
    }

    @Override
    public @NotNull ExpressionResult handle(@NotNull BindingAccess ctx) {
        List<ExtractedBody.FakeBinding> bindings = support.emitIfNeeded(ctx.codegen());

        StringBuilder call = new StringBuilder();
        call.append(support.methodName()).append("(");
        for (int i = 0; i < bindings.size(); i++) {
            if (i > 0) call.append(", ");
            call.append(ctx.java(bindings.get(i).bindingName()));
        }
        call.append(")");

        return new ExpressionResult(call.toString(), refTypeId, javaType);
    }

    private static @NotNull String resolveReturnType(@NotNull ExtractedBody body, @Nullable String refTypeId, @Nullable String javaType) {
        if (refTypeId != null) {
            RefType refType = RefType.byId(refTypeId);
            if (refType != null) return refType.javaType();
        }
        if (javaType != null) return javaType;
        return InjectableHandlerSupport.descriptorToJavaType(body.returnDescriptor());
    }
}
