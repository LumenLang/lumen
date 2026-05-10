package dev.lumenlang.build.validate.walk;

import dev.lumenlang.build.source.phase.Phase;
import dev.lumenlang.build.source.phase.PhaseMarker;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Resolves which {@link Phase} a given source line belongs to, given the
 * ordered list of markers captured from the file. Lines before any marker
 * are runtime by default.
 */
public final class PhaseLookup {

    private final @NotNull List<PhaseMarker> markers;

    public PhaseLookup(@NotNull List<PhaseMarker> markers) {
        this.markers = markers;
    }

    public @NotNull Phase phaseAt(int line) {
        Phase current = Phase.RUNTIME;
        for (PhaseMarker m : markers) {
            if (m.line() > line) break;
            current = m.phase();
        }
        return current;
    }
}
