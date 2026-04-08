---
description: "Lumen's publish/subscribe event bus for reacting to events."
---

# Event Bus

Lumen has a lightweight event bus for reacting to lifecycle events. It is separate from Bukkit's event system and is designed for Lumen.

Access it via `LumenProvider.bus()`.

## Subscribing

Create a listener class with methods annotated with `@Subscribe`. Each method must accept exactly one `LumenEvent` subclass parameter.

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

If your listener is a `@Registration` class, any `@Subscribe` methods on it are automatically registered by the scanner. No manual call to `bus().register()` needed.

```java
@Registration
public final class MyListener {

    @Call
    public void register(@NotNull LumenAPI api) { /* ... */ }

    @Subscribe
    public void onScriptLoad(@NotNull ScriptLoadEvent event) { /* ... */ }
}
```

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

## Async Dispatch

Add `@Async` to a subscriber method to run it on a background thread:

```java
@Subscribe
@Async
public void onScriptLoad(@NotNull ScriptLoadEvent event) {
    // runs off the main thread
}
```

To post the entire event asynchronously (all subscribers run off the calling thread):

```java
LumenProvider.bus().postAsync(new SomeEvent());
```

## Built-in Events

- `ScriptLoadEvent`
- `ScriptUnloadEvent`
- `AllScriptsLoadedEvent`
