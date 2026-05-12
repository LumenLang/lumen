package dev.lumenlang.lumen.pipeline.language.simulator.prefilter.result;

import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat per-literal view of a {@link Pattern}'s structure, with each entry carrying every form
 * the literal accepts, its pattern-part index, and whether it sits inside an optional group.
 *
 * @param forms     accepted literal forms, primary form first
 * @param partIndex index of this literal within the pattern's parts
 * @param optional  {@code true} when the literal sits inside a non-required group
 */
public record LiteralInfo(@NotNull List<String> forms, int partIndex, boolean optional) {

    /**
     * Flattens {@code pattern} into a list of {@link LiteralInfo}, walking required groups by
     * their first alternative and marking literals inside optional groups.
     */
    public static @NotNull List<LiteralInfo> extract(@NotNull Pattern pattern) {
        List<LiteralInfo> result = new ArrayList<>();
        extractFromParts(pattern.parts(), result, false);
        return result;
    }

    private static void extractFromParts(@NotNull List<PatternPart> parts, @NotNull List<LiteralInfo> result, boolean parentOptional) {
        int partIndex = result.size();
        for (PatternPart part : parts) {
            if (part instanceof PatternPart.Literal lit) {
                result.add(new LiteralInfo(List.of(lit.text()), partIndex++, parentOptional));
            } else if (part instanceof PatternPart.FlexLiteral flex) {
                result.add(new LiteralInfo(flex.forms(), partIndex++, parentOptional));
            } else if (part instanceof PatternPart.Group group) {
                if (!group.alternatives().isEmpty()) {
                    extractFromParts(group.alternatives().get(0), result, !group.required() || parentOptional);
                }
                partIndex++;
            } else {
                partIndex++;
            }
        }
    }

    public @NotNull String primaryForm() {
        return forms.get(0);
    }
}
