---
description: "Method signatures, return-type rules, and inline vs method-based emission for each handler kind."
---

# Handler shape

Every handler is a `public static` method. The kind annotation decides the return type:

- `@Statement` returns `void`
- `@Condition` returns `boolean`
- `@Expression` returns the value type (e.g. `Location`)

Each method takes one parameter per pattern placeholder, marked with `@Inject` (see [Inject parameters](04-inject-params.md)). A leading `HandlerContext` parameter is optional and only needed when the handler has a [compile section](05-phases.md).

## Statements

The body is emitted into the script class as-is.

```java
@Statement
@Pattern("brand %who:PLAYER% with %mark:STRING%")
public static void brand(@Inject Player who, @Inject String mark) {
    who.setCustomName(mark);
    who.setCustomNameVisible(true);
}
```

## Conditions and expressions

A single `return X;` body is inlined as the boolean expression (for `@Condition`) or the value expression (for `@Expression`):

```java
@Condition
@Pattern("%who:PLAYER% can fly")
public static boolean canFly(@Inject Player who) {
    return who.getAllowFlight();
}

@Expression
@Pattern("airspeed of %who:PLAYER%")
public static double airspeedOf(@Inject Player who) {
    return who.getVelocity().length();
}
```

When the body has more than one statement, emission switches to method-based: a static method is generated on the script class and a call to it is emitted at every call site.

```java
@Expression
@Pattern("orbital target for %who:PLAYER%")
public static Location orbitalTargetFor(@Inject Player who) {
    Location anchor = who.getLocation().clone();
    anchor.setY(anchor.getY() + 32);
    return anchor;
}
```

## Forcing method-based emission for statements

Statements inline by default. Add `@MethodBased` when a long body is expected to be repeated across many call sites and the JIT benefits from a single profiled method:

```java
@Statement
@MethodBased
@Pattern("rebuild aura of %who:PLAYER%")
public static void rebuildAuraOf(@Inject Player who) {
    who.removePotionEffect(PotionEffectType.SPEED);
    who.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 2));
    who.removePotionEffect(PotionEffectType.JUMP);
    who.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 600, 2));
}
```

Conditions and expressions choose the mode automatically; `@MethodBased` is ignored on them.

## Banned constructs

Handler bodies cannot declare anonymous or local classes. Extract them to a top-level class.

## Documentation annotations

`@Description`, `@Example` (repeatable), `@Since`, `@Category`, and `@Deprecated` populate the same metadata fields the classic builder API exposes:

```java
@Statement
@Pattern("brand %who:PLAYER% with %mark:STRING%")
@Description("Sets a custom name on the player and makes it visible.")
@Example("brand player with \"chosen\"")
@Since("1.4.0")
@Category("Player")
public static void brand(@Inject Player who, @Inject String mark) { ... }
```
