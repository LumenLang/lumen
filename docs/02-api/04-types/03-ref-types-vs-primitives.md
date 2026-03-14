---
description: "Why Lumen splits types into ref types and primitive types, and how to use each correctly."
---

# Ref Types vs Primitive Types

The `Types` class contains two kinds of constants that look similar but behave differently. This page explains what they are, why they are separate, and how to use them correctly.

## What Are Ref Types?

Ref types are `RefTypeHandle` instances. They represent Bukkit or Java object types that carry identity, meaning they can be stored in global variables, looked up by key, and have placeholder properties attached to them.

```java
Types.PLAYER   // RefTypeHandle, id = "PLAYER", javaType = "org.bukkit.entity.Player"
Types.LOCATION // RefTypeHandle, id = "LOCATION", javaType = "org.bukkit.Location"
Types.ENTITY   // RefTypeHandle, id = "ENTITY", javaType = "org.bukkit.entity.Entity"
```

Check the `Types` class in your IDE for the full list of built in ref types. Addons can register their own:

```java
RefTypeHandle villagerType = api.refTypes().register(
        "VILLAGER", "org.bukkit.entity.Villager");
```

## What Are Primitive Types?

Primitive types are plain `String` constants. They represent Java primitives and `String`, types that do not carry identity and cannot have placeholder properties.

```java
Types.INT     // "int"
Types.DOUBLE  // "double"
Types.STRING  // "String"
Types.BOOLEAN // "boolean"
```

## Why Are They Separate?

Ref types need features that primitives do not:

1. **Global variables and storage.** Ref types support `keyExpression()`, which produces a unique string key for an object instance. This is how global and stored variables can look up the correct value. An `int` or a `String` has no meaningful identity to key on, so it does not need this.

2. **Placeholder properties.** You can register `{player_name}` or `{location_world}` because `PLAYER` and `LOCATION` are ref types with known structures. Primitives like `int` or `boolean` have no properties to access.

3. **Type bindings.** Ref types can have associated `AddonTypeBinding` implementations that control how pattern placeholders like `%who:PLAYER%` consume and parse tokens. Primitives are handled directly by the compiler.

Because of these differences, ref types are full `RefTypeHandle` objects while primitives are just identifier strings. Combining them into one type would either force unnecessary complexity onto primitives or strip capabilities from ref types.

## Using Ref Types and Primitive Types

### In Expression Handlers

When returning from an expression handler, set `refTypeId` for ref types and `javaType` for primitives. Only one should be non null:

```java
// Expression returns a Location (ref type)
return new ExpressionResult(
        ctx.java("who") + ".getLocation()",
        Types.LOCATION.id());

// Expression returns an int (primitive)
return new ExpressionResult(
        ctx.java("str") + ".length()",
        null, Types.INT);
```

### In Expression Builders

Declare the return type statically so tooling can resolve types without executing the handler:

```java
// Ref type return
api.patterns().expression(b -> b
        .pattern("[get] %who:PLAYER% location")
        .returnRefTypeId(Types.LOCATION.id())
        .handler(ctx -> new ExpressionResult(
                ctx.java("who") + ".getLocation()",
                Types.LOCATION.id())));

// Primitive type return
api.patterns().expression(b -> b
        .pattern("%str:EXPR% length")
        .returnJavaType(Types.INT)
        .handler(ctx -> new ExpressionResult(
                ctx.java("str") + ".length()",
                null, Types.INT)));
```

Use `returnRefTypeId()` for object types. Use `returnJavaType()` for primitives and strings.

### In Event Variable Definitions

Both ref types and primitives can be used. The builder has overloaded `addVar` methods that accept either:

```java
api.events().register(api.events().builder("entity_damage")
        .className("org.bukkit.event.entity.EntityDamageEvent")
        .addVar("entity", Types.ENTITY, "event.getEntity()")   // RefTypeHandle
        .addVar("damage", Types.DOUBLE, "event.getDamage()")   // String
        .build());
```

### In Block Variables

Block variables use `addVar` on the `BlockBuilder`. There are two overloads:

- `addVar(name, RefTypeHandle)` stores a full typed reference that tooling can resolve.
- `addVar(name, String)` stores a documentation label only. No `RefTypeHandle` is attached.

When you write `addVar("slot", Types.INT)`, the `Types.INT` constant is the `String` value `"int"`. This matches the `addVar(String, String)` overload, so it acts as a documentation label. Tooling will see the type string `"int"` but will not have a `RefTypeHandle` for it, because primitives do not have one:

```java
// These use the RefTypeHandle overload (full type info for tooling)
.addVar("player", Types.PLAYER)
.addVar("inventory", Types.INVENTORY)

// These use the String overload (documentation label only)
.addVar("slot", Types.INT)
.addVar("title", Types.STRING)
```

This is intentional. Primitives in block variables are simple values that do not need the identity, storage, or placeholder features that ref types provide. The string label is enough for documentation, and the compiler infers the actual type from the Java expression at compile time.
