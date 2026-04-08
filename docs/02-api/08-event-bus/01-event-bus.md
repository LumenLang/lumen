---
description: "Lumen's publish/subscribe event bus for reacting to events."
---

# Event Bus

Lumen has a lightweight event bus for reacting to lifecycle events. It is separate from Bukkit's event system and is designed for Lumen.

Access it via `LumenProvider.bus()`.

## Subscribing

Create a listener class with methods annotated with `@Subscribe`. Each method must accept exactly one parameter, which may be `LumenEvent`, any subclass of it, or any interface.

```java
public class MyListener {

    @Subscribe
    public void onScriptLoad(@NotNull ScriptLoadEvent event) {
        System.out.println("Script loaded: " + event.scriptName());
    }
}
```

Register it with the bus:

```java
LumenProvider.bus().register(new MyListener());
```

To unregister:

```java
LumenProvider.bus().unregister(myListener);
```

### Auto-registration via @Registration

If your listener is a `@Registration` class, any `@Subscribe` methods on it are automatically registered by the scanner. No manual call to `bus().register()` needed. This is the recommended approach for addons.

```java
@Registration
public final class MyListener {

    @Call
    public void register(@NotNull LumenAPI api) { /* ... */ }

    @Subscribe
    public void onScriptLoad(@NotNull ScriptLoadEvent event) { /* ... */ }
}
```

:::alert warning
Listeners registered via manual `bus().register()` calls are cleared when Lumen disables. If Lumen is disabled and re-enabled, those listeners are lost. Only use manual registration if you handle re-registration in response to Lumen's lifecycle. For reliable listener registration across Lumen restarts, use `@Registration` with `@Subscribe` instead.
:::

## Priority

Subscribers run in order from `LOWEST` to `MONITOR`. Use `MONITOR` to observe the final outcome only, not to modify or cancel events.

```java
@Subscribe(priority = Priority.HIGH)
public void onScriptLoad(@NotNull ScriptLoadEvent event) { /* ... */ }
```

## Cancellable Events

Events that extend `Cancellable` can be cancelled. All subscribers still receive the event regardless.

```java
@Subscribe
public void onSomeEvent(@NotNull SomeCancellableEvent event) {
    event.cancelled(true);
}
```

Check the result after posting:

```java
SomeCancellableEvent event = bus.post(new SomeCancellableEvent());
if (!event.cancelled()) {
    // proceed
}
```

## Subscribing to Supertypes

A subscriber method's parameter type can be a superclass of `LumenEvent`, or any interface. It will receive all events whose concrete type is an instance of, or implements, that type.

```java
@Subscribe
public void onAnyEvent(@NotNull LumenEvent event) {
    // receives every event posted on the bus
}
```

## Built-in Events

- `ScriptLoadEvent`
- `ScriptUnloadEvent`
- `AllScriptsLoadedEvent`
