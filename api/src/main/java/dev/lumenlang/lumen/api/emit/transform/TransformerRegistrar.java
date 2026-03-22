package dev.lumenlang.lumen.api.emit.transform;

import org.jetbrains.annotations.NotNull;

/**
 * Registrar for code transformers.
 *
 * <p>Addons register their transformers here so the pipeline can run them
 * after code generation. Each transformer declares the tags it owns through
 * {@link CodeTransformer#tags()} and can modify only those lines.
 *
 * @see CodeTransformer
 */
public interface TransformerRegistrar {

    /**
     * Registers a code transformer.
     *
     * @param transformer the transformer to register
     */
    void register(@NotNull CodeTransformer transformer);

    /**
     * Removes any registered transformer whose {@link CodeTransformer#tags()}
     * list contains {@code tag}.
     *
     * <p>Has no effect if no matching transformer is found.
     *
     * @param tag the tag to match
     */
    void unregister(@NotNull String tag);
}
