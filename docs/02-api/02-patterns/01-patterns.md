---
description: "How to register custom statement, block, condition, expression, and loop patterns."
---

# Patterns

Patterns are the core of Lumen's syntax system. Every statement, block, condition, expression, and loop source in the language is defined as a pattern. Addons register patterns using the `PatternRegistrar`, which is accessed via `api.patterns()`.

## How Patterns Work

Lumen is a compiled scripting language. When a script is loaded, Lumen compiles it into a Java class that runs on the server. Pattern handlers do not execute script logic themselves. Instead, they **emit Java source code** that will be compiled and executed later.

Because handlers emit Java code rather than running logic directly, the values you work with in a handler are **compile time** values. A `PLAYER` parameter does not give you an actual `Player` object. It gives you the Java variable name (like `"player"`) that will refer to one at runtime.

## Pattern Syntax

A pattern string describes what a line of script should look like:

- `%name:TYPE%` is a placeholder with an explicit type. It consumes tokens and parses them using the named type binding.
- `%name%` is a placeholder with the implicit `EXPR` type. It consumes all remaining tokens until the next literal.
- `literal` is fixed text that must appear in the script. Matching is case insensitive.
- `(a|b|c)` is a required choice group. Exactly one of the alternatives must match.
- `[text]` is an optional group. The text may be present or absent.
- `[a|b|c]` is an optional choice group. Zero or one of the alternatives matches.
- `word[suffix]` is an optional suffix on a literal, for example `info[rmation]` matches both `info` and `information`.
- `[prefix]word` is an optional prefix on a literal, for example `[un]hide` matches both `hide` and `unhide`.

### Example

```
give %who:PLAYER% %item:MATERIAL% %amt:INT%
```

This pattern matches a line like `give player diamond 64`. The `%who:PLAYER%` part consumes one token and parses it as a `PLAYER` type binding, `%item:MATERIAL%` consumes one token as a material, and `%amt:INT%` consumes one token as an integer.

### Choice and Optional Groups

```
(heal|restore) %who:PLAYER%
```

This matches both `heal player` and `restore player`. The handler can check which alternative was matched using `ctx.choice(0)`.

```
send %msg% [to %target:PLAYER%]
```

This matches both `send "hello" to player` and `send "hello"` (where the target is optional).

## Statements

Statement patterns match a single line of script and emit Java code.

### Simple Registration

```java
api.patterns().statement(
    "heal %who:PLAYER%",
    (line, ctx, out) -> out.line(ctx.java("who") + ".setHealth(20);")
);
```

When a script contains `heal player`, the handler runs and emits `player.setHealth(20);` into the generated Java class.

### Builder Registration

The builder approach adds documentation metadata that is used for reference documentation:

```java
api.patterns().statement(b -> b
    .by("MyAddon")
    .pattern("heal %who:PLAYER%")
    .description("Fully heals a player.")
    .example("heal player")
    .since("1.0.0")
    .category(Categories.PLAYER)
    .handler((line, ctx, out) -> {
        out.line(ctx.java("who") + ".setHealth(20);");
    })
);
```

### Multiple Patterns

You can register multiple pattern strings that all use the same handler:

```java
api.patterns().statement(
    List.of("heal %who:PLAYER%", "restore %who:PLAYER%"),
    (line, ctx, out) -> out.line(ctx.java("who") + ".setHealth(20);")
);
```

### Adding Imports

If the generated Java code uses classes that need importing, use `ctx.codegen().addImport()`:

```java
api.patterns().statement(
    "give %who:PLAYER% %item:MATERIAL% %amt:INT%",
    (line, ctx, out) -> {
        ctx.codegen().addImport("org.bukkit.inventory.ItemStack");
        out.line(
            ctx.java("who") + ".getInventory().addItem(new ItemStack("
                + ctx.java("item") + ", " + ctx.java("amt") + "));"
        );
    }
);
```

## Blocks

Block patterns match lines that end with `:` and wrap around child statements. A `BlockHandler` has two methods: `begin()` runs before the children, and `end()` runs after.

```java
api.patterns().block(
    "repeat %n:INT% times",
    new BlockHandler() {
        @Override
        public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
            out.line("for (int i = 0; i < " + ctx.java("n") + "; i++) {");
        }

        @Override
        public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
            out.line("}");
        }
    }
);
```

In a script:

```luma
repeat 5 times:
    message player "Hello!"
```

This generates a for loop wrapping the child statements.

Builder registration works the same way:

```java
api.patterns().block(b -> b
    .by("MyAddon")
    .pattern("repeat %n:INT% times")
    .description("Repeats the enclosed block a given number of times.")
    .example("repeat 5 times:")
    .since("1.0.0")
    .category(Categories.CONTROL_FLOW)
    .handler(new BlockHandler() {
        @Override
        public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
            out.line("for (int i = 0; i < " + ctx.java("n") + "; i++) {");
        }

        @Override
        public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
            out.line("}");
        }
    })
);
```

### Block Variables

Blocks can declare which variables they provide to child statements using `addVar()`. This is purely for documentation: the actual variable emission is still handled by the `BlockHandler`. These declarations tell documentation generators what variables are available and their types.

There are two overloads of `addVar()`:

- `addVar(name, type)` takes a variable name and a human readable type string (e.g. `"Player"`, `"int"`). You can use `Types.STRING`, `Types.INT`, and other primitive constants from `Types` as the type string.
- `addVar(name, refType)` takes a variable name and a `RefTypeHandle` (e.g. `Types.PLAYER`, `Types.INVENTORY`). The human readable type string is derived automatically from the Java class simple name, and the ref type is stored for tooling.

```java
api.patterns().block(b -> b
    .by("MyAddon")
    .pattern("command %name:EXPR%")
    .description("Declares a custom command.")
    .example("command hello:")
    .since("1.0.0")
    .category(Categories.COMMAND)
    .supportsRootLevel(true)
    .supportsBlock(false)
    .addVar("player", Types.PLAYER)
        .withMeta("nullable", true)
        .varDescription("The player who executed the command, or null if the console ran it")
    .addVar("sender", Types.SENDER)
        .varDescription("The command sender (player or console)")
    .addVar("world", Types.WORLD)
        .withMeta("nullable", true)
        .varDescription("The world the player is in, or null if the console ran it")
    .addVar("args", "List<String>")
        .varDescription("The command arguments as a mutable list of strings")
    .handler(new BlockHandler() {
        @Override
        public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
            // emit command method and variables
        }

        @Override
        public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
            out.line("}");
        }
    })
);
```

After `addVar()`, you can chain:

- `.withMeta(key, value)` to attach metadata (such as `"nullable"` set to `true`)
- `.varDescription(description)` to set a human readable description

These are recorded in `BlockVarInfo` objects and stored in the `RegisteredBlock`. The documentation dumper serializes them into the JSON output under a `"variables"` array for each block entry.

### Root Level and Block Nesting

The builder provides two flags that control where a block can be used:

- `.supportsRootLevel(boolean)` controls whether the block can appear at the top level of a script, outside any other block. Defaults to `false`.
- `.supportsBlock(boolean)` controls whether the block can be nested inside another block. Defaults to `true`.

For example, blocks like `on join:` are root level only:

```java
api.patterns().block(b -> b
    .by("MyAddon")
    .pattern("click in %inv:EXPR%")
    .supportsRootLevel(true)
    .supportsBlock(false)
    // ...
);
```

Most control-flow blocks, such as typical `repeat` loops, are block level only (they can only appear inside another block). Blocks that need to be allowed at the root can opt in by setting `.supportsRootLevel(true)`. For a block-only control-flow block, you would configure it like this:

```java
api.patterns().block(b -> b
    .by("MyAddon")
    .pattern("repeat %n:INT% times")
    .supportsRootLevel(false)
    .supportsBlock(true)
    // ...
);
```

Both flags default to their most common values (`supportsRootLevel = false`, `supportsBlock = true`), so most blocks only need to set these when they deviate from the defaults.

## Conditions

Condition patterns match boolean expressions used inside `if` blocks. The handler returns a Java boolean expression string.

```java
api.patterns().condition(
    "%p:PLAYER% is swimming",
    (match, env, ctx) -> match.ref("p").java() + ".isSwimming()"
);
```

### ConditionMatch Parameter Access

The `ConditionMatch` parameter provides the same three accessor methods as `BindingAccess`. Condition handlers receive a `ConditionMatch` instead of a `BindingAccess` because conditions return a single Java expression rather than emitting lines through `JavaOutput`.

- `match.ref("name")` returns a `VarHandle` (the compile time variable descriptor with `.java()`, `.type()`, `.meta()`)
- `match.value("name")` returns the raw object from the type binding's `parse()` method
- `match.java("name", ctx, env)` returns a Java source expression string ready to embed in output

The condition handler signature passes `ctx` and `env` separately, so `java()` requires all three arguments. For a full explanation of what each method returns and when to use it, see [Parameter Access Methods](../06-code-generation/01-code-generation#accessing-parameters) in the Code Generation guide.

```java
api.patterns().condition(
    "%p:PLAYER% has health above %n:INT%",
    (match, env, ctx) -> {
        String playerJava = match.java("p", ctx, env);  // "player"
        String numberJava = match.java("n", ctx, env);  // "10"
        return playerJava + ".getHealth() > " + numberJava;
    }
);
```

All three methods also have positional variants (`ref(index)`, `value(index)`, `java(index, ctx, env)`) that access parameters by position instead of by name.

In a script:

```luma
if player is swimming:
    message player "You are swimming!"
```

Builder registration:

```java
api.patterns().condition(b -> b
    .by("MyAddon")
    .pattern("%p:PLAYER% is swimming")
    .description("Checks whether a player is currently swimming.")
    .example("if player is swimming:")
    .since("1.0.0")
    .category(Categories.PLAYER)
    .handler((match, env, ctx) -> {
        return match.ref("p").java() + ".isSwimming()";
    })
);
```

## Expressions

Expression patterns are used in `set x to <pattern>` statements. The handler returns an `ExpressionResult` containing a Java expression and optional type information.

```java
api.patterns().expression(
    "get player %name:STRING%",
    ctx -> new ExpressionResult(
        "Bukkit.getPlayer(" + ctx.java("name") + ")",
        Types.PLAYER.id()
    )
);
```

The first argument of `ExpressionResult` is the Java expression, and the second is the ref type ID so the resulting variable gets type information for placeholder resolution.

In a script:

```luma
set target to get player "Notch"
message target "Hello!"
```

Builder registration:

```java
api.patterns().expression(b -> b
    .by("MyAddon")
    .pattern("get player %name:STRING%")
    .description("Looks up an online player by name.")
    .example("set target to get player \"Notch\"")
    .since("1.0.0")
    .category(Categories.PLAYER)
    .handler(ctx -> new ExpressionResult(
        "Bukkit.getPlayer(" + ctx.java("name") + ")",
        Types.PLAYER.id()
    ))
);
```

## Loops

Loop patterns define what collection a `loop ... in <source>:` block iterates over. The handler returns a `LoopResult` containing a Java iterable expression and the element type.

```java
api.patterns().loop(
    "all players",
    ctx -> new LoopResult("Bukkit.getOnlinePlayers()", "PLAYER")
);
```

The first argument of `LoopResult` is a Java expression that evaluates to an `Iterable`, and the second is the ref type ID for each element so the loop variable gets type information.

In a script:

```luma
loop p in all players:
    message p "Hello!"
```

Builder registration:

```java
api.patterns().loop(b -> b
    .by("MyAddon")
    .pattern("all players")
    .description("Iterates over all online players.")
    .example("loop p in all players:")
    .since("1.0.0")
    .category(Categories.PLAYER)
    .handler(ctx -> new LoopResult("Bukkit.getOnlinePlayers()", "PLAYER"))
);
```
