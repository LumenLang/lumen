package dev.lumenlang.lumen.pipeline.language.simulator.walk;

import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.TypeBinding;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.pattern.Placeholder;
import dev.lumenlang.lumen.pipeline.language.simulator.result.Position;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Forward-only pattern walk producing a {@link Position}. Walks parts left to right, consumes
 * tokens via the registered {@link TypeBinding}s for placeholders, and stops at the first
 * failed part or when input runs out. No backtracking, no fuzzy matching, no fallback.
 */
public final class Walker {

    private Walker() {
    }

    /**
     * Walks {@code pattern} against {@code tokens} and reports where the walk landed.
     */
    public static @NotNull Position walk(@NotNull List<Token> tokens, @NotNull Pattern pattern, double confidence, @NotNull TypeRegistry types, @NotNull TypeEnvImpl env) {
        State state = new State(tokens, types, env);
        walkParts(pattern.parts(), state);
        return new Position(pattern, confidence, state.ti, state.stoppedAt, state.stoppedAtBindingId, state.remainingParts, Map.copyOf(state.bound));
    }

    private static void walkParts(@NotNull List<PatternPart> parts, @NotNull State state) {
        for (int pi = 0; pi < parts.size(); pi++) {
            PatternPart part = parts.get(pi);
            if (state.stopped) {
                state.remainingParts = parts.subList(pi, parts.size());
                return;
            }
            consumePart(part, state, parts, pi);
        }
    }

    private static void consumePart(@NotNull PatternPart part, @NotNull State state, @NotNull List<PatternPart> siblings, int pi) {
        if (part instanceof PatternPart.Literal lit) {
            consumeLiteral(part, lit.text(), state, siblings, pi);
        } else if (part instanceof PatternPart.FlexLiteral flex) {
            consumeFlex(part, flex.forms(), state, siblings, pi);
        } else if (part instanceof PatternPart.PlaceholderPart pp) {
            consumePlaceholder(part, pp.ph(), state, siblings, pi);
        } else if (part instanceof PatternPart.Group group) {
            consumeGroup(part, group, state, siblings, pi);
        }
    }

    private static void consumeLiteral(@NotNull PatternPart part, @NotNull String literal, @NotNull State state, @NotNull List<PatternPart> siblings, int pi) {
        if (state.ti >= state.tokens.size()) {
            state.stop(part, null, siblings, pi);
            return;
        }
        int merged = tryMergeTokens(state.tokens, state.ti, literal);
        if (merged < 0) {
            state.stop(part, null, siblings, pi);
            return;
        }
        state.ti += merged;
    }

    private static void consumeFlex(@NotNull PatternPart part, @NotNull List<String> forms, @NotNull State state, @NotNull List<PatternPart> siblings, int pi) {
        if (state.ti >= state.tokens.size()) {
            state.stop(part, null, siblings, pi);
            return;
        }
        int best = -1;
        for (String form : forms) {
            int merged = tryMergeTokens(state.tokens, state.ti, form);
            if (merged > best) best = merged;
        }
        if (best < 0) {
            state.stop(part, null, siblings, pi);
            return;
        }
        state.ti += best;
    }

    private static void consumePlaceholder(@NotNull PatternPart part, @NotNull Placeholder ph, @NotNull State state, @NotNull List<PatternPart> siblings, int pi) {
        TypeBinding binding = state.types.get(ph.typeId());
        if (binding == null) {
            state.stop(part, ph.typeId(), siblings, pi);
            return;
        }
        if (state.ti >= state.tokens.size()) {
            state.stop(part, ph.typeId(), siblings, pi);
            return;
        }
        List<Token> remaining = state.tokens.subList(state.ti, state.tokens.size());
        int consume;
        try {
            consume = binding.consumeCount(remaining, state.env);
        } catch (RuntimeException e) {
            state.stop(part, ph.typeId(), siblings, pi);
            return;
        }
        int end = consume < 0 ? state.tokens.size() : Math.min(state.ti + consume, state.tokens.size());
        List<Token> slice = state.tokens.subList(state.ti, end);
        try {
            binding.parse(slice, state.env);
        } catch (RuntimeException e) {
            state.stop(part, ph.typeId(), siblings, pi);
            return;
        }
        state.bound.put(ph.name(), List.copyOf(slice));
        state.ti = end;
    }

    private static void consumeGroup(@NotNull PatternPart part, @NotNull PatternPart.Group group, @NotNull State state, @NotNull List<PatternPart> siblings, int pi) {
        if (state.ti >= state.tokens.size()) {
            state.stop(part, null, siblings, pi);
            return;
        }
        for (List<PatternPart> alt : group.alternatives()) {
            int savedTi = state.ti;
            Map<String, List<Token>> savedBound = new LinkedHashMap<>(state.bound);
            walkParts(alt, state);
            if (!state.stopped) return;
            state.ti = savedTi;
            state.bound = savedBound;
            state.stopped = false;
            state.stoppedAt = null;
            state.stoppedAtBindingId = null;
            state.remainingParts = List.of();
        }
        if (!group.required()) return;
        state.stop(part, null, siblings, pi);
    }

    private static int tryMergeTokens(@NotNull List<Token> tokens, int start, @NotNull String literal) {
        if (tokens.get(start).text().equalsIgnoreCase(literal)) return 1;
        StringBuilder merged = new StringBuilder(tokens.get(start).text());
        for (int i = start + 1; i < tokens.size(); i++) {
            Token prev = tokens.get(i - 1);
            Token curr = tokens.get(i);
            if (curr.start() != prev.end()) break;
            merged.append(curr.text());
            if (merged.toString().equalsIgnoreCase(literal)) return i - start + 1;
            if (merged.length() > literal.length()) break;
        }
        return -1;
    }

    private static final class State {
        private final @NotNull List<Token> tokens;
        private final @NotNull TypeRegistry types;
        private final @NotNull TypeEnvImpl env;
        private @NotNull Map<String, List<Token>> bound = new LinkedHashMap<>();
        private int ti = 0;
        private boolean stopped = false;
        private @Nullable PatternPart stoppedAt = null;
        private @Nullable String stoppedAtBindingId = null;
        private @NotNull List<PatternPart> remainingParts = new ArrayList<>();

        State(@NotNull List<Token> tokens, @NotNull TypeRegistry types, @NotNull TypeEnvImpl env) {
            this.tokens = tokens;
            this.types = types;
            this.env = env;
        }

        void stop(@NotNull PatternPart part, @Nullable String bindingId, @NotNull List<PatternPart> siblings, int pi) {
            stopped = true;
            stoppedAt = part;
            stoppedAtBindingId = bindingId;
            remainingParts = siblings.subList(pi, siblings.size());
        }
    }
}
