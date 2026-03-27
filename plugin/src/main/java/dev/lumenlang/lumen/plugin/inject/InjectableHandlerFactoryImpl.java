package dev.lumenlang.lumen.plugin.inject;

import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.inject.body.InjectableBody;
import dev.lumenlang.lumen.api.inject.body.InjectableCondition;
import dev.lumenlang.lumen.api.inject.body.InjectableExpression;
import dev.lumenlang.lumen.pipeline.inject.InjectableHandlers;
import dev.lumenlang.lumen.plugin.inject.handler.InjectableConditionHandler;
import dev.lumenlang.lumen.plugin.inject.handler.InjectableExpressionHandler;
import dev.lumenlang.lumen.plugin.inject.handler.InjectableStatementHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InjectableHandlerFactoryImpl implements InjectableHandlers.Factory {

    @Override
    public @NotNull StatementHandler statement(@NotNull InjectableBody body, boolean methodBased) {
        return new InjectableStatementHandler(body, methodBased);
    }

    @Override
    public @NotNull StatementHandler statement(@NotNull Class<?> clazz, @NotNull String methodName, boolean methodBased) {
        return new InjectableStatementHandler(clazz, methodName, methodBased);
    }

    @Override
    public @NotNull ExpressionHandler expression(@NotNull InjectableExpression expression, @Nullable String refTypeId, @Nullable String javaType, boolean methodBased) {
        return new InjectableExpressionHandler(expression, refTypeId, javaType, methodBased);
    }

    @Override
    public @NotNull ExpressionHandler expression(@NotNull Class<?> clazz, @NotNull String methodName, @Nullable String refTypeId, @Nullable String javaType, boolean methodBased) {
        return new InjectableExpressionHandler(clazz, methodName, refTypeId, javaType, methodBased);
    }

    @Override
    public @NotNull ConditionHandler condition(@NotNull InjectableCondition condition, boolean methodBased) {
        return new InjectableConditionHandler(condition, methodBased);
    }

    @Override
    public @NotNull ConditionHandler condition(@NotNull Class<?> clazz, @NotNull String methodName, boolean methodBased) {
        return new InjectableConditionHandler(clazz, methodName, methodBased);
    }
}
