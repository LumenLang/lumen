---
description: "Register statement, block, condition, expression, and loop patterns."
---

# Patterns

Patterns define the surface syntax of the language. Every statement, block, condition, expression source, and loop
source is a registered pattern. Register them through `api.patterns()`.

Handlers run at compile time and emit Java source that is compiled later. Values you receive are compile-time artefacts,
not runtime objects: a `PLAYER` parameter gives you the Java variable name that will hold a player at runtime.

## Pattern Syntax

- `%name:TYPE%` placeholder with an explicit type binding
- `%name%` placeholder with the implicit `EXPR` type (captures remaining tokens until the next literal)
- `%name:NULLABLE_TYPE%` accepts nullable values for the given type (
  see [Nullable Placeholders](#nullable-placeholders))
- `literal` fixed text, matched case-insensitively
- `(a|b|c)` required choice, exactly one alternative
- `[text]` optional group
- `[a|b|c]` optional choice
- `word[suffix]` and `[prefix]word` for optional suffixes and prefixes on a literal (`info[rmation]`, `[un]hide`)

```
give %who:PLAYER% %item:MATERIAL% %amt:INT%
(heal|restore) %who:PLAYER%
send %msg% [to %target:PLAYER%]
```

## Handler Context

All handlers receive a single `HandlerContext`. It exposes the matched parameters, the type environment, the Java
output, and code generation utilities. Use `ctx.java(name)` for the Java expression of a parameter,
`ctx.out().line(code)` to emit a line, and `ctx.choice(i)` to read the matched alternative of the `i`-th required choice
group.

See [Handler Context](../06-handler-context/01-handler-context.md) for the full surface of `HandlerContext`.

## Statements

```java
api.patterns().statement("heal %who:PLAYER%",
    ctx -> ctx.out().line(ctx.java("who") + ".setHealth(20);"));
```

Builder form adds documentation metadata:

```java
api.patterns().statement(b -> b
    .pattern("heal %who:PLAYER%")
    .description("Fully heals a player.")
    .example("heal player")
    .since("1.0.0")
    .category(Categories.PLAYER)
    .handler(ctx -> ctx.out().line(ctx.java("who") + ".setHealth(20);")));
```

Register several pattern strings against the same handler by passing a `List<String>`.

## Blocks

Blocks match a header ending in `:` and wrap their children. A `BlockHandler` emits code before (`begin`) and after (
`end`) the children are processed.

```java
api.patterns().block("repeat %n:INT% times", new BlockHandler() {
    @Override public void begin(@NotNull HandlerContext ctx) {
        ctx.out().line("for (int i = 0; i < " + ctx.java("n") + "; i++) {");
    }
    @Override public void end(@NotNull HandlerContext ctx) {
        ctx.out().line("}");
    }
});
```

### Variables

A block that introduces variables for its children declares them with `addVar(name, type)` for documentation and
tooling. The handler is still responsible for actually emitting the variable declarations.

```java
.addVar("player", MinecraftTypes.PLAYER)
    .withMeta("nullable", true)
    .varDescription("Executor of the command, or null if the console ran it")
.addVar("args", "List<String>")
    .varDescription("Command arguments as a mutable list")
```

### Placement

`supportsRootLevel` and `supportsBlock` control where a block is allowed. Defaults: root-level disabled, nestable
enabled. Set `supportsRootLevel(true)` for blocks like `on join:` that belong at the top level.

## Conditions

A condition handler returns the Java boolean expression that will appear in the emitted `if`:

```java
api.patterns().condition("%p:PLAYER% is swimming",
    ctx -> ctx.requireVarHandle("p").java() + ".isSwimming()");
```

## Expressions

Used in `set x to <pattern>`. The handler returns an `ExpressionResult` with the Java expression and the `LumenType` of
its result so the assigned variable gets the correct type.

```java
api.patterns().expression("get player %name:STRING%",
    ctx -> new ExpressionResult(
        "Bukkit.getPlayer(" + ctx.java("name") + ")",
        MinecraftTypes.PLAYER));
```

## Loops

A loop source handler returns a `LoopResult` with a Java `Iterable` expression and the element type, which becomes the
type of the loop variable.

```java
api.patterns().loop("all players",
    ctx -> new LoopResult("Bukkit.getOnlinePlayers()", MinecraftTypes.PLAYER));
```

## Nullable Placeholders

Prefix a type with `NULLABLE_` in a pattern to accept nullable values:

```
notify %p:NULLABLE_PLAYER% about %event:STRING%
```

At match time, the pattern engine strips the prefix, resolves the underlying binding (`PLAYER`), and wraps the resulting
`LumenType` as nullable so the type checker lets null values through. The handler itself still receives the same parsed
value; the difference is that the type system knows the value might be null and downstream assignments are checked
accordingly.

Pair this with event variables whose expression can legitimately return null, or with expressions typed as nullable,
rather than using it everywhere by default.
