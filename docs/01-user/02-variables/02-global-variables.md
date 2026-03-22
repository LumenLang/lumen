---
description: "Global variables shared across commands and events, with optional per-player scoping."
---

# Global Variables

Global variables are shared across all commands, events, and schedules in the same script. They persist for as long as the script is loaded, but are lost when the server restarts or the script is reloaded.

## Defining Global Variables

Declare them at the top level of your script using `global var`:

```luma
global var heartbeat_count default 0

every 1 minute as "heartbeat":
    add 1 to heartbeat_count

command heartbeat:
    message player "&eHeartbeats so far: {heartbeat_count}"
```

The `default` keyword sets the initial value. Both the schedule and the command can see and modify `heartbeat_count` because it is global.

:::alert note
Global variables are reset whenever the script reloads or the server restarts. Use stored variables if you need persistence.
:::

## Per-Player Global Variables

Add `for ref type player` to give each player their own independent copy of the variable:

```luma
global var swap_enabled for ref type player default 0

command swap:
    if swap_enabled == 0:
        set swap_enabled to 1
        message player "&aSwap mode enabled!"
    else:
        set swap_enabled to 0
        message player "&cSwap mode disabled."
```

Each player has their own `swap_enabled` value. Enabling swap mode for one player does not affect others.

## Accessing Another Player's Value

When a variable is per-player, use `for <player>` to read or write a specific player's copy:

```luma
global var coins for ref type player default 0

command earn:
    add 10 to coins
    message player "&a+10 coins! Balance: {coins}"

command pay:
    if args size < 1:
        message player "&cUsage: /pay <player>"
    else:
        var target = get args at index 0
        subtract 10 from coins
        add 10 to coins for target
        message player "&eSent 10 coins to {target}!"
```

With `for target`, you are explicitly accessing another player's copy of the variable.

## No Default

If you omit `default`, the variable starts unset. You can check this with `is set` / `is not set`:

```luma
global var pos1 for ref type player default no location

command select:
    set pos1 to get player's location
    message player "&aPosition set!"

command check:
    if pos1 is not set:
        message player "&cNo position selected."
    else:
        message player "&aPosition is set."
```
