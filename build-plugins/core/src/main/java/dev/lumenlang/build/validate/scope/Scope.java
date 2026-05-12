package dev.lumenlang.build.validate.scope;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * One lexical scope's locally-declared names. The walker pushes a new scope
 * when entering any name-binding form (method body, lambda, for-loop, catch,
 * etc.) and pops it on exit.
 */
public final class Scope {

    private final Set<String> locals = new HashSet<>();

    public void declare(@NotNull String name) {
        locals.add(name);
    }

    public boolean contains(@NotNull String name) {
        return locals.contains(name);
    }
}
