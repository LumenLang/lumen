---
description: "How to register custom emit handlers that run before pattern matching."
---

# Emit System

The emit system lets addons hook into the code generation pipeline at a lower level than patterns. Emit handlers run
before pattern matching, giving them first priority over how blocks are processed.

Emit handlers are registered through `api.emitters()`, which returns an `EmitRegistrar`.

## Block Form Handlers

A `BlockFormHandler` is tried before block pattern matching. It has two methods: `matches()` checks if the block header
matches, and `handle()` processes it.

```java
api.emitters().

blockForm(new BlockFormHandler() {
    @Override
    public boolean matches (@NotNull List < ? extends ScriptToken > tokens){
        return !tokens.isEmpty() && tokens.get(0).text().equals("setup");
    }

    @Override
    public void handle (@NotNull List < ? extends ScriptToken > tokens,
            @NotNull List < ? extends ScriptLine > children,
            @NotNull EmitContext ctx){
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
- `children`: the child lines inside the block, each as a `ScriptLine` with `lineNumber()`, `raw()`, and `tokens()`
  methods
- `ctx`: the emit context

## Block Enter Hooks

A `BlockEnterHook` runs after every pattern matched block's `begin()` method and before its children are processed. This
lets you inject code at the start of any block.

Block enter hooks are called for all pattern matched blocks, not just specific ones. To filter, check `raw()` or other
context from `EmitContext`.

## EmitContext

`EmitContext` is passed to all emit handlers. It provides `env()` for the compile time symbol table, `codegen()` for
class level metadata, `out()` for emitting Java lines, and `resolveExpression(tokens)` for resolving a token list into a
Java expression string. It also has `line()` and `raw()` for source location.

## When to Use Emit Handlers

Use block form handlers when you need block syntax that does not fit the pattern system. For example:

- Block types with custom child processing
- Syntax that requires inspecting raw tokens rather than typed placeholders

For most cases, regular patterns are simpler. Emit handlers are the extension point for when you need full control over
the compilation of specific block syntax forms.
