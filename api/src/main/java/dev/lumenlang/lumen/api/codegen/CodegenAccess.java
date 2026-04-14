package dev.lumenlang.lumen.api.codegen;

import dev.lumenlang.lumen.api.handler.StatementHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Provides class-level metadata operations during code generation.
 *
 * <p>Handlers use this to add import statements and to obtain the generated class name.
 *
 * @see StatementHandler
 */
public interface CodegenAccess {

    /**
     * Returns the normalised Java class name for the script being compiled.
     *
     * @return the generated class name (e.g. {@code "MyScript"})
     */
    @NotNull String className();

    /**
     * Adds a fully-qualified class name to the import set for the generated script class.
     *
     * @param fqcn the fully-qualified class name to import
     *             (e.g. {@code "org.bukkit.inventory.ItemStack"})
     */
    void addImport(@NotNull String fqcn);

    /**
     * Adds a class-level field declaration to the generated script class.
     *
     * @param line the full field declaration line
     *             (e.g. {@code "private String name = \"hello\";"})
     */
    void addField(@NotNull String line);

    /**
     * Adds an additional interface that the generated class should implement.
     *
     * <p>The generated class always implements {@code Listener}. This method
     * allows adding extra interfaces (e.g. {@code "Runnable"} or
     * {@code "org.bukkit.event.HandlerList"}).
     *
     * @param fqcn the simple or fully-qualified interface name
     */
    void addInterface(@NotNull String fqcn);

    /**
     * Adds a complete method declaration to the generated script class.
     *
     * <p>The method source is rendered as a top-level member of the class, alongside
     * fields and the main body methods.
     *
     * @param methodSource the full method source code including signature and body
     */
    void addMethod(@NotNull String methodSource);

    /**
     * Returns the raw script file name.
     *
     * @return the original script file name (e.g. {@code "hello.luma"})
     */
    @NotNull String scriptName();

    /**
     * Returns a unique method identifier for the current script compilation.
     *
     * @return a unique integer for method name generation
     */
    int nextMethodId();
}
