package dev.lumenlang.build.emit.slice;

import net.vansencool.vanta.parser.ast.statement.Statement;
import org.jetbrains.annotations.NotNull;

/**
 * One runtime statement paired with its source text.
 *
 * @param node the AST node this slice came from, retained for kind checks (return, throw, etc.)
 * @param text the source text of the statement, stripped of leading and trailing whitespace
 */
public record SourceStatement(@NotNull Statement node, @NotNull String text) {
}
