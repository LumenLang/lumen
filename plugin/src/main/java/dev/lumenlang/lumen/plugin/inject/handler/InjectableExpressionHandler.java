package dev.lumenlang.lumen.plugin.inject.handler;

import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.inject.body.InjectableExpression;
import dev.lumenlang.lumen.pipeline.inject.PatternHinted;
import dev.lumenlang.lumen.api.type.LumenTypeRegistry;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.plugin.inject.bytecode.BytecodeExtractor;
import dev.lumenlang.lumen.plugin.inject.bytecode.ExtractedBody;
import dev.lumenlang.lumen.plugin.inject.bytecode.MethodDecompiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link ExpressionHandler} that extracts bytecode from an {@link InjectableExpression}.
 * In inline mode (default), the decompiled return expression is used directly.
 * In method based mode, a bridge method call is generated.
 */
public final class InjectableExpressionHandler implements ExpressionHandler, PatternHinted {

    private final InjectableHandlerSupport support;
    private final @Nullable String typeId;

    /**
     * Creates a handler from the given injectable expression.
     *
     * @param expression the injectable expression whose bytecode will be extracted and injected
     * @param typeId     the type id for the return value (e.g. "PLAYER" or "int"), or null
     */
    public InjectableExpressionHandler(@NotNull InjectableExpression expression, @Nullable String typeId) {
        ExtractedBody body = BytecodeExtractor.extract(expression);
        String returnTypeJava = resolveReturnType(body, typeId);
        this.support = new InjectableHandlerSupport(body, returnTypeJava, false);
        this.typeId = typeId;
    }

    /**
     * Creates a handler from a named static method in the given class.
     *
     * @param clazz      the class containing the method
     * @param methodName the name of the static method
     * @param typeId     the type id for the return value, or null
     */
    public InjectableExpressionHandler(@NotNull Class<?> clazz, @NotNull String methodName, @Nullable String typeId) {
        ExtractedBody body = BytecodeExtractor.extractMethod(clazz, methodName);
        String returnTypeJava = resolveReturnType(body, typeId);
        this.support = new InjectableHandlerSupport(body, returnTypeJava, false);
        this.typeId = typeId;
    }

    private static @NotNull String resolveReturnType(@NotNull ExtractedBody body, @Nullable String typeId) {
        if (typeId != null) {
            ObjectType resolvedType = LumenTypeRegistry.byId(typeId);
            if (resolvedType != null) return resolvedType.javaType();
            return typeId;
        }
        return InjectableHandlerSupport.descriptorToJavaType(body.returnDescriptor());
    }

    @Override
    public void patternHint(@NotNull String pattern) {
        support.patternHint(pattern);
    }

    @Override
    public void validateAdditionalPattern(@NotNull String pattern) {
        support.validateAdditionalPattern(pattern);
    }

    @Override
    public @NotNull ExpressionResult handle(@NotNull BindingAccess ctx) {
        MethodDecompiler.DecompiledInlineBody inlineBody = support.inlineBody();
        if (inlineBody != null && inlineBody.returnExpression() != null) {
            support.addInlineImports(ctx.codegen());
            Map<String, String> bindingExpressions = new HashMap<>();
            for (ExtractedBody.FakeBinding binding : support.bindings()) {
                bindingExpressions.put(binding.bindingName(), ctx.java(binding.bindingName()));
            }
            String expression = support.replaceBindings(inlineBody.returnExpression(), bindingExpressions);
            return new ExpressionResult(expression, typeId);
        }

        List<ExtractedBody.FakeBinding> bindings = support.emitIfNeeded(ctx.codegen());
        StringBuilder call = new StringBuilder();
        call.append(support.methodName()).append("(");
        for (int i = 0; i < bindings.size(); i++) {
            if (i > 0) call.append(", ");
            call.append(ctx.java(bindings.get(i).bindingName()));
        }
        call.append(")");
        return new ExpressionResult(call.toString(), typeId);
    }
}
