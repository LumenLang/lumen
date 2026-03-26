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

    public static @NotNull StatementHandler statement(@NotNull InjectableBody body) {
        return factory.statement(body);
    }

    public static @NotNull StatementHandler statement(@NotNull Class<?> clazz, @NotNull String methodName) {
        return factory.statement(clazz, methodName);
    }

    public static @NotNull ExpressionHandler expression(@NotNull InjectableExpression expression, @Nullable String refTypeId, @Nullable String javaType) {
        return factory.expression(expression, refTypeId, javaType);
    }

    public static @NotNull ExpressionHandler expression(@NotNull Class<?> clazz, @NotNull String methodName, @Nullable String refTypeId, @Nullable String javaType) {
        return factory.expression(clazz, methodName, refTypeId, javaType);
    }

    public static @NotNull ConditionHandler condition(@NotNull InjectableCondition condition) {
        return factory.condition(condition);
    }

    public static @NotNull ConditionHandler condition(@NotNull Class<?> clazz, @NotNull String methodName) {
        return factory.condition(clazz, methodName);
    }

    public interface Factory {

        @NotNull StatementHandler statement(@NotNull InjectableBody body);

        @NotNull StatementHandler statement(@NotNull Class<?> clazz, @NotNull String methodName);

        @NotNull ExpressionHandler expression(@NotNull InjectableExpression expression, @Nullable String refTypeId, @Nullable String javaType);

        @NotNull ExpressionHandler expression(@NotNull Class<?> clazz, @NotNull String methodName, @Nullable String refTypeId, @Nullable String javaType);

        @NotNull ConditionHandler condition(@NotNull InjectableCondition condition);

        @NotNull ConditionHandler condition(@NotNull Class<?> clazz, @NotNull String methodName);
    }
}
