package net.vansencool.lumen.pipeline.addon;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.emit.BlockEnterHook;
import net.vansencool.lumen.api.emit.BlockFormHandler;
import net.vansencool.lumen.api.emit.EmitRegistrar;
import net.vansencool.lumen.api.emit.StatementFormHandler;
import net.vansencool.lumen.api.event.AdvancedEventBuilder;
import net.vansencool.lumen.api.event.AdvancedEventDefinition;
import net.vansencool.lumen.api.event.EventBuilder;
import net.vansencool.lumen.api.event.EventDefinition;
import net.vansencool.lumen.api.event.EventRegistrar;
import net.vansencool.lumen.api.handler.BlockHandler;
import net.vansencool.lumen.api.handler.ConditionHandler;
import net.vansencool.lumen.api.handler.ExpressionHandler;
import net.vansencool.lumen.api.handler.LoopHandler;
import net.vansencool.lumen.api.handler.StatementHandler;
import net.vansencool.lumen.api.pattern.PatternRegistrar;
import net.vansencool.lumen.api.pattern.builder.BlockBuilder;
import net.vansencool.lumen.api.pattern.builder.ConditionBuilder;
import net.vansencool.lumen.api.pattern.builder.ExpressionBuilder;
import net.vansencool.lumen.api.pattern.builder.LoopBuilder;
import net.vansencool.lumen.api.pattern.builder.StatementBuilder;
import net.vansencool.lumen.api.placeholder.PlaceholderRegistrar;
import net.vansencool.lumen.api.placeholder.PlaceholderType;
import net.vansencool.lumen.api.type.AddonTypeBinding;
import net.vansencool.lumen.api.type.RefTypeHandle;
import net.vansencool.lumen.api.type.RefTypeRegistrar;
import net.vansencool.lumen.api.type.TypeRegistrar;
import net.vansencool.lumen.pipeline.addon.bridge.TypeBindingBridge;
import net.vansencool.lumen.pipeline.events.EventDefRegistry;
import net.vansencool.lumen.pipeline.events.def.EventDef;
import net.vansencool.lumen.pipeline.language.emit.EmitRegistry;
import net.vansencool.lumen.pipeline.language.pattern.PatternRegistry;
import net.vansencool.lumen.pipeline.placeholder.PlaceholderRegistry;
import net.vansencool.lumen.pipeline.typebinding.TypeRegistry;
import net.vansencool.lumen.pipeline.var.RefType;
import net.vansencool.lumen.pipeline.var.VarDef;
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

    public LumenAPIImpl(@NotNull PatternRegistry patternRegistry,
                        @NotNull TypeRegistry typeRegistry,
                        @NotNull EmitRegistry emitRegistry) {
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
}
