package dev.lumenlang.lumen.pipeline.language.pattern;

import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.BlockVarInfo;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Associates a compiled {@link Pattern} with the {@link BlockHandler} that should be invoked
 * when the pattern is matched against a block header during code generation.
 *
 * @param pattern   the compiled pattern to match against block header token lists
 * @param handler   the handler whose {@code begin}/{@code end} methods bracket the child nodes
 * @param meta      documentation metadata for this pattern
 * @param variables the variables this block provides to child statements
 * @see PatternRegistry#block(String, BlockHandler)
 * @see RegisteredBlockMatch
 */
public record RegisteredBlock(@NotNull Pattern pattern, @NotNull BlockHandler handler,
                              @NotNull PatternMeta meta,
                              @NotNull List<BlockVarInfo> variables) {

    public RegisteredBlock(@NotNull Pattern pattern, @NotNull BlockHandler handler) {
        this(pattern, handler, PatternMeta.EMPTY, List.of());
    }
}
