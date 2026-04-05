---
description: "How to register placeholder properties for use inside strings and expressions."
---

# Placeholders

Placeholders let scripts embed variable properties inside strings using the `{variable_property}` syntax. Addons can register custom placeholder properties for any reference type.

Placeholders are registered through `api.placeholders()`, which returns a `PlaceholderRegistrar`.

## How Placeholders Work

When a script contains a string like `"Hello {player_name}"`, the compiler:

1. Finds the `{player_name}` placeholder
2. Splits it into the variable name (`player`) and the property name (`name`)
3. Looks up the variable's ref type (e.g. `PLAYER`)
4. Finds the registered property template for that ref type and property
5. Replaces `$` in the template with the Java variable name
6. Builds a Java string concatenation expression

This all happens at compile time. There is no runtime placeholder parsing.

## Registering Properties

### String Properties

By default, properties are treated as strings:

```java
api.placeholders().property(Types.PLAYER, "name", "$.getName()");
api.placeholders().property(Types.PLAYER, "world", "$.getWorld().getName()");
api.placeholders().property(Types.PLAYER, "uuid", "$.getUniqueId().toString()");
```

The `$` in the template is replaced with the Java variable name at compile time. If the script has a variable named `player`, then `{player_name}` compiles to `player.getName()`.

### Numeric Properties

Properties that return numbers should specify `PlaceholderType.NUMBER` so they can be used in math expressions:

```java
api.placeholders().property(Types.PLAYER, "health", "$.getHealth()", PlaceholderType.NUMBER);
api.placeholders().property(Types.PLAYER, "x", "$.getLocation().getX()", PlaceholderType.NUMBER);
api.placeholders().property(Types.PLAYER, "y", "$.getLocation().getY()", PlaceholderType.NUMBER);
api.placeholders().property(Types.PLAYER, "z", "$.getLocation().getZ()", PlaceholderType.NUMBER);
```

In a script, numeric properties can participate in arithmetic:

```luma
set yBelow to {player_y} - 1
```

String properties can only be used in string contexts.

### Boolean Properties

For boolean values, use `PlaceholderType.BOOLEAN`:

```java
api.placeholders().property(Types.PLAYER, "sneaking", "$.isSneaking()", PlaceholderType.BOOLEAN);
```

## Default Properties

You can set a default property for a ref type. This is used when the placeholder has no underscore (e.g. `{player}` instead of `{player_name}`):

```java
api.placeholders().defaultProperty(Types.PLAYER, "name");
```

With this, `{player}` in a string behaves the same as `{player_name}`.

## Custom Ref Type Placeholders

When you register your own ref type, you can also register placeholders for it:

```java
RefTypeHandle villagerType = api.refTypes().register("VILLAGER", "org.bukkit.entity.Villager");

api.placeholders().property(villagerType, "profession", "$.getProfession().name()");
api.placeholders().property(villagerType, "level", "$.getVillagerLevel()", PlaceholderType.NUMBER);
api.placeholders().defaultProperty(villagerType, "profession");
```

In a script:

```luma
message player "This villager is a {villager_profession} at level {villager_level}"
```

## PlaceholderType

The `PlaceholderType` enum controls how the property value is treated. You can pass `PlaceholderType.STRING` (the default), `PlaceholderType.NUMBER` (for use in math), or `PlaceholderType.BOOLEAN` as the last argument to `property()`.
