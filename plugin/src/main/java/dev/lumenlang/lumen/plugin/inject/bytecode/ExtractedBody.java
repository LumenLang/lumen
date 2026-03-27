package dev.lumenlang.lumen.plugin.inject.bytecode;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.List;

/**
 * Holds extracted bytecode instructions along with metadata about fake method calls
 * that need to be replaced with real bindings.
 *
 * @param instructions     the raw ASM instruction list extracted from the body method
 * @param fakeBindings     ordered list of fake method calls found
 * @param tryCatchBlocks   the exception table from the original method
 * @param localVariables   the local variable table from the original method
 * @param sourceClass      the internal name of the class the bytecode was extracted from
 * @param sourceMethodName the name of the method the bytecode was extracted from
 * @param returnDescriptor the return type descriptor of the extracted method (e.g. "V" for void)
 * @param maxStack         the max stack depth of the original method
 * @param maxLocals        the max local variable count of the original method
 */
public record ExtractedBody(@NotNull InsnList instructions, @NotNull List<FakeBinding> fakeBindings,
                            @NotNull List<TryCatchBlockNode> tryCatchBlocks,
                            @NotNull List<LocalVariableNode> localVariables, @NotNull String sourceClass,
                            @NotNull String sourceMethodName, @NotNull String returnDescriptor, int maxStack,
                            int maxLocals) {

    /**
     * A detected call to a Fakes method that will be replaced with a real binding parameter load.
     *
     * @param bindingName      the binding name passed to the fake method (e.g. "who")
     * @param fakeMethodName   the fake method that was called (e.g. "fake", "fakeInt")
     * @param returnDescriptor the return type descriptor of the fake method
     */
    public record FakeBinding(@NotNull String bindingName, @NotNull String fakeMethodName,
                              @NotNull String returnDescriptor) {
    }
}
