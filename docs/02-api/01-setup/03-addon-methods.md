---
description: "Addon lifecycle hooks, registration timing, and configuration overrides."
---

# Addon Methods

This page covers addon lifecycle hooks and configuration overrides. For the basics of creating an addon and registering it, see [Getting Started](01-getting-started.md).

## Lifecycle

### `onLoad()`

Called immediately after the addon is discovered (jar based) or registered (plugin based), before `onEnable()`. The `LumenAPI` is available at this point via `LumenProvider.api()`, since Lumen initializes itself before any addon's `onLoad()` runs.

```java
@Override
public void onLoad() {
    loadMyConfig();
}
```

### `onDisable()`

Called when Lumen shuts down (server stop or plugin disable). Addons are disabled in reverse registration order.

```java
@Override
public void onDisable() {
    connection.close();
}
```

## Registration Timing

`LumenProvider.registerAddon()` is available during the `onLoad()` phase. This means plugin based addons can register from their own `onLoad()`:

```java
public class MyPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        LumenProvider.registerAddon(new MyAddon());
    }
}
```

The addon's `onEnable(LumenAPI)` is called later, once all addons have been collected and Lumen is ready to accept registrations. This is true regardless of the `enable-all-scripts-immediately-on-startup` setting.

If you register during your plugin's `onEnable()` instead, the addon is enabled immediately since Lumen has already finished its own enable phase by then.

## Configuration Overrides

Addons can programmatically override Lumen's boolean config options by returning `ConfigOverride` entries from `configOverrides()`. This avoids asking users to manually edit `config.yml`.

### Available Options

- `PAPER_ONLY_FEATURES` controls whether Paper specific APIs are used.
- `REDUCE_CLASSPATH` controls whether the compiler removes internal libraries while compiling. Disable if your addon needs those internal libraries.
- `ENABLE_ALL_SCRIPTS_IMMEDIATELY` controls whether scripts load during startup before plugins enable. Disable if your addon registers patterns in the plugin enable phase.

Both `disable()` and `enable()` are supported.

### Persistence Levels

Each override has a persistence level that controls how long it lasts:

- `session`
  - Does **not** survive a config reload
  - Does **not** survive a restart
- `lastingSession`
  - **Survives** a config reload
  - Does **not** survive a restart
- `permanent`
  - **Survives** a config reload
  - **Survives** a restart
  - Written to `config.yml`

### Example

```java
@Override
public @NotNull List<ConfigOverride> configOverrides() {
    return List.of(
        ConfigOverride.disable(ConfigOption.REDUCE_CLASSPATH)
            .permanent("provides compile-time helper classes"),
        ConfigOverride.disable(ConfigOption.ENABLE_ALL_SCRIPTS_IMMEDIATELY)
            .lastingSession("registers patterns during plugin enable")
    );
}
```

Lumen logs each override to the console with the addon name, version, and reason.
