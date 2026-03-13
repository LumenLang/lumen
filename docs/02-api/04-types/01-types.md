---
description: "How to register custom type bindings and reference types."
---

# Types

Lumen's pattern system uses type bindings to determine how pattern placeholders consume and parse tokens. Addons can register custom type bindings and reference types to extend the type system.

## Type Bindings

A type binding tells the pattern matcher how to handle a `%name:TYPE%` placeholder. It defines how many tokens to consume, how to parse them, and how to convert the parsed value into Java code.

### Implementing AddonTypeBinding

```java
api.types().register(new AddonTypeBinding() {
    @Override
    public @NotNull String id() {
        return "ENTITY";
    }

    @Override
    public Object parse(@NotNull List<String> tokens,
                        @NotNull EnvironmentAccess env) {
        return env.lookupVar(tokens.get(0));
    }

    @Override
    public @NotNull String toJava(Object value,
                                  @NotNull CodegenAccess ctx,
                                  @NotNull EnvironmentAccess env) {
        return ((EnvironmentAccess.VarHandle) value).java();
    }
});
```

The key methods on `AddonTypeBinding` are:

- `id()` returns the unique type identifier used in patterns (e.g. `%name:ENTITY%`)
- `parse()` receives the consumed tokens and returns a parsed value
- `toJava()` converts that parsed value into a Java source expression
- `consumeCount()` controls how many tokens the placeholder consumes (default is 1, return -1 to consume all remaining tokens)
- `meta()` returns optional documentation metadata

### Token Consumption

Override `consumeCount` to control how many tokens the placeholder eats:

- Return `-1` to consume all remaining tokens until the next literal or end of line. This is what `EXPR` and `COND` use.
- Return `0` to consume nothing but still call `parse()` with an empty list. Useful for implicit values.
- Return `1` or more to consume exactly that many tokens.

The default returns 1 for non empty token lists and throws `ParseFailureException` for empty lists.

### Parse Failures

When a type binding cannot parse the given tokens, throw `ParseFailureException`. This tells the pattern matcher to try other patterns or backtrack, rather than treating it as an error.

```java
@Override
public Object parse(@NotNull List<String> tokens,
                    @NotNull EnvironmentAccess env) {
    EnvironmentAccess.VarHandle handle = env.lookupVar(tokens.get(0));
    if (handle == null) {
        throw new ParseFailureException(
            "Unknown variable: " + tokens.get(0)
        );
    }
    return handle;
}
```

### Documentation Metadata

You can provide documentation metadata for your type binding:

```java
@Override
public @Nullable TypeBindingMeta meta() {
    return new TypeBindingMeta(
        "An entity reference variable",
        "org.bukkit.entity.Entity",
        List.of("entity", "zombie"),
        "1.0.0",
        false
    );
}
```

## Reference Types

Reference types (ref types) represent compile time type information for variables. They tell the type system what kind of object a variable holds, enabling placeholder resolution and type checking.

### Registering a Ref Type

```java
RefTypeHandle myType = api.refTypes().register(
    "VILLAGER",
    "org.bukkit.entity.Villager"
);
```

The `register` method takes a type ID and the fully qualified Java class name, and returns a `RefTypeHandle` that you can use when registering events, placeholders, and type bindings.

### Using Built In Types

The `Types` class provides constants for all built in ref types like `Types.PLAYER`, `Types.ENTITY`, `Types.LOCATION`, `Types.WORLD`, `Types.BLOCK`, and `Types.ITEMSTACK`. Use these constants anywhere a `RefTypeHandle` is expected. Check the `Types` class in your IDE for the full list.

### Looking Up Ref Types

You can look up existing ref types by their ID:

```java
RefTypeHandle player = api.refTypes().byId("PLAYER");
```

Returns `null` if the type is not registered.

## Putting It All Together

Type bindings and ref types work together. A type binding handles how a pattern placeholder is parsed, while a ref type tracks what kind of object a variable holds at compile time.

Here is a complete example that registers a custom `VILLAGER` type with type binding, ref type, and placeholder properties:

```java
RefTypeHandle villagerType = api.refTypes().register(
    "VILLAGER",
    "org.bukkit.entity.Villager"
);

api.types().register(new AddonTypeBinding() {
    @Override
    public @NotNull String id() {
        return "VILLAGER";
    }

    @Override
    public Object parse(@NotNull List<String> tokens,
                        @NotNull EnvironmentAccess env) {
        EnvironmentAccess.VarHandle handle = env.lookupVar(tokens.get(0));
        if (handle == null || !villagerType.equals(handle.type())) {
            throw new ParseFailureException(
                "Expected a villager variable"
            );
        }
        return handle;
    }

    @Override
    public @NotNull String toJava(Object value,
                                  @NotNull CodegenAccess ctx,
                                  @NotNull EnvironmentAccess env) {
        return ((EnvironmentAccess.VarHandle) value).java();
    }
});

api.placeholders().property(villagerType, "profession", "$.getProfession().name()");
api.placeholders().property(
    villagerType,
    "level",
    "$.getVillagerLevel()",
    PlaceholderType.NUMBER
);
api.placeholders().defaultProperty(villagerType, "profession");
```

Now scripts can use the new type in patterns and placeholders:

```luma
on villager_interact:
    message player "This villager is a {villager_profession} at level {villager_level}"
```
