package dev.lumenlang.build.source.phase;

import net.vansencool.vanta.parser.ast.comment.Comment;
import net.vansencool.vanta.parser.ast.comment.CommentTable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Pulls {@code // lumen:compile} and {@code // lumen:runtime} markers out of
 * Vanta's {@link CommentTable}.
 */
public final class PhaseMarkerScanner {

    private PhaseMarkerScanner() {
    }

    public static @NotNull List<PhaseMarker> scan(@NotNull CommentTable comments) {
        List<PhaseMarker> out = new ArrayList<>();
        for (Comment c : comments.all()) {
            if (c.kind() != Comment.Kind.LINE) continue;
            String body = c.text().strip();
            if ("lumen:compile".equals(body)) out.add(new PhaseMarker(Phase.COMPILE, c.startLine()));
            else if ("lumen:runtime".equals(body)) out.add(new PhaseMarker(Phase.RUNTIME, c.startLine()));
        }
        return out;
    }
}
