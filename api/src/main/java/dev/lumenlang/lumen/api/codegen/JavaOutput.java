package dev.lumenlang.lumen.api.codegen;

import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for emitting Java source lines during code generation.
 *
 * <p>Statement and block handlers use this to append Java code to the class being generated.
 *
 * @see StatementHandler
 * @see BlockHandler
 */
public interface JavaOutput {

    /**
     * Appends a line of Java source code.
     *
     * @param code the Java source line to append
     */
    void line(@NotNull String code);

    /**
     * Returns the current line number in the output.
     *
     * @return the 0-based line index
     */
    int lineNum();

    /**
     * Inserts a line of Java source code at the specified index.
     *
     * <p>Existing lines from that index onward are shifted down.
     *
     * @param index the 0-based index at which to insert the line
     * @param code  the Java source line to insert
     */
    void insertLine(int index, @NotNull String code);
}
