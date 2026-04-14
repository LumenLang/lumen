---
description: "How to register custom events that scripts can listen to with the on keyword."
---

# Events

Events let scripts react to things happening on the server. Addons can register custom event definitions so scripts can use `on <name>:` to listen for any Bukkit event or custom behavior.

Events are registered through `api.events()`, which returns an `EventRegistrar`.

## Regular Events

Regular events are tied to a Bukkit event class. When you register a regular event, Lumen automatically generates an `@EventHandler` method that listens for that Bukkit event and runs the script's body.

### Using the Builder

```java
api.events().register(
    api.events().builder("respawn")
        .by("MyAddon")
        .className("org.bukkit.event.player.PlayerRespawnEvent")
        .description("Fires when a player respawns.")
        .example("on respawn:")
        .since("1.0.0")
        .category("Player")
        .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
        .cancellable(false)
        .build()
);
```

In a script:

```luma
on respawn:
    message player "Welcome back!"
```

### Adding Variables

The `addVar` method defines variables that are available inside the event block. Each variable has a name, a ref type from the `Types` class, and a Java expression that extracts the value from the Bukkit event object.

```java
api.events().register(
    api.events().builder("block_break")
        .by("MyAddon")
        .className("org.bukkit.event.block.BlockBreakEvent")
        .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
        .addVar("block", MinecraftTypes.BLOCK, "event.getBlock()")
        .cancellable(true)
        .build()
);
```

The Java expression uses `event` as the variable name for the Bukkit event object. Scripts can then use these variables directly:

```luma
on block_break:
    message player "You broke a {block_type}!"
```

### Variable Descriptions

You can chain `.varDescription()` after `addVar()` to document what a specific variable represents:

```java
api.events().register(
    api.events().builder("join")
        .by("MyAddon")
        .className("org.bukkit.event.player.PlayerJoinEvent")
        .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
        .varDescription("The player who joined the server")
        .addVar("message", "String", "event.getJoinMessage()")
        .varDescription("The join message shown in chat")
        .build()
);
```

### Variable Metadata

You can chain `.withMeta()` after `addVar()` to attach compile time metadata to a variable. Common metadata keys include `"nullable"` for variables that may be null:

```java
api.events().register(
    api.events().builder("vehicle_enter")
        .by("MyAddon")
        .className("org.bukkit.event.vehicle.VehicleEnterEvent")
        .addVar("player", MinecraftTypes.PLAYER, "event.getEntered() instanceof Player ? (Player) event.getEntered() : null")
        .withMeta("nullable", true)
        .varDescription("The player who entered, or null if the entity is not a player")
        .addVar("vehicle", "org.bukkit.entity.Vehicle", "event.getVehicle()")
        .varDescription("The vehicle that was entered")
        .cancellable(true)
        .build()
);
```

Metadata is propagated to the resulting `VarHandle` when the event fires. Downstream patterns can inspect metadata using `VarHandle.hasMeta()` and `VarHandle.meta()` to perform parse time validation.

## Advanced Events

Advanced events give you full control over the generated code. Instead of tying to a Bukkit event class, you provide a `BlockHandler` that emits whatever Java code you want. This is useful for events that are not directly backed by a Bukkit event, like lifecycle hooks or scheduled ticks.

Here is an example based on the real `tick` event implementation, which uses `@LumenPreload` to generate a method that starts a repeating task:

```java
api.events().advanced(b -> b
    .name("tick")
    .by("MyAddon")
    .description("Runs every server tick.")
    .example("on tick:")
    .since("1.0.0")
    .category("Lifecycle")
    .addImport("org.bukkit.scheduler.BukkitRunnable")
    .handler(new BlockHandler() {
        @Override
        public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
            ctx.codegen().addImport(LumenPreload.class.getName());
            out.line("@LumenPreload");
            out.line("public void __tick_" + out.lineNum() + "() {");
            out.line("new BukkitRunnable() { public void run() {");
        }

        @Override
        public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
            out.line("} }.runTaskTimer(Lumen.instance(), 0L, 1L);");
            out.line("}");
        }
    })
);
```

Advanced events can also add imports, fields, and interfaces to the generated class using the builder methods `addImport()`, `field()`, and `addInterface()`.

## Looking Up Events

You can look up previously registered events by name:

```java
EventDefinition join = api.events().lookup("join");
AdvancedEventDefinition tick = api.events().lookupAdvanced("tick");
```

Both return `null` if the event is not registered. This can be useful for checking whether another addon has already registered an event before yours.
