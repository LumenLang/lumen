---
description: "Match handler parameters to pattern placeholders and validate their Java types against the type binding."
---

# Inject parameters

`@Inject` marks a handler parameter as a placeholder substitution. The parameter's name (or `@Inject("name")` when overridden) must match a placeholder in the handler's `@Pattern`.

```java
@Statement
@Pattern("hand %item:MATERIAL% to %who:PLAYER%")
public static void hand(@Inject Material item, @Inject Player who) {
    who.getInventory().addItem(new ItemStack(item, 1));
}
```

When the parameter name in source differs from the placeholder name, override it explicitly:

```java
@Statement
@Pattern("hand %item:MATERIAL% to %who:PLAYER%")
public static void hand(@Inject("item") Material material,
                        @Inject("who")  Player target) {
    target.getInventory().addItem(new ItemStack(material, 1));
}
```

## Type checking

Each `@Inject` parameter's declared Java type is checked against the placeholder's type binding. The build fails if they disagree:

```
@Inject parameter 'who' has type 'Ljava/lang/String;' but binding 'PLAYER' produces 'Lorg/bukkit/entity/Player;'
```

Bindings the build plugin does not know about are skipped without diagnostic.
