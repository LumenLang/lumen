---
description: "Define script settings with the config block and use them as variables."
---

# Config

The `config:` block lets you define settings for your script. Config values become variables that are accessible from any command, event, or schedule in the script. They make it easy to adjust behavior without digging through your script logic.

## Defining Config Values

Place a `config:` block at the top level of your script with simple `key: value` pairs:

```luma
config:
    prefix: "&6[Server]&r"
    maxLevel: 30
```

Each entry creates a variable with that name. Numbers stay as numbers, and text values must be wrapped in quotes.

## Using Config Values

Once defined, config values work as regular variables. Use them in statements, conditions, and string placeholders.

```luma
config:
    prefix: "&6[Server]&r"
    maxLevel: 30

on join:
    broadcast "{prefix} &a{player_name} joined!"
    message player "{prefix} &eWelcome back!"

    set lv to get player's xp level
    if lv > maxLevel:
        set player's xp level to maxLevel
        message player "{prefix} &7Level capped to {maxLevel}."

on quit:
    broadcast "{prefix} &c{player_name} left."

command serverinfo:
    description "Show server configuration"

    message player "{prefix} &6Server Settings:"
    message player "  &7Max level: &f{maxLevel}"
```

The `{prefix}` placeholder gets replaced with the gold `[Server]` text everywhere it appears. Changing `prefix` in the config block instantly updates every usage.

## Config Values in Schedules and Conditions

Config values can be used anywhere a normal variable or number would go, including schedule intervals and condition checks:

```luma
config:
    cooldown_ticks: 100
    spawn_interval: 200

every spawn_interval ticks as "spawner":
    broadcast "&6Something spawned!"
```

## Value Types

Config entries have two allowed value types:

- **Numbers** (like `30`, `100`, `5.5`) are stored as numbers and can be used in math and comparisons
- **Quoted strings** (like `"&6[Server]&r"`, `"hello world"`) are stored as text

Unquoted text is not allowed.
