package dev.lumenlang.build.validate.scope;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Scope stack for lexical name lookups during AST traversal. The bottom frame
 * holds method parameters; later frames hold blocks, lambdas, for-loops, etc.
 */
public final class ScopeStack {

    private final Deque<Scope> stack = new ArrayDeque<>();

    public ScopeStack() {
        stack.push(new Scope());
    }

    public void push() {
        stack.push(new Scope());
    }

    public void pop() {
        if (stack.size() <= 1) {
            throw new IllegalStateException("Cannot pop the root scope");
        }
        stack.pop();
    }

    public void declare(@NotNull String name) {
        stack.peek().declare(name);
    }

    /**
     * True when {@code name} is bound by any active frame.
     */
    public boolean shadows(@NotNull String name) {
        for (Scope s : stack) {
            if (s.contains(name)) return true;
        }
        return false;
    }
}
