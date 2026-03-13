package dev.lumenlang.lumen.api.binder;

import org.jetbrains.annotations.NotNull;

/**
 * Registrar for custom script annotation binders.
 *
 * <p>Binders registered here are invoked automatically whenever a compiled script
 * class is loaded or unloaded. This allows addons to define their own annotations
 * and binding logic without modifying the core binding pipeline.
 *
 * @see ScriptAnnotationBinder
 */
public interface ScriptBinderRegistrar {

    /**
     * Registers a script annotation binder.
     *
     * @param binder the binder to register
     */
    void register(@NotNull ScriptAnnotationBinder binder);
}
