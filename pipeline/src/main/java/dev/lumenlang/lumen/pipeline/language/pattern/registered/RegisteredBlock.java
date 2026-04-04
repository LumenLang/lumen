package dev.lumenlang.lumen.pipeline.language.pattern.registered;

import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.BlockVarInfo;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Associates a compiled {@link Pattern} with the {@link BlockHandler} that should be invoked
 * when the pattern is matched against a block header during code generation.
 *
 * @param pattern           the compiled pattern to match against block header token lists
 * @param handler           the handler whose {@code begin}/{@code end} methods bracket the child nodes
 * @param meta              documentation metadata for this pattern
 * @param variables         the variables this block provides to child statements
 * @param supportsRootLevel whether this block can be used at the root level of a script
 * @param supportsBlock     whether this block can be used inside another block
 */
public record RegisteredBlock(@NotNull Pattern pattern, @NotNull BlockHandler handler,
                              @NotNull PatternMeta meta,
                              @NotNull List<BlockVarInfo> variables,
                              boolean supportsRootLevel,
                              boolean supportsBlock) {

    public RegisteredBlock(@NotNull Pattern pattern, @NotNull BlockHandler handler) {
        this(pattern, handler, PatternMeta.EMPTY, List.of(), false, true);
    }
}
