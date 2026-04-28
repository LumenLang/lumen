---
description: "Register events that scripts listen to with on <name>: blocks."
---

# Events

Events are what `on <name>:` blocks bind to. A regular event wraps a Bukkit event class and auto-generates an
`@EventHandler` method. An advanced event runs custom code generation instead, for things that aren't Bukkit events at
all (lifecycle hooks, repeating ticks, etc.).

Register events through `api.events()`.

## Regular Events

```java
api.events().register(
    api.events().builder("respawn")
        .className("org.bukkit.event.player.PlayerRespawnEvent")
        .description("Fires when a player respawns.")
        .example("on respawn:")
        .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
        .cancellable(false)
        .build()
);
```

The builder generates a method that listens for `PlayerRespawnEvent`, runs the script body, and exposes `player` inside
it.

## addVar

Each `addVar(name, type, expr)` declares a script-visible variable. `expr` is a Java snippet evaluated once at the top
of the generated method, with `event` in scope as the Bukkit event instance. The result is assigned to a local of the
declared `type` and then bound under `name` so subsequent script lines (and child statements of the `on` block) can
reference it.

Multiple `addVar` calls produce multiple locals, emitted in call order:

```java
api.events().builder("block_break")
    .className("org.bukkit.event.block.BlockBreakEvent")
    .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
    .addVar("block", MinecraftTypes.BLOCK, "event.getBlock()")
    .cancellable(true)
    .build();
```

```luma
on block_break:
    message player "You broke a {block_type}!"
```

Because each variable is a real local in the generated method, a multi-line `on` block can read the same variable as
many times as needed without re-evaluating `expr`.

### Documenting and Annotating Variables

Chain after `addVar` to attach information to the most recently added variable:

- `.varDescription(text)` for human-readable docs
- `.withMeta(key, value)` for compile-time metadata that downstream pattern handlers can read via `VarHandle.meta()`

```java
.addVar("entered", MinecraftTypes.PLAYER, "event.getEntered() instanceof Player p ? p : null")
    .withMeta("nullable", true)
    .varDescription("The player who entered, or null if the entity is not a player")
```

Metadata is the common way to signal per-event nuances like nullability without inventing new types.

### Multi-line Expressions

`expr` can be a multi-line Java text block when simple assignment isn't enough. The variable name is already declared as
a local before the block runs, so you assign to it directly:

```java
.addVar("killer",MinecraftTypes.PLAYER,
    """
    if (event.getEntity().getKiller() != null) {
        killer = event.getEntity().getKiller();
    } else {
        killer = null;
    }""")
```

Use a temp variable with a `__` prefix (like `__p`) to avoid colliding with the declared local.

## Advanced Events

When the event isn't backed by a Bukkit event, provide a `BlockHandler` that emits the whole method itself. This is how
`on tick:` is implemented:

```java
api.events().advanced(b -> b
    .name("tick")
    .description("Runs every server tick.")
    .example("on tick:")
    .addImport("org.bukkit.scheduler.BukkitRunnable")
    .handler(new BlockHandler() {
        @Override
        public void begin(@NotNull HandlerContext ctx) {
            ctx.codegen().addImport(LumenPreload.class.getName());
            ctx.out().line("@LumenPreload");
            ctx.out().line("public void __tick_" + ctx.codegen().nextMethodId() + "() {");
            ctx.out().line("new BukkitRunnable() { public void run() {");
        }

        @Override
        public void end(@NotNull HandlerContext ctx) {
            ctx.out().line("} }.runTaskTimer(Lumen.instance(), 0L, 1L);");
            ctx.out().line("}");
        }
    })
);
```

The builder also takes `field(...)` and `addInterface(...)` when the generated class needs extra members.

## Lookup

`api.events().lookup(name)` and `lookupAdvanced(name)` return the registered definition, or null. Useful for detecting
an event another addon already owns.
