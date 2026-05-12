package dev.lumenlang.lumen.pipeline.language.suggestor.result;

import dev.lumenlang.lumen.pipeline.language.suggestor.token.ActiveToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Full ranked suggestor output, with views tailored for completion popup, signature line, and
 * inline ghost text.
 *
 * @param active  the cursor's active token (PREFIX text or empty for COMPLETE)
 * @param entries ranked candidate patterns, best first
 */
public record SuggestorPlan(@NotNull ActiveToken active, @NotNull List<PlanEntry> entries) {

    private static final double MIN_GHOST_CONFIDENCE = 0.85;

    /**
     * Merged completion list across top-ranked entries that share the same active binding
     * (or no binding, i.e. literal slots), within {@code 0.10} of the best score, deduplicated
     * by insert text. Cross-binding entries do not pollute the list.
     */
    public @NotNull List<CompletionItem> completions() {
        if (entries.isEmpty()) return List.of();
        double topScore = entries.get(0).score();
        String topBinding = entries.get(0).position().atBindingId();
        Map<String, CompletionItem> dedup = new LinkedHashMap<>();
        for (PlanEntry entry : entries) {
            if (entry.score() < topScore - 0.10) break;
            String entryBinding = entry.position().atBindingId();
            if (!Objects.equals(entryBinding, topBinding)) continue;
            for (CompletionItem c : entry.candidates()) {
                CompletionItem prev = dedup.get(c.insertText());
                if (prev == null || c.score() > prev.score()) dedup.put(c.insertText(), c);
            }
        }
        List<CompletionItem> out = new ArrayList<>(dedup.values());
        out.sort((a, b) -> Double.compare(b.score(), a.score()));
        return List.copyOf(out);
    }

    /**
     * Signature line for the top-ranked entry, or {@code null} when no entries survived.
     */
    public @Nullable SignatureItem signature() {
        if (entries.isEmpty()) return null;
        return entries.get(0).signature();
    }

    /**
     * Inline ghost-text continuation when the top entry's best candidate is confidently unique,
     * otherwise {@code null}.
     */
    public @Nullable GhostText ghostText() {
        if (entries.isEmpty()) return null;
        if (active.policy() != ActiveToken.Policy.PREFIX || active.text().isEmpty()) return null;
        PlanEntry top = entries.get(0);
        if (top.candidates().isEmpty()) return null;
        CompletionItem best = top.candidates().get(0);
        if (best.score() < MIN_GHOST_CONFIDENCE) return null;
        if (top.candidates().size() > 1) {
            CompletionItem second = top.candidates().get(1);
            if (second.score() >= best.score() - 0.05) return null;
        }
        if (!best.insertText().toLowerCase().startsWith(active.text().toLowerCase())) return null;
        String continuation = best.insertText().substring(active.text().length());
        if (continuation.isEmpty()) return null;
        return new GhostText(continuation, active.rangeEnd());
    }

    /**
     * Playground breakdown tree with all entries and their attributes.
     */
    public @NotNull List<TreeNode> tree() {
        List<TreeNode> out = new ArrayList<>(entries.size());
        for (PlanEntry entry : entries) {
            Map<String, Object> attrs = new LinkedHashMap<>();
            attrs.put("confidence", entry.position().confidence());
            attrs.put("consumed", entry.position().consumedTokens());
            attrs.put("score", entry.score());
            attrs.put("atBinding", entry.position().atBindingId());
            List<TreeNode> children = new ArrayList<>(entry.candidates().size());
            for (CompletionItem c : entry.candidates()) {
                Map<String, Object> cAttrs = new LinkedHashMap<>();
                cAttrs.put("insert", c.insertText());
                cAttrs.put("detail", c.detail());
                cAttrs.put("score", c.score());
                children.add(new TreeNode(c.displayLabel(), List.of(), null, Map.copyOf(cAttrs)));
            }
            out.add(new TreeNode(entry.pattern().raw(), List.copyOf(children), entry.position(), Map.copyOf(attrs)));
        }
        return List.copyOf(out);
    }
}
