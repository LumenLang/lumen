package dev.lumenlang.lumen.api.binder;

/**
 * Reason a script instance is being unbound.
 */
public enum BindingMode {

    /**
     * Full teardown. Binders release every resource tied to the instance.
     */
    UNLOAD,

    /**
     * Teardown ahead of an incoming new version. Binders may preserve state
     * that the new instance can resume.
     */
    RELOAD
}
