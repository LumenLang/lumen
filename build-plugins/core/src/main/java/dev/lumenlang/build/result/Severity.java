package dev.lumenlang.build.result;

/**
 * Diagnostic severity. Build shims map errors to build failure and warnings
 * to the build system's warning channel.
 */
public enum Severity {
    ERROR,
    WARNING
}
