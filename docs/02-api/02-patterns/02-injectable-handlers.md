---
description: "How to register patterns using injectable bytecode instead of string-based Java emission."
---

# Injectable handlers

> **Beta:** Injectable handlers are functional but still evolving. The API and capabilities will change in future versions.

Injectable handlers let you write real, compilable Java inside a lambda or a static method instead of building Java source code as strings. Lumen extracts the compiled bytecode from your lambda or method and injects it directly into the generated script class.

## How It Works

You write a lambda or reference a static method with real Java code as your handler. When the addon loads, Lumen takes the compiled bytecode from that lambda or method. When a script matches the pattern, the bytecode gets transplanted into the compiled script class so it runs with the real parameter values.

You never construct Java strings. The IDE type checks your code, gives you autocomplete, and catches errors before anything runs.

## Fakes

Since the lambda runs at registration time, you cannot access real parameter values. `Fakes` provides placeholder accessors that Lumen detects and replaces with the actual parameter variables during injection.

```java
Player player = Fakes.fake("who");
Location loc = Fakes.fake("loc");
String msg = Fakes.fakeString("msg");
int amount = Fakes.fakeInt("amount");
double value = Fakes.fakeDouble("value");
long ticks = Fakes.fakeLong("ticks");
float speed = Fakes.fakeFloat("speed");
boolean flag = Fakes.fakeBoolean("flag");
```

The string argument must match the placeholder name in the pattern (e.g. `%who:PLAYER%` uses `"who"`).

`Fakes.fake()` is `@NotNull`, so the IDE will not warn about null checks on the returned value. If you have a binding that could genuinely be nullable, use `Fakes.fakeNullable()` instead, which is `@Nullable`.

## Statements

### Simple Registration

```java
api.patterns().injectable("heal %who:PLAYER% by %amount:INT%", () -> {
    Player player = Fakes.fake("who");
    int amount = Fakes.fakeInt("amount");
    player.setHealth(Math.min(player.getHealth() + amount, 20.0));
    player.sendMessage("Healed by " + amount + "!");
});
```

### Builder Registration

```java
api.patterns().statement(b -> b
    .by("MyAddon")
    .pattern("boost %who:PLAYER% by %amount:DOUBLE%")
    .description("Boosts a player by the given velocity.")
    .example("boost player by 2.0")
    .since("1.0.0")
    .category(Categories.PLAYER)
    .injectableHandler(() -> {
        Player player = Fakes.fake("who");
        double amount = Fakes.fakeDouble("amount");
        player.setVelocity(player.getVelocity().setY(amount));
        player.sendMessage("Boosted!");
    })
);
```

## Expressions

### Simple Registration

```java
api.patterns().injectableExpression("world of %who:PLAYER%", Types.WORLD.id(), null, () -> {
    Player player = Fakes.fake("who");
    return player.getWorld();
});
```

### Builder Registration

```java
api.patterns().expression(b -> b
    .by("MyAddon")
    .pattern("location of %who:PLAYER%")
    .description("Returns the current location of a player.")
    .example("var loc = location of player")
    .since("1.0.0")
    .category(Categories.PLAYER)
    .returnRefTypeId(Types.LOCATION.id())
    .injectableHandler(() -> {
        Player player = Fakes.fake("who");
        return player.getLocation();
    })
);
```

## Conditions

### Simple Registration

```java
api.patterns().injectableCondition("%p:PLAYER% is on fire", () -> {
    Player player = Fakes.fake("p");
    return player.getFireTicks() > 0;
});
```

### Builder Registration

```java
api.patterns().condition(b -> b
    .by("MyAddon")
    .pattern("%p:PLAYER% is swimming")
    .description("Checks whether a player is currently swimming.")
    .example("if player is swimming:")
    .since("1.0.0")
    .category(Categories.PLAYER)
    .injectableHandler(() -> {
        Player player = Fakes.fake("p");
        return player.isSwimming();
    })
);
```

## Static Methods

Instead of an inline lambda you can point Lumen at a plain static method in your class. The bytecode is extracted from the method body exactly the same way.

### Statement

```java
api.patterns().statement(b -> b
    // ...
    .injectableHandler(MyAddon.class, "test")
);

public static void test() {
    // ...
}
```

### Expression

```java
api.patterns().expression(b -> b
    // ...
    .injectableHandler(MyAddon.class, "test")
);

public static Object test() {
    /// ...
}
```

The method return type must be `Object` for reference types when declared as a static method.

### Condition

```java

api.patterns().expression(b -> b
    // ...
    .injectableHandler(MyAddon.class, "test")
);

public static boolean test() {
    // ...
}
```