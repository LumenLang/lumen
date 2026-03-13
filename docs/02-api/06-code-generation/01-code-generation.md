---
description: "How to use BindingAccess, JavaOutput, CodegenAccess, and EnvironmentAccess in pattern handlers."
---

# Code Generation

When a pattern handler runs, it receives several context objects that provide access to matched parameters, the Java output builder, class level metadata, and the compile time symbol table. These are the tools you use to emit Java code from your patterns.

## BindingAccess

`BindingAccess` is the primary context passed to statement and block handlers. It provides access to the values matched by pattern placeholders.

### Accessing Parameters

You can access matched parameters by name or by positional index (left to right in the pattern):

```java
api.patterns().statement(
    "give %who:PLAYER% %item:MATERIAL% %amt:INT%",
    (line, ctx, out) -> {
        String whoJava = ctx.java("who");
        String itemJava = ctx.java("item");
        String amtJava = ctx.java("amt");
        out.line(
            whoJava + ".getInventory().addItem(new ItemStack("
                + itemJava + ", " + amtJava + "));"
        );
    }
);
```

Use `ctx.java(name)` or `ctx.java(index)` to get the Java expression for a parameter. Use `ctx.value(name)` for the raw parsed value, or `ctx.varHandle(name)` to get it as a `VarHandle` directly.

### Choice Groups

When a pattern contains choice groups like `(test|other)`, use `ctx.choice(index)` to check which alternative was matched:

```java
api.patterns().statement(
    "(heal|hurt) %who:PLAYER%",
    (line, ctx, out) -> {
        String choice = ctx.choice(0);
        if ("heal".equals(choice)) {
            out.line(ctx.java("who") + ".setHealth(20);");
        } else {
            out.line(ctx.java("who") + ".damage(10);");
        }
    }
);
```

### Parsing Conditions

For block handlers that need to evaluate a condition, use `ctx.parseCondition(paramName)` to turn condition tokens into a Java boolean expression:

```java
api.patterns().block("if %cond:EXPR%", new BlockHandler() {
    @Override
    public void begin(@NotNull BindingAccess ctx,
                      @NotNull JavaOutput out) {
        out.line("if (" + ctx.parseCondition("cond") + ") {");
    }

    @Override
    public void end(@NotNull BindingAccess ctx,
                    @NotNull JavaOutput out) {
        out.line("}");
    }
});
```

### Related Accessors

From `BindingAccess` you can reach the other context objects:

- `ctx.env()` returns the `EnvironmentAccess` (compile time symbol table)
- `ctx.codegen()` returns the `CodegenAccess` (class level metadata)
- `ctx.block()` returns the `BlockAccess` (block position in the AST)

## JavaOutput

`JavaOutput` is the builder you use to emit lines of Java source code. Call `out.line(code)` to append a line. Use `out.lineNum()` to get the current 0 based line index, which is useful for generating unique names. Use `out.insertLine(index, code)` to insert a line at a specific position.

```java
(line, ctx, out) -> {
    ctx.codegen().addImport("org.bukkit.inventory.ItemStack");
    out.line(
        ctx.java("who") + ".getInventory().addItem(new ItemStack("
            + ctx.java("item") + ", " + ctx.java("amt") + "));"
    );
}
```

## CodegenAccess

`CodegenAccess` provides class level operations. Use it to add imports, fields, and interfaces to the generated Java class:

```java
(line, ctx, out) -> {
    ctx.codegen().addImport("com.example.MyHelper");
    ctx.codegen().addField("private final MyHelper helper = new MyHelper();");
    out.line("helper.doSomething();");
}
```

You can also query the generated class name with `ctx.codegen().className()` or the original script file name with `ctx.codegen().scriptName()`.

## EnvironmentAccess

`EnvironmentAccess` is the compile time symbol table. It tracks variables, their types, and their scopes. Access it through `ctx.env()`.

### Looking Up and Defining Variables

```java
EnvironmentAccess.VarHandle handle = ctx.env().lookupVar("player");
if (handle != null) {
    String javaName = handle.java();
    RefTypeHandle type = handle.type();
}
```

Define a new variable in the current scope:

```java
ctx.env().defineVar("target", Types.PLAYER, "target_var");
```

This registers a variable named `target` that maps to the Java variable `target_var` with the `PLAYER` ref type.

### Looking Up by Type

Find the first variable in scope that matches a specific ref type:

```java
EnvironmentAccess.VarHandle player = ctx.env().lookupVarByType(Types.PLAYER);
```

### Expanding Placeholders

The `expandPlaceholders` method resolves `{variable_property}` syntax in a raw string and returns the corresponding Java expression:

```java
String javaExpr = ctx.env().expandPlaceholders("{player_name}");
```

## BlockAccess

`BlockAccess` provides information about where a block sits in the AST. This is commonly used by blocks like `else` that need to verify the previous sibling was an `if`:

```java
api.patterns().block("else", new BlockHandler() {
    @Override
    public void begin(@NotNull BindingAccess ctx,
                      @NotNull JavaOutput out) {
        if (!ctx.block().prevHeadEquals("if")) {
            throw new RuntimeException("'else' must follow 'if'");
        }
        out.line("else {");
    }

    @Override
    public void end(@NotNull BindingAccess ctx,
                    @NotNull JavaOutput out) {
        out.line("}");
    }
});
```

Use `ctx.block().isRoot()` to check if the block is at the top level of the script, and `ctx.block().raw()` to get the raw text of the block header.
