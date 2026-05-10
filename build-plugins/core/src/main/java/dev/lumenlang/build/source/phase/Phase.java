package dev.lumenlang.build.source.phase;

/**
 * Which side of a handler body a section belongs to.
 */
public enum Phase {

    /**
     * Runs at script parse time. Has access to {@code HandlerContext}; cannot
     * reference {@code @Inject} parameters.
     */
    COMPILE,

    /**
     * Runs at script execution time. Has access to {@code @Inject} parameters;
     * cannot reference {@code HandlerContext}. Default when no markers are
     * present.
     */
    RUNTIME
}
