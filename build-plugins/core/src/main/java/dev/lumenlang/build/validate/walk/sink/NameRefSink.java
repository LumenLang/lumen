package dev.lumenlang.build.validate.walk.sink;

import dev.lumenlang.build.source.phase.Phase;
import dev.lumenlang.build.validate.walk.AstWalker;
import org.jetbrains.annotations.NotNull;

/**
 * Receives every free name reference encountered by {@link AstWalker}. Free
 * means the name is not declared by any enclosing scope at the reference
 * site.
 */
@FunctionalInterface
public interface NameRefSink {

    void accept(@NotNull String name, int line, @NotNull Phase phase);
}
