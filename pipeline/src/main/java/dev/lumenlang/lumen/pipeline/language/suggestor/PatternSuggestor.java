package dev.lumenlang.lumen.pipeline.language.suggestor;

import dev.lumenlang.lumen.api.language.SemanticKind;
import dev.lumenlang.lumen.api.language.Suggestion;
import dev.lumenlang.lumen.api.type.TypeBindingMeta;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.TypeBinding;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOption;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOptions;
import dev.lumenlang.lumen.pipeline.language.simulator.result.Position;
import dev.lumenlang.lumen.pipeline.language.suggestor.filter.PrefixFilter;
import dev.lumenlang.lumen.pipeline.language.suggestor.filter.PrefixScore;
import dev.lumenlang.lumen.pipeline.language.suggestor.rank.Ranker;
import dev.lumenlang.lumen.pipeline.language.suggestor.render.ShapeRenderer;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.CompletionItem;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.PlanEntry;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.SignatureItem;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.SuggestorPlan;
import dev.lumenlang.lumen.pipeline.language.suggestor.token.ActiveToken;
import dev.lumenlang.lumen.pipeline.language.suggestor.token.ActiveTokenParser;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Editor-facing completion entry point. Takes a raw line plus cursor column, walks every
 * candidate pattern via the simulator, and turns each walk position into a list of completions
 * appropriate for the cursor's slot (literal prefixes or type-binding values).
 */
public final class PatternSuggestor {

    private PatternSuggestor() {
    }

    private static final SimulatorOptions EDITOR_OPTIONS = SimulatorOptions.builder().set(SimulatorOption.MAX_CANDIDATES, 50).set(SimulatorOption.MAX_SUGGESTIONS, 50).set(SimulatorOption.MIN_REPORT_CONFIDENCE, 0.0).build();

    public static @NotNull SuggestorPlan suggest(@NotNull String rawLine, int cursorCol, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull PatternSimulator.Scope scope) {
        return suggest(rawLine, cursorCol, reg, env, scope, EDITOR_OPTIONS);
    }

    public static @NotNull SuggestorPlan suggest(@NotNull String rawLine, int cursorCol, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull PatternSimulator.Scope scope, @NotNull SimulatorOptions opts) {
        ActiveTokenParser.Parsed parsed = ActiveTokenParser.parse(rawLine, cursorCol);
        ActiveToken active = parsed.active();
        List<Token> completed = parsed.completed();
        List<Position> positions = PatternSimulator.positions(completed, reg, env, scope, opts);
        List<PlanEntry> entries = new ArrayList<>(positions.size());
        for (Position p : positions) {
            List<CompletionItem> candidates = candidatesFor(p, active, reg, env);
            SignatureItem signature = ShapeRenderer.render(p);
            double score = Ranker.score(p, completed.size(), active);
            entries.add(new PlanEntry(p.pattern(), p, candidates, signature, score));
        }
        entries.sort(Comparator.comparingDouble(PlanEntry::score).reversed());
        return new SuggestorPlan(active, List.copyOf(entries));
    }

    private static @NotNull List<CompletionItem> candidatesFor(@NotNull Position position, @NotNull ActiveToken active, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env) {
        if (position.atPart() == null) return List.of();
        if (position.atPart() instanceof PatternPart.Literal lit) {
            return literalCandidates(List.of(lit.text()), active);
        }
        if (position.atPart() instanceof PatternPart.FlexLiteral flex) {
            return literalCandidates(flex.forms(), active);
        }
        if (position.atPart() instanceof PatternPart.PlaceholderPart pp) {
            return bindingCandidates(pp.ph().typeId(), active, reg, env);
        }
        if (position.atPart() instanceof PatternPart.Group group) {
            return groupCandidates(group, active);
        }
        return List.of();
    }

    private static @NotNull List<CompletionItem> literalCandidates(@NotNull List<String> forms, @NotNull ActiveToken active) {
        List<CompletionItem> out = new ArrayList<>(forms.size());
        for (String form : forms) {
            if (!PrefixFilter.matches(form, active.text())) continue;
            double score = active.text().isEmpty() ? 1.0 : PrefixScore.of(active.text(), form);
            out.add(new CompletionItem(form, form, "keyword", SemanticKind.KEYWORD, active.rangeStart(), active.rangeEnd(), score));
        }
        out.sort(Comparator.comparingDouble(CompletionItem::score).reversed());
        return List.copyOf(out);
    }

    private static @NotNull List<CompletionItem> bindingCandidates(@NotNull String bindingId, @NotNull ActiveToken active, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env) {
        TypeBinding binding = reg.getTypeRegistry().get(bindingId);
        if (binding == null) return List.of();
        List<Suggestion> raw;
        try {
            raw = binding.suggestions(env, null);
        } catch (RuntimeException e) {
            raw = List.of();
        }
        List<CompletionItem> wrapped = wrap(PrefixFilter.apply(raw, active.text()), active);
        if (!wrapped.isEmpty()) return wrapped;
        return List.of(placeholderHint(bindingId, reg, active));
    }

    private static @NotNull CompletionItem placeholderHint(@NotNull String bindingId, @NotNull PatternRegistry reg, @NotNull ActiveToken active) {
        TypeBindingMeta meta = reg.getTypeRegistry().getMeta(bindingId);
        String label = meta.displayName() != null ? meta.displayName() : "<" + bindingId.toLowerCase() + ">";
        return new CompletionItem("", label, label, SemanticKind.PASSTHROUGH, active.rangeStart(), active.rangeEnd(), 0.0);
    }

    private static @NotNull List<CompletionItem> groupCandidates(@NotNull PatternPart.Group group, @NotNull ActiveToken active) {
        List<CompletionItem> out = new ArrayList<>();
        for (List<PatternPart> alt : group.alternatives()) {
            if (alt.isEmpty()) continue;
            PatternPart first = alt.get(0);
            if (first instanceof PatternPart.Literal lit) {
                if (!PrefixFilter.matches(lit.text(), active.text())) continue;
                double score = active.text().isEmpty() ? 1.0 : PrefixScore.of(active.text(), lit.text());
                out.add(new CompletionItem(lit.text(), lit.text(), "keyword", SemanticKind.KEYWORD, active.rangeStart(), active.rangeEnd(), score));
            } else if (first instanceof PatternPart.FlexLiteral flex) {
                for (String form : flex.forms()) {
                    if (!PrefixFilter.matches(form, active.text())) continue;
                    double score = active.text().isEmpty() ? 1.0 : PrefixScore.of(active.text(), form);
                    out.add(new CompletionItem(form, form, "keyword", SemanticKind.KEYWORD, active.rangeStart(), active.rangeEnd(), score));
                }
            }
        }
        out.sort(Comparator.comparingDouble(CompletionItem::score).reversed());
        return List.copyOf(out);
    }

    private static @NotNull List<CompletionItem> wrap(@NotNull List<Suggestion> raw, @NotNull ActiveToken active) {
        List<CompletionItem> out = new ArrayList<>(raw.size());
        for (Suggestion s : raw) {
            double score = active.text().isEmpty() ? 0.7 : PrefixScore.of(active.text(), s.insertText());
            out.add(new CompletionItem(s.insertText(), s.insertText(), s.detail(), s.kind(), active.rangeStart(), active.rangeEnd(), score));
        }
        out.sort(Comparator.comparingDouble(CompletionItem::score).reversed());
        return List.copyOf(out);
    }
}
