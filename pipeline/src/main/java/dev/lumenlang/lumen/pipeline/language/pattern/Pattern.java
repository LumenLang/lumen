package dev.lumenlang.lumen.pipeline.language.pattern;

import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.compile.PatternCompiler;
import dev.lumenlang.lumen.pipeline.language.match.PatternMatcher;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A compiled representation of a Lumen pattern string.
 *
 * <p>A {@code Pattern} is the result of calling {@link PatternCompiler#compile(String)}
 * on a raw pattern string such as {@code "give %who:PLAYER% %item:MATERIAL% %amt:INT%"}. It holds
 * an ordered list of {@link PatternPart} objects that describe the expected sequence of literal
 * tokens and typed placeholders.
 *
 * @param raw   the original, unmodified pattern string
 * @param parts the ordered list of literal and placeholders parts
 * @see PatternPart
 * @see PatternCompiler#compile(String)
 * @see PatternMatcher#match(List, Pattern, TypeRegistry, TypeEnv)
 */
public record Pattern(@NotNull String raw, @NotNull List<PatternPart> parts) {
}