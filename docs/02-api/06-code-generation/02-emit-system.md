---
description: "How to register custom emit handlers that run before pattern matching."
---

# Emit System

The emit system lets addons hook into the code generation pipeline at a lower level than patterns. Emit handlers run before pattern matching, giving them first priority over how statements and blocks are processed.

All of Lumen's built in language features (variable declarations, global variables, stored variables, config blocks, data blocks) are implemented through this same system. Addons have the same capabilities as the core language.

Emit handlers are registered through `api.emitters()`, which returns an `EmitRegistrar`.

## Statement Form Handlers

A `StatementFormHandler` is tried before pattern matching for every statement. If it returns `true`, the statement is considered handled and no further matching is attempted.

```java
api.emitters().statementForm((tokens, emitCtx) -> {
    if (tokens.size() >= 2 && tokens.get(0).text().equals("debug")) {
        String message = tokens.stream()
            .skip(1)
            .map(ScriptToken::text)
            .collect(Collectors.joining(" "));
        emitCtx.out().line(
            "System.out.println(\"[DEBUG] \" + "
                + emitCtx.env().expandPlaceholders(message) + ");"
        );
        return true;
    }
    return false;
});
```

The handler receives:
- `tokens`: the list of `ScriptToken` objects from the current line
- `emitCtx`: an `EmitContext` that provides access to `env()`, `codegen()`, `out()`, and `resolveExpression()`

Each `ScriptToken` has `text()` for the token text, `tokenType()` for the kind (`IDENT`, `NUMBER`, `STRING`, `SYMBOL`), and `line()`, `start()`, `end()` for source location.

## Block Form Handlers

A `BlockFormHandler` is tried before block pattern matching. It has two methods: `matches()` checks if the block header matches, and `handle()` processes it.

```java
api.emitters().blockForm(new BlockFormHandler() {
    @Override
    public boolean matches(@NotNull List<? extends ScriptToken> tokens) {
        return !tokens.isEmpty() && tokens.get(0).text().equals("setup");
    }

    @Override
    public void handle(@NotNull List<? extends ScriptToken> tokens,
                       @NotNull List<? extends ScriptLine> children,
                       @NotNull EmitContext ctx) {
        ctx.out().line("public void setup() {");
        for (ScriptLine child : children) {
            ctx.out().line("// " + child.raw());
        }
        ctx.out().line("}");
    }
});
```

The `handle` method receives:
- `tokens`: the block header tokens
- `children`: the child lines inside the block, each as a `ScriptLine` with `lineNumber()`, `raw()`, and `tokens()` methods
- `ctx`: the emit context

## Block Enter Hooks

A `BlockEnterHook` runs after every pattern matched block's `begin()` method and before its children are processed. This lets you inject code at the start of any block.

```java
api.emitters().blockEnterHook(emitCtx -> {
    emitCtx.out().line(
        "// Block started at line " + emitCtx.line()
    );
});
```

Block enter hooks are called for all pattern matched blocks, not just specific ones. To filter, check `raw()` or other context from `EmitContext`.

## EmitContext

`EmitContext` is passed to all emit handlers. It provides `env()` for the compile time symbol table, `codegen()` for class level metadata, `out()` for emitting Java lines, and `resolveExpression(tokens)` for resolving a token list into a Java expression string. It also has `line()` and `raw()` for source location.

## When to Use Emit Handlers

Use statement and block form handlers when you need syntax that does not fit the pattern system. For example:

- Variable declarations with special behavior
- Block types with custom child processing
- Syntax that requires inspecting raw tokens rather than typed placeholders

For most cases, regular patterns are simpler. Emit handlers are the extension point for when you need full control over the compilation of specific syntax forms.
