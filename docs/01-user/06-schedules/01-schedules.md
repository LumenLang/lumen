---
description: "Delays, repeating timers, named schedules, and cancellation."
---

# Schedules

Schedules let you run code after a delay or on a repeating timer.

## Time Units

| Unit | Equivalent |
|---|---|
| `ticks` | 1 tick (20 ticks = 1 second) |
| `seconds` | 20 ticks |
| `minutes` | 1200 ticks |
| `hours` | 72000 ticks |
| `days` | 1728000 ticks |

## One-Shot Delays

Run code once after a specified time. Three keywords are available and all do the same thing: `wait`, `after`, and `in`.

```luma
command demo:
    message player "&aStarting..."

    wait 40 ticks:
        message player "&e[2s] Fired after 40 ticks!"

    after 3 seconds:
        message player "&e[3s] Fired after 3 seconds!"

    in 4 seconds:
        message player "&e[4s] Fired after 4 seconds!"
```

Anonymous delays (without a name) are automatically cancelled if the script reloads.

## Repeating Schedules

Run code at a fixed interval:

```luma
every 2 seconds:
    broadcast "&dThis repeats every 2 seconds until reload."
```

## Named Schedules

Giving a schedule a name with `as "name"` lets you control its reload behavior and cancel it from elsewhere.

### Default (Hot-Reload)

No modifier. On reload, the code is swapped but the timer keeps its current position in the interval:

```luma
every 1 minute as "heartbeat":
    add 1 to heartbeat_count
```

### Restarting

On reload, the timer resets from scratch:

```luma
every 5 minutes as "periodic_reminder" restarting:
    broadcast "&eReminder: Check /rules!"
```

### Cancelling

On reload or unload, the schedule is removed entirely:

```luma
every 30 seconds as "temp_task" cancelling:
    broadcast "&7[Debug] Script is still loaded."
```

:::alert info
Anonymous schedules (without a name) are always cancelled on reload. Named schedules without a modifier use hot-reload by default.
:::

## Cancelling Schedules

### By Name

Cancel any named schedule from anywhere in the script:

```luma
command stop:
    cancel schedule "demo_delayed"
    message player "&cSchedule cancelled."
```

### From Inside (Self-Cancel)

Use `cancel` with no arguments inside a schedule body to stop it from running again:

```luma
global var countdown default 5

every 20 ticks as "countdown_task" restarting:
    if countdown > 0:
        message player "&e{countdown}..."
        subtract 1 from countdown
    else:
        message player "&a&lGo!"
        cancel
```

This counts down from 5 and then stops itself.

## Top-Level Schedules

Schedules placed at the top level of a script start as soon as the script loads:

```luma
every 5 minutes as "periodic_reminder" restarting:
    broadcast "&eReminder: Check /rules!"
```