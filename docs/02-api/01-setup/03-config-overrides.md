---
description: "Configuration overrides."
---

# Configuration Overrides

Addons can programmatically override Lumen's boolean config options by returning `ConfigOverride` entries from `configOverrides()` in a `LumenAddon`. This avoids asking users to manually edit `config.yml`.

## Persistence Levels

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

## Example

```java
@Override
public @NotNull List<ConfigOverride> configOverrides() {
    return List.of(
        ConfigOverride.disable(ConfigOption.ENABLE_ALL_SCRIPTS_IMMEDIATELY)
            .lastingSession("registers patterns during plugin enable")
    );
}
```

Lumen logs each override to the console with the addon name, version, and reason.
