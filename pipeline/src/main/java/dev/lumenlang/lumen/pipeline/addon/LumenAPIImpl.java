package dev.lumenlang.lumen.pipeline.addon;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.binder.ScriptBinderRegistrar;
import dev.lumenlang.lumen.api.emit.BlockEnterHook;
import dev.lumenlang.lumen.api.emit.BlockFormHandler;
import dev.lumenlang.lumen.api.emit.EmitRegistrar;
import dev.lumenlang.lumen.api.emit.StatementFormHandler;
import dev.lumenlang.lumen.api.emit.transform.CodeTransformer;
import dev.lumenlang.lumen.api.emit.transform.TransformerRegistrar;
import dev.lumenlang.lumen.api.event.AdvancedEventBuilder;
import dev.lumenlang.lumen.api.event.AdvancedEventDefinition;
import dev.lumenlang.lumen.api.event.EventBuilder;
import dev.lumenlang.lumen.api.event.EventDefinition;
import dev.lumenlang.lumen.api.event.EventRegistrar;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.handler.LoopHandler;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.inject.body.InjectableBody;
import dev.lumenlang.lumen.api.inject.body.InjectableCondition;
import dev.lumenlang.lumen.api.inject.body.InjectableExpression;
import dev.lumenlang.lumen.api.pattern.PatternRegistrar;
import dev.lumenlang.lumen.api.pattern.builder.BlockBuilder;
import dev.lumenlang.lumen.api.pattern.builder.ConditionBuilder;
import dev.lumenlang.lumen.api.pattern.builder.ExpressionBuilder;
import dev.lumenlang.lumen.api.pattern.builder.LoopBuilder;
import dev.lumenlang.lumen.api.pattern.builder.StatementBuilder;
import dev.lumenlang.lumen.api.placeholder.PlaceholderRegistrar;
import dev.lumenlang.lumen.api.placeholder.PlaceholderType;
import dev.lumenlang.lumen.api.type.AddonTypeBinding;
import dev.lumenlang.lumen.api.type.RefTypeHandle;
import dev.lumenlang.lumen.api.type.RefTypeRegistrar;
import dev.lumenlang.lumen.api.type.TypeRegistrar;
import dev.lumenlang.lumen.pipeline.addon.bridge.TypeBindingBridge;
import dev.lumenlang.lumen.pipeline.events.EventDefRegistry;
import dev.lumenlang.lumen.pipeline.events.def.EventDef;
import dev.lumenlang.lumen.pipeline.inject.InjectableHandlers;
import dev.lumenlang.lumen.pipeline.language.emit.EmitRegistry;
import dev.lumenlang.lumen.pipeline.language.emit.TransformerRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderRegistry;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import dev.lumenlang.lumen.pipeline.var.RefType;
import dev.lumenlang.lumen.pipeline.var.VarDef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Internal implementation of {@link LumenAPI} that delegates to the real
 * registries.
 */
public final class LumenAPIImpl implements LumenAPI {

    private final PatternRegistrar patterns;
    private final TypeRegistrar types;
    private final EventRegistrar events;
    private final RefTypeRegistrar refTypes;
    private final PlaceholderRegistrar placeholders;
    private final EmitRegistrar emitters;
    private final TransformerRegistrar transformerRegistrar;
    private final ScriptBinderManager binderManager;

    public LumenAPIImpl(@NotNull PatternRegistry patternRegistry,
                        @NotNull TypeRegistry typeRegistry,
                        @NotNull EmitRegistry emitRegistry,
                        @NotNull TransformerRegistry transformerRegistry,
                        @NotNull ScriptBinderManager binderManager) {
        this.binderManager = binderManager;
        this.patterns = new PatternRegistrar() {
            @Override
            public void statement(@NotNull String pattern,
                                  @NotNull StatementHandler handler) {
                patternRegistry.statement(pattern, handler);
            }

            @Override
            public void statement(@NotNull List<String> patterns,
                                  @NotNull StatementHandler handler) {
                for (String pattern : patterns) {
                    patternRegistry.statement(pattern, handler);
                }
            }

            @Override
            public void block(@NotNull String pattern,
                              @NotNull BlockHandler handler) {
                patternRegistry.block(pattern, handler);
            }

            @Override
            public void block(@NotNull List<String> patterns,
                              @NotNull BlockHandler handler) {
                for (String pattern : patterns) {
                    patternRegistry.block(pattern, handler);
                }
            }

            @Override
            public void condition(@NotNull String pattern,
                                  @NotNull ConditionHandler handler) {
                patternRegistry.condition(pattern, handler);
            }

            @Override
            public void condition(@NotNull List<String> patterns,
                                  @NotNull ConditionHandler handler) {
                for (String pattern : patterns) {
                    patternRegistry.condition(pattern, handler);
                }
            }

            @Override
            public void expression(@NotNull String pattern,
                                   @NotNull ExpressionHandler handler) {
                patternRegistry.expression(pattern, handler);
            }

            @Override
            public void expression(@NotNull List<String> patterns,
                                   @NotNull ExpressionHandler handler) {
                for (String pattern : patterns) {
                    patternRegistry.expression(pattern, handler);
                }
            }

            @Override
            public void statement(@NotNull Consumer<StatementBuilder> builderConsumer) {
                patternRegistry.statement(builderConsumer);
            }

            @Override
            public void block(@NotNull Consumer<BlockBuilder> builderConsumer) {
                patternRegistry.block(builderConsumer);
            }

            @Override
            public void condition(@NotNull Consumer<ConditionBuilder> builderConsumer) {
                patternRegistry.condition(builderConsumer);
            }

            @Override
            public void expression(@NotNull Consumer<ExpressionBuilder> builderConsumer) {
                patternRegistry.expression(builderConsumer);
            }

            @Override
            public void loop(@NotNull String pattern, @NotNull LoopHandler handler) {
                patternRegistry.loop(pattern, handler);
            }

            @Override
            public void loop(@NotNull List<String> patterns, @NotNull LoopHandler handler) {
                patternRegistry.loop(patterns, handler);
            }

            @Override
            public void loop(@NotNull Consumer<LoopBuilder> builderConsumer) {
                patternRegistry.loop(builderConsumer);
            }

            @Override
            public void injectable(@NotNull String pattern, @NotNull InjectableBody body) {
                patternRegistry.statement(pattern, InjectableHandlers.statement(body, false));
            }

            @Override
            public void injectable(@NotNull List<String> patterns, @NotNull InjectableBody body) {
                patternRegistry.statement(patterns, InjectableHandlers.statement(body, false));
            }

            @Override
            public void injectableExpression(@NotNull String pattern, @Nullable String refTypeId, @Nullable String javaType, @NotNull InjectableExpression expression) {
                patternRegistry.expression(pattern, InjectableHandlers.expression(expression, refTypeId, javaType, false));
            }

            @Override
            public void injectableExpression(@NotNull List<String> patterns, @Nullable String refTypeId, @Nullable String javaType, @NotNull InjectableExpression expression) {
                patternRegistry.expression(patterns, InjectableHandlers.expression(expression, refTypeId, javaType, false));
            }

            @Override
            public void injectableCondition(@NotNull String pattern, @NotNull InjectableCondition condition) {
                patternRegistry.condition(pattern, InjectableHandlers.condition(condition, false));
            }

            @Override
            public void injectableCondition(@NotNull List<String> patterns, @NotNull InjectableCondition condition) {
                patternRegistry.condition(patterns, InjectableHandlers.condition(condition, false));
            }
        };

        // noinspection Convert2Lambda
        this.types = new TypeRegistrar() {
            @Override
            public void register(@NotNull AddonTypeBinding binding) {
                typeRegistry.register(new TypeBindingBridge(binding));
                typeRegistry.registerMeta(binding.id(), binding.meta());
            }
        };

        this.events = new EventRegistrar() {
            @Override
            public void register(@NotNull EventDefinition def) {
                EventDef internal = new EventDef();
                internal.name = def.name();
                internal.className = def.className();
                for (var entry : def.vars().entrySet()) {
                    String name = entry.getKey();
                    EventDefinition.VarEntry ve = entry.getValue();
                    RefType rt = ve.refTypeId() != null ? RefType.byId(ve.refTypeId()) : null;
                    if (ve.refTypeId() != null && rt == null) {
                        throw new IllegalArgumentException(
                                "Unknown ref type: " + ve.refTypeId());
                    }
                    internal.vars.put(name, new VarDef(rt, ve.javaType(), ve.expr()));
                }
                EventDefRegistry.register(internal);
                EventDefRegistry.registerApiDefinition(def);
            }

            @Override
            public @NotNull EventBuilder builder(@NotNull String name) {
                return new EventBuilder(name);
            }

            @Override
            public @Nullable EventDefinition lookup(@NotNull String name) {
                String normalized = name.replace(' ', '_');
                EventDefinition apiDef = EventDefRegistry.apiDefinitions().get(normalized);
                if (apiDef == null)
                    apiDef = EventDefRegistry.apiDefinitions().get(name);
                if (apiDef != null)
                    return apiDef;

                EventDef internal = EventDefRegistry.get(name);
                if (internal == null)
                    return null;

                EventBuilder b = new EventBuilder(internal.name)
                        .className(internal.className);
                for (var entry : internal.vars.entrySet()) {
                    VarDef vd = entry.getValue();
                    b.addVar(entry.getKey(), vd.refType(), vd.javaType(), vd.expr());
                }
                return b.build();
            }

            @Override
            public void advanced(@NotNull Consumer<AdvancedEventBuilder> builderConsumer) {
                AdvancedEventBuilder builder = new AdvancedEventBuilder();
                builderConsumer.accept(builder);
                registerAdvanced(builder.build());
            }

            @Override
            public void registerAdvanced(@NotNull AdvancedEventDefinition def) {
                EventDefRegistry.registerAdvanced(def);
            }

            @Override
            public @Nullable AdvancedEventDefinition lookupAdvanced(@NotNull String name) {
                return EventDefRegistry.getAdvanced(name);
            }
        };

        this.refTypes = new RefTypeRegistrar() {
            @Override
            public @NotNull RefTypeHandle register(@NotNull String id, @NotNull String javaType) {
                return RefType.register(id, javaType);
            }

            @Override
            public @Nullable RefTypeHandle byId(@NotNull String id) {
                return RefType.byId(id);
            }
        };

        this.placeholders = new PlaceholderRegistrar() {
            @Override
            public void property(@NotNull RefTypeHandle type, @NotNull String property, @NotNull String template) {
                property(type, property, template, PlaceholderType.STRING);
            }

            @Override
            public void property(@NotNull RefTypeHandle type, @NotNull String property, @NotNull String template,
                                 @NotNull PlaceholderType returnType) {
                RefType internal = type instanceof RefType rt ? rt : RefType.byId(type.id());
                if (internal == null) {
                    throw new IllegalArgumentException("Unknown ref type: " + type.id());
                }
                PlaceholderRegistry.registerProperty(internal, property, template, returnType);
            }

            @Override
            public void defaultProperty(@NotNull RefTypeHandle type, @NotNull String defaultProperty) {
                RefType internal = type instanceof RefType rt ? rt : RefType.byId(type.id());
                if (internal == null) {
                    throw new IllegalArgumentException("Unknown ref type: " + type.id());
                }
                PlaceholderRegistry.registerDefault(internal, defaultProperty);
            }
        };

        this.emitters = new EmitRegistrar() {
            @Override
            public void statementForm(@NotNull StatementFormHandler handler) {
                emitRegistry.addStatementForm(handler);
            }

            @Override
            public void blockForm(@NotNull BlockFormHandler handler) {
                emitRegistry.addBlockForm(handler);
            }

            @Override
            public void blockEnterHook(@NotNull BlockEnterHook hook) {
                emitRegistry.addBlockEnterHook(hook);
            }
        };

        this.transformerRegistrar = new TransformerRegistrar() {
            @Override
            public void register(@NotNull CodeTransformer transformer) {
                transformerRegistry.addTransformer(transformer);
            }

            @Override
            public void unregister(@NotNull String tag) {
                transformerRegistry.removeTransformer(tag);
            }
        };
    }

    @Override
    public @NotNull PatternRegistrar patterns() {
        return patterns;
    }

    @Override
    public @NotNull TypeRegistrar types() {
        return types;
    }

    @Override
    public @NotNull EventRegistrar events() {
        return events;
    }

    @Override
    public @NotNull RefTypeRegistrar refTypes() {
        return refTypes;
    }

    @Override
    public @NotNull PlaceholderRegistrar placeholders() {
        return placeholders;
    }

    @Override
    public @NotNull EmitRegistrar emitters() {
        return emitters;
    }

    @Override
    public @NotNull ScriptBinderRegistrar binders() {
        return binderManager;
    }

    @Override
    public @NotNull TransformerRegistrar transformers() {
        return transformerRegistrar;
    }
}
