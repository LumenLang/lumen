package dev.lumenlang.lumen.plugin.inject.handler;

import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.inject.body.InjectableBody;
import dev.lumenlang.lumen.pipeline.inject.PatternHinted;
import dev.lumenlang.lumen.plugin.inject.bytecode.BytecodeExtractor;
import dev.lumenlang.lumen.plugin.inject.bytecode.ExtractedBody;
import dev.lumenlang.lumen.plugin.inject.bytecode.MethodDecompiler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link StatementHandler} that extracts bytecode from an {@link InjectableBody}.
 * In inline mode (default), the decompiled body is emitted directly at the call site.
 * In method based mode, a bridge method call is generated instead.
 */
public final class InjectableStatementHandler implements StatementHandler, PatternHinted {

    private final InjectableHandlerSupport support;

    /**
     * Creates a handler from the given injectable body.
     *
     * @param body        the injectable body whose bytecode will be extracted and injected
     * @param methodBased true to force method based injection instead of inline
     */
    public InjectableStatementHandler(@NotNull InjectableBody body, boolean methodBased) {
        this.support = new InjectableHandlerSupport(BytecodeExtractor.extract(body), "void", methodBased);
    }

    /**
     * Creates a handler from a named static method in the given class.
     *
     * @param clazz       the class containing the method
     * @param methodName  the name of the static method
     * @param methodBased true to force method based injection instead of inline
     */
    public InjectableStatementHandler(@NotNull Class<?> clazz, @NotNull String methodName, boolean methodBased) {
        this.support = new InjectableHandlerSupport(BytecodeExtractor.extractMethod(clazz, methodName), "void", methodBased);
    }

    private static boolean canInlineStatement(@NotNull MethodDecompiler.DecompiledInlineBody body) {
        if (body.alwaysThrows()) return false;
        for (String line : body.bodyLines()) {
            if (line.trim().equals("return;")) return false;
        }
        return true;
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
    public void handle(int line, @NotNull BindingAccess ctx, @NotNull JavaOutput out) {
        MethodDecompiler.DecompiledInlineBody inlineBody = support.inlineBody();
        if (inlineBody != null && canInlineStatement(inlineBody)) {
            support.addInlineImports(ctx.codegen());
            Map<String, String> bindingExpressions = new HashMap<>();
            for (ExtractedBody.FakeBinding binding : support.bindings()) {
                bindingExpressions.put(binding.bindingName(), ctx.java(binding.bindingName()));
            }
            out.line("// @injected");
            for (String bodyLine : inlineBody.bodyLines()) {
                out.line(support.replaceBindings(bodyLine, bindingExpressions));
            }
            return;
        }

        List<ExtractedBody.FakeBinding> bindings = support.emitIfNeeded(ctx.codegen());
        StringBuilder call = new StringBuilder();
        call.append(support.methodName()).append("(");
        for (int i = 0; i < bindings.size(); i++) {
            if (i > 0) call.append(", ");
            call.append(ctx.java(bindings.get(i).bindingName()));
        }
        call.append(");");
        out.line(call.toString());
    }
}
