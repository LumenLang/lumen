package dev.lumenlang.lumen.api.scanner;

import dev.lumenlang.lumen.api.annotations.Registration;
import org.jetbrains.annotations.NotNull;

/**
 * Scans a package and its sub-packages for classes annotated with {@link Registration},
 * instantiates them, and invokes their registration methods.
 *
 * <p>Classes are sorted by {@link Registration#order()} before processing. Lower values
 * are processed first.
 */
public final class RegistrationScanner {

    private static Backend backend;

    private RegistrationScanner() {
    }

    /**
     * Scans the given base package for registration classes and invokes their call methods.
     *
     * @param basePackage the base package to scan (e.g. {@code "dev.lumenlang.lumen.defaults"})
     * @throws IllegalStateException if the scanner backend has not been initialized
     */
    public static void scan(@NotNull String basePackage) {
        if (backend == null) {
            throw new IllegalStateException("RegistrationScanner has not been initialized");
        }
        backend.scan(basePackage);
    }

    /**
     * Sets the internal backend implementation used for scanning.
     * <b>Not part of the public API contract. Do not call from addons.</b>
     *
     * @param impl the backend implementation
     */
    public static void init(@NotNull Backend impl) {
        backend = impl;
    }

    /**
     * Clears the internal backend reference.
     * <b>Not part of the public API contract. Do not call from addons.</b>
     */
    public static void teardown() {
        backend = null;
    }

    /**
     * Internal interface for the scanning implementation.
     * <b>Not part of the public API contract.</b>
     */
    @FunctionalInterface
    public interface Backend {

        /**
         * Scans the given base package for registration classes and processes them.
         *
         * @param basePackage the base package to scan
         */
        void scan(@NotNull String basePackage);
    }
}
