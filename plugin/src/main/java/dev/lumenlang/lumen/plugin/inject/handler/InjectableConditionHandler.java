package dev.lumenlang.lumen.plugin.inject.handler;

import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.pipeline.inject.PatternHinted;
import dev.lumenlang.lumen.api.inject.body.InjectableCondition;
import dev.lumenlang.lumen.plugin.inject.bytecode.BytecodeExtractor;
import dev.lumenlang.lumen.plugin.inject.bytecode.ExtractedBody;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@link ConditionHandler} that extracts bytecode from an {@link InjectableCondition}
 * and generates a bridge method call that returns a boolean.
 */
public final class InjectableConditionHandler implements ConditionHandler, PatternHinted {

    private final InjectableHandlerSupport support;

    /**
     * Creates a handler from the given injectable condition.
     *
     * @param condition the injectable condition whose bytecode will be extracted and injected
     */
    public InjectableConditionHandler(@NotNull InjectableCondition condition) {
        this.support = new InjectableHandlerSupport(BytecodeExtractor.extract(condition), "boolean");
    }

    /**
     * Creates a handler from a named static method in the given class.
     *
     * @param clazz      the class containing the method
     * @param methodName the name of the static method
     */
    public InjectableConditionHandler(@NotNull Class<?> clazz, @NotNull String methodName) {
        this.support = new InjectableHandlerSupport(BytecodeExtractor.extractMethod(clazz, methodName), "boolean");
    }

    @Override
    public void patternHint(@NotNull String pattern) {
        support.patternHint(pattern);
    }

    @Override
    public @NotNull String handle(@NotNull ConditionMatch match, @NotNull EnvironmentAccess env, @NotNull CodegenAccess ctx) {
        List<ExtractedBody.FakeBinding> bindings = support.emitIfNeeded(ctx);

        StringBuilder call = new StringBuilder();
        call.append(support.methodName()).append("(");
        for (int i = 0; i < bindings.size(); i++) {
            if (i > 0) call.append(", ");
            call.append(match.java(bindings.get(i).bindingName(), ctx, env));
        }
        call.append(")");

        return call.toString();
    }
}
