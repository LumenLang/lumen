---
description: "How to register type bindings that parse pattern placeholders into values and emit Java code."
---

# Type Bindings

A type binding teaches Lumen how to parse a pattern placeholder like `%x:MYTYPE%`. When a pattern matches, the binding
consumes some tokens, parses them into a value, and later converts that value into a Java source expression.

## Anatomy

Every binding implements three operations:

- `consumeCount` decides how many tokens the binding claims from the input
- `parse` turns those tokens into a parsed value
- `toJava` converts the parsed value into Java source for code emission

```java
api.types().

register(new AddonTypeBinding() {
    @Override
    public @NotNull String id () {
        return "ATTRIBUTE";
    }

    @Override
    public int consumeCount (@NotNull List < String > tokens, @NotNull EnvironmentAccess env){
        if (tokens.isEmpty()) throw new ParseFailureException("expected an attribute name");
        return AttributeNames.resolve(tokens.get(0)) != null ? 1 : 0;
    }

    @Override
    public Object parse (@NotNull List < String > tokens, @NotNull EnvironmentAccess env){
        String resolved = AttributeNames.resolve(tokens.get(0));
        if (resolved == null) throw new ParseFailureException("'" + tokens.get(0) + "' is not a valid attribute");
        return resolved;
    }

    @Override
    public @NotNull String toJava (Object value, @NotNull CodegenAccess ctx, @NotNull EnvironmentAccess env){
        ctx.addImport(Attribute.class.getName());
        return "Attribute." + value;
    }
});
```

## consumeCount

Return values:

- `-1` consumes all tokens up to the next literal (used by `EXPR`)
- `0` consumes no tokens but still calls `parse` with an empty list
- `1` or more consumes exactly that many tokens

Throw `ParseFailureException` to reject the match. The pattern matcher treats this as a non-match and tries other
candidates.

## Working With ObjectType and Bindings

`ObjectType` defines the type itself (name, Java class, dedup key). The binding handles pattern parsing and code
generation.

```java
// 1. Register the ObjectType
ObjectType rewardType = LumenTypeRegistry.register(new ObjectType("REWARD", "com.example.Reward"));

// 2. Register the binding with same id
api.

types().

register(new AddonTypeBinding() {
    @Override
    public @NotNull String id () {
        return "REWARD";
    }

    @Override
    public int consumeCount (@NotNull List < String > tokens, @NotNull EnvironmentAccess env){
        return 1;
    }

    @Override
    public Object parse (@NotNull List < String > tokens, @NotNull EnvironmentAccess env){
        String name = tokens.get(0);
        VarHandle ref = env.lookupVar(name);
        if (ref == null || !isReward(ref)) {
            throw new ParseFailureException("'" + name + "' is not a reward variable");
        }
        return ref;
    }

    @Override
    public @NotNull String toJava (Object value, @NotNull CodegenAccess ctx, @NotNull EnvironmentAccess env){
        return ((VarHandle) value).java();
    }

    private boolean isReward (@NotNull VarHandle ref){
        return rewardType.equals(ref.type().unwrap());
    }
});
```

Now `%reward:REWARD%` in patterns resolves both the type and binding automatically. The binding validates that variables
match the registered type before code generation.

## Nullable Placeholders

Prefix the type with `NULLABLE_` in a pattern to accept nullable values:

```
give %p:NULLABLE_PLAYER% reward
```

The matcher strips the prefix, resolves `PLAYER`, and wraps the resulting type as nullable. You do not write a separate
binding; nullability is applied to the existing one.

## Metadata

Override `meta()` so the documentation generator and tooling can describe your binding:

```java

@Override
public @NotNull TypeBindingMeta meta() {
    return new TypeBindingMeta(
            "Resolves an attribute name into a Bukkit Attribute constant.",
            Attribute.class.getName(),
            List.of("set entity max_health to 40"),
            "1.0.0",
            false
    );
}
```
