---
description: "How to use the registration scanner to organize and auto-discover your addon's registrations."
---

# Registration Scanner

The registration scanner discovers classes annotated with `@Registration` in a given package, sorts them by order, and invokes their `@Call` methods with the `LumenAPI` instance. This is how Lumen itself registers all of its built-in patterns, types, events, and more.

Addons can use the same system to organize their registrations into clean, separate classes instead of putting everything in a single `onEnable` method.

## Annotating a Registration Class

A registration class needs two annotations:

- `@Registration` on the class
- `@Call` on the method that receives the API

```java
@Registration
public final class MyPatterns {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(
            "heal %who:PLAYER%",
            (line, ctx, out) -> out.line(ctx.java("who") + ".setHealth(20);")
        );
    }
}
```

The `@Call` method must accept exactly one parameter of type `LumenAPI`. You can name the method whatever you want.

## Scanning a Package

Call `RegistrationScanner.scan()` with the base package to scan. All classes in that package and its sub-packages that are annotated with `@Registration` will be discovered and processed.

```java
@Override
public void onEnable(@NotNull LumenAPI api) {
    RegistrationScanner.scan("com.example.myaddon.registrations");
}
```

This scans `com.example.myaddon.registrations` and all nested packages, finds every `@Registration` class, and invokes their `@Call` methods.

## Controlling Order

Registration classes are sorted by their `order` value before processing. Lower values run first. The default order is `0`.

```java
@Registration(order = -100)
public final class MyTypes {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.types().register(new MyCustomType());
    }
}

@Registration
public final class MyPatterns {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
            .pattern("get %thing:MYTYPE%")
            .handler(ctx -> { /* ... */ }));
    }
}
```

Here `MyTypes` runs before `MyPatterns` because `-100` is lower than `0`. This matters when patterns reference types that must already be registered.

## Requirements

- The class must have a public no-arg constructor (the scanner instantiates it via reflection)
- The `@Call` method must accept exactly one `LumenAPI` parameter
- Classes without `@Registration` in the scanned package are ignored
