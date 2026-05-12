package dev.lumenlang.lumen.pipeline.language.suggestor.result;

import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.simulator.result.Position;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * One candidate pattern's entry in a {@link SuggestorPlan}.
 *
 * @param pattern    the candidate pattern
 * @param position   walk position within the pattern
 * @param candidates completion items for the cursor's slot, filtered by active prefix
 * @param signature  rendered shape with the cursor segment highlighted
 * @param score      editor rank score (0..1)
 */
public record PlanEntry(@NotNull Pattern pattern, @NotNull Position position,
                        @NotNull List<CompletionItem> candidates, @NotNull SignatureItem signature,
                        double score) {
}
