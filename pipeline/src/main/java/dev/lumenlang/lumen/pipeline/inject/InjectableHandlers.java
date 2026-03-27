package dev.lumenlang.lumen.pipeline.inject;

import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.inject.body.InjectableBody;
import dev.lumenlang.lumen.api.inject.body.InjectableCondition;
import dev.lumenlang.lumen.api.inject.body.InjectableExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory bridge for creating injectable handlers. The concrete implementation
 * lives in the plugin module and is registered during initialization.
 */
public final class InjectableHandlers {

    private static Factory factory;

    public static void factory(@NotNull Factory factory) {
        InjectableHandlers.factory = factory;
    }

    public static @NotNull StatementHandler statement(@NotNull InjectableBody body, boolean methodBased) {
        return factory.statement(body, methodBased);
    }

    public static @NotNull StatementHandler statement(@NotNull Class<?> clazz, @NotNull String methodName, boolean methodBased) {
        return factory.statement(clazz, methodName, methodBased);
    }

    public static @NotNull ExpressionHandler expression(@NotNull InjectableExpression expression, @Nullable String refTypeId, @Nullable String javaType, boolean methodBased) {
        return factory.expression(expression, refTypeId, javaType, methodBased);
    }

    public static @NotNull ExpressionHandler expression(@NotNull Class<?> clazz, @NotNull String methodName, @Nullable String refTypeId, @Nullable String javaType, boolean methodBased) {
        return factory.expression(clazz, methodName, refTypeId, javaType, methodBased);
    }

    public static @NotNull ConditionHandler condition(@NotNull InjectableCondition condition, boolean methodBased) {
        return factory.condition(condition, methodBased);
    }

    public static @NotNull ConditionHandler condition(@NotNull Class<?> clazz, @NotNull String methodName, boolean methodBased) {
        return factory.condition(clazz, methodName, methodBased);
    }

    public interface Factory {

        @NotNull StatementHandler statement(@NotNull InjectableBody body, boolean methodBased);

        @NotNull StatementHandler statement(@NotNull Class<?> clazz, @NotNull String methodName, boolean methodBased);

        @NotNull ExpressionHandler expression(@NotNull InjectableExpression expression, @Nullable String refTypeId, @Nullable String javaType, boolean methodBased);

        @NotNull ExpressionHandler expression(@NotNull Class<?> clazz, @NotNull String methodName, @Nullable String refTypeId, @Nullable String javaType, boolean methodBased);

        @NotNull ConditionHandler condition(@NotNull InjectableCondition condition, boolean methodBased);

        @NotNull ConditionHandler condition(@NotNull Class<?> clazz, @NotNull String methodName, boolean methodBased);
    }
}
