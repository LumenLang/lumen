package dev.lumenlang.lumen.plugin.inject.handler;

import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.inject.body.InjectableBody;
import dev.lumenlang.lumen.plugin.inject.bytecode.BytecodeExtractor;
import dev.lumenlang.lumen.plugin.inject.bytecode.ExtractedBody;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@link StatementHandler} that extracts bytecode from an {@link InjectableBody} and
 * generates a bridge method call in the script class.
 */
public final class InjectableStatementHandler implements StatementHandler {

    private final InjectableHandlerSupport support;

    /**
     * Creates a handler from the given injectable body.
     *
     * @param body the injectable body whose bytecode will be extracted and injected
     */
    public InjectableStatementHandler(@NotNull InjectableBody body) {
        this.support = new InjectableHandlerSupport(BytecodeExtractor.extract(body), "void");
    }

    /**
     * Creates a handler from a named static method in the given class.
     *
     * @param clazz the class containing the method
     * @param methodName the name of the static method
     */
    public InjectableStatementHandler(@NotNull Class<?> clazz, @NotNull String methodName) {
        this.support = new InjectableHandlerSupport(BytecodeExtractor.extractMethod(clazz, methodName), "void");
    }

    @Override
    public void handle(int line, @NotNull BindingAccess ctx, @NotNull JavaOutput out) {
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
