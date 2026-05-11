---
description: "Annotation-driven pattern API where each handler is a static Java method instead of a builder lambda."
---

# Modern pattern API

The modern API turns each handler into a static Java method tagged with a kind annotation (`@Statement`, `@Condition`, `@Expression`) and a `@Pattern`. The build plugin scans the compiled addon, the runtime registers each entry through the same builder API as the [classic form](../01-patterns.md).

```java
@Statement
@Pattern("heal %who:PLAYER%")
public static void heal(@Inject Player who) {
    who.setHealth(20);
}
```

Same registration, classic form:

```java
api.patterns().statement("heal %who:PLAYER%",
    ctx -> ctx.out().line(ctx.java("who") + ".setHealth(20);"));
```

## When classic form is required

The classic form is required when the emitted Java differs per call site, e.g. reading `ctx.choice(0)` to pick between branches. The modern API always emits the same body with placeholder substitutions, unless you opt into a [compile section](05-phases.md).
