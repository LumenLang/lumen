package dev.lumenlang.lumen.pipeline.language.suggestor.render;

import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.pattern.Placeholder;
import dev.lumenlang.lumen.pipeline.language.simulator.result.Position;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.SegmentKind;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.ShapeSegment;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.SignatureItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a pattern's parts into a {@link SignatureItem} with each segment marked matched
 * (already consumed by input) or pending (yet to type).
 */
public final class ShapeRenderer {

    private ShapeRenderer() {
    }

    /**
     * Renders {@code position}'s pattern into a signature. The segment containing {@code atPart}
     * (or the next pending one when the pattern is complete) is marked active.
     */
    public static @NotNull SignatureItem render(@NotNull Position position) {
        Pattern pattern = position.pattern();
        List<PatternPart> parts = pattern.parts();
        List<ShapeSegment> segments = new ArrayList<>(parts.size());
        int activeIdx = -1;
        boolean reachedActive = position.atPart() == null;
        for (int i = 0; i < parts.size(); i++) {
            PatternPart part = parts.get(i);
            if (!reachedActive && part == position.atPart()) {
                reachedActive = true;
                activeIdx = i;
            }
            segments.add(toSegment(part, !reachedActive));
        }
        StringBuilder rendered = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) rendered.append(' ');
            rendered.append(segments.get(i).text());
        }
        return new SignatureItem(rendered.toString(), List.copyOf(segments), activeIdx);
    }

    private static @NotNull ShapeSegment toSegment(@NotNull PatternPart part, boolean matched) {
        if (part instanceof PatternPart.Literal lit) {
            return new ShapeSegment(matched ? SegmentKind.LITERAL_MATCHED : SegmentKind.LITERAL_PENDING, lit.text(), null);
        }
        if (part instanceof PatternPart.FlexLiteral flex) {
            String text = flex.forms().isEmpty() ? "" : flex.forms().get(0);
            return new ShapeSegment(matched ? SegmentKind.LITERAL_MATCHED : SegmentKind.LITERAL_PENDING, text, null);
        }
        if (part instanceof PatternPart.PlaceholderPart pp) {
            Placeholder ph = pp.ph();
            String text = "%" + ph.name() + ":" + ph.typeId() + "%";
            return new ShapeSegment(matched ? SegmentKind.PLACEHOLDER_FILLED : SegmentKind.PLACEHOLDER_PENDING, text, ph.typeId());
        }
        if (part instanceof PatternPart.Group group) {
            String text = renderGroup(group);
            SegmentKind kind = matched ? SegmentKind.LITERAL_MATCHED : SegmentKind.LITERAL_PENDING;
            return new ShapeSegment(kind, text, null);
        }
        return new ShapeSegment(SegmentKind.LITERAL_PENDING, "?", null);
    }

    private static @NotNull String renderGroup(@NotNull PatternPart.Group group) {
        StringBuilder sb = new StringBuilder(group.required() ? "(" : "[");
        for (int a = 0; a < group.alternatives().size(); a++) {
            if (a > 0) sb.append('|');
            List<PatternPart> alt = group.alternatives().get(a);
            for (int p = 0; p < alt.size(); p++) {
                if (p > 0) sb.append(' ');
                sb.append(altText(alt.get(p)));
            }
        }
        sb.append(group.required() ? ")" : "]");
        return sb.toString();
    }

    private static @NotNull String altText(@NotNull PatternPart part) {
        if (part instanceof PatternPart.Literal lit) return lit.text();
        if (part instanceof PatternPart.FlexLiteral flex) return flex.forms().isEmpty() ? "" : flex.forms().get(0);
        if (part instanceof PatternPart.PlaceholderPart pp) return "%" + pp.ph().name() + ":" + pp.ph().typeId() + "%";
        if (part instanceof PatternPart.Group inner) return renderGroup(inner);
        return "?";
    }
}
