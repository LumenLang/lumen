---
description: "The context every handler receives for parameter access, output, and compile-time state."
---

# Handler Context

Every handler (statement, block, condition, expression, loop) receives a `HandlerContext`. It is the one object you use
to read matched parameters, emit Java code, and reach into the surrounding compile-time state.

Handlers run at compile time. The values on `ctx` describe what will exist at runtime, not what exists now.

## Accessing Parameters

Four methods read the placeholders in the pattern:

- `ctx.java(name)` returns the Java source expression for the parameter
- `ctx.value(name)` returns whatever the type binding's `parse` produced
- `ctx.tokens(name)` returns the original tokens that were consumed
- `ctx.varHandle(name)` / `ctx.requireVarHandle(name)` cast the parsed value to a `VarHandle` when the parameter is a
  variable reference

```java
api.patterns().

statement("heal %who:PLAYER%",
          ctx ->ctx.

out().

line(ctx.java("who") +".setHealth(20);"));
```

Each method has a positional overload that takes an index (left-to-right) if you prefer.

### Choice Groups

For required choice groups like `(heal|hurt)`, `ctx.choice(i)` returns the matched alternative:

```java
api.patterns().

statement("(heal|hurt) %who:PLAYER%",ctx ->{
String verb = ctx.choice(0);
    if("heal".

equals(verb)){
        ctx.

out().

line(ctx.java("who") +".setHealth(20);");
        }else{
        ctx.

out().

line(ctx.java("who") +".damage(10);");
        }
        });
```

## Emitting Code

`ctx.out()` is the Java output builder. `line(code)` appends a line. `taggedLine(tag, code)` attaches a tag
for [code transformers](../07-transformers/01-transformers.md).

`ctx.codegen()` is class-level: imports, fields, interfaces, and the generated class name. Use it whenever the code
you're emitting depends on something outside the current method.

## Variables

`ctx.env()` is the compile-time symbol table. It hides the scope stack behind a small surface:

```java
VarHandle player = ctx.env().lookupVar("player");
ctx.

env().

defineVar("target",MinecraftTypes.PLAYER, "target_var");
```

A `VarHandle` carries the Java name, the `LumenType`, and any metadata the defining handler attached.
`lookupVarByType(type)` finds the first in-scope variable of a given type, which is handy for patterns that implicitly
operate on the current `player` or `entity`.

## Block Position

`ctx.block()` describes where the current block sits in the AST. Use it for structural checks like "`else` must follow
`if`":

```java
if(!ctx.block().

prevHeadEquals("if")){
        throw new

RuntimeException("'else' must follow 'if'");
}
```

It also exposes a small per-block env (`putEnv`/`getEnv`, plus walks up the parent chain) for sharing state between a
block's `begin` and `end`, or between parent and child blocks.
