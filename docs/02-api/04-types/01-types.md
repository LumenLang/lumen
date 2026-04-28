---
description: "The Lumen compile-time type system for describing variables, parameters, and expressions."
---

# Types

Every value in Lumen has a known compile-time type, represented by a `LumenType`. Types describe what a variable holds,
what a parameter expects, and what an expression produces. When you register patterns, events, or type bindings, you'll
reference `LumenType` to declare these things.

## Type Categories

There are four kinds of `LumenType`:

- `PrimitiveType` for `int`, `long`, `double`, `float`, `boolean`, `string`
- `ObjectType` for Bukkit classes (Player, Location, ItemStack) and addon-defined object types
- `CollectionType` for parameterized containers like `list of player`
- `NullableType` wrapping any of the above to indicate the value may be null

## Primitives

`PrimitiveType` is an enum. Reference constants directly:

```java
PrimitiveType.INT
PrimitiveType.STRING
PrimitiveType.BOOLEAN
```

## Object Types

`ObjectType` represents a Java class at the Lumen level. Built-in Minecraft types live on `MinecraftTypes`:

```java
MinecraftTypes.PLAYER
MinecraftTypes.LOCATION
MinecraftTypes.ENTITY
MinecraftTypes.ITEMSTACK
```

To expose a new Java class to the type system, construct an `ObjectType` and register it:

```java
ObjectType myType = new ObjectType("MYTYPE", "com.example.MyClass");
LumenTypeRegistry.

register(myType);
```

### Supertypes

Pass supertypes when the new type should participate in a hierarchy. `PLAYER` is declared as a subtype of
`LIVING_ENTITY`, which in turn extends `ENTITY`, so anywhere an `ENTITY` is expected a `PLAYER` is accepted:

```java
new ObjectType("WOLF","org.bukkit.entity.Wolf","$.getUniqueId().toString()",List.of(MinecraftTypes.LIVING_ENTITY));
```

### Key Templates

The third constructor argument is a Java expression template for producing a unique storage key, where `$` stands in for
the variable. This is used by scoped variables and persistent storage to identify an instance:

```java
new ObjectType("PLAYER","org.bukkit.entity.Player","$.getUniqueId().toString()")
```

If omitted, the key defaults to `String.valueOf($)`.

## Nullability

Wrap any type to mark it nullable:

```java
LumenType nullablePlayer = MinecraftTypes.PLAYER.wrapAsNullable();
```

Check and unwrap:

```java
type.nullable()
type.

unwrap()
```

`assignableFrom` respects nullability: a nullable value cannot be assigned into a non-nullable slot.
