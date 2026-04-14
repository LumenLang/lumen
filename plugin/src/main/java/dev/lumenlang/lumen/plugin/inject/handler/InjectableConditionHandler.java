package dev.lumenlang.lumen.plugin.inject.handler;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.inject.body.InjectableCondition;
import dev.lumenlang.lumen.pipeline.inject.PatternHinted;
import dev.lumenlang.lumen.plugin.inject.bytecode.BytecodeExtractor;
import dev.lumenlang.lumen.plugin.inject.bytecode.ExtractedBody;
import dev.lumenlang.lumen.plugin.inject.bytecode.MethodDecompiler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link ConditionHandler} that extracts bytecode from an {@link InjectableCondition}.
 * In inline mode (default), the decompiled return expression is used directly.
 * In method based mode, a bridge method call is generated.
 */
public final class InjectableConditionHandler implements ConditionHandler, PatternHinted {

    private final InjectableHandlerSupport support;

    /**
     * Creates a handler from the given injectable condition.
     *
     * @param condition the injectable condition whose bytecode will be extracted and injected
     */
    public InjectableConditionHandler(@NotNull InjectableCondition condition) {
        this.support = new InjectableHandlerSupport(BytecodeExtractor.extract(condition), "boolean", false);
    }

    /**
     * Creates a handler from a named static method in the given class.
     *
     * @param clazz      the class containing the method
     * @param methodName the name of the static method
     */
    public InjectableConditionHandler(@NotNull Class<?> clazz, @NotNull String methodName) {
        this.support = new InjectableHandlerSupport(BytecodeExtractor.extractMethod(clazz, methodName), "boolean", false);
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
    public @NotNull String handle(@NotNull HandlerContext ctx) {
        MethodDecompiler.DecompiledInlineBody inlineBody = support.inlineBody();
        if (inlineBody != null && inlineBody.returnExpression() != null) {
            support.addInlineImports(ctx.codegen());
            Map<String, String> bindingExpressions = new HashMap<>();
            for (ExtractedBody.FakeBinding binding : support.bindings()) {
                bindingExpressions.put(binding.bindingName(), ctx.java(binding.bindingName()));
            }
            return support.replaceBindings(inlineBody.returnExpression(), bindingExpressions);
        }

        List<ExtractedBody.FakeBinding> bindings = support.emitIfNeeded(ctx.codegen());
        StringBuilder call = new StringBuilder();
        call.append(support.methodName()).append("(");
        for (int i = 0; i < bindings.size(); i++) {
            if (i > 0) call.append(", ");
            call.append(ctx.java(bindings.get(i).bindingName()));
        }
        call.append(")");
        return call.toString();
    }
}
