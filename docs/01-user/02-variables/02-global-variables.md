---
description: "Global variables shared across commands and events, with optional scoped access per reference type."
---

# Global Variables

Global variables are shared across all commands, events, and schedules in the same script. They persist for as long as the script is loaded, but are lost when the server restarts or the script is reloaded.

## Defining Global Variables

Declare them at the top level of your script using `global`:

```luma
global heartbeat_count with default 0

every 1 minute as "heartbeat":
    add 1 to heartbeat_count

command heartbeat:
    message player "&eHeartbeats so far: {heartbeat_count}"
```

The `with default` clause sets the initial value. Both the schedule and the command can see and modify `heartbeat_count` because it is global.

:::alert note
Global variables are reset whenever the script reloads or the server restarts. Use stored variables if you need persistence.
:::

## Scoped Global Variables

Add `scoped` to give each scope reference its own independent value:

```luma
global scoped swap_enabled with default 0

command swap:
    set enabled to get swap_enabled for player
    if enabled == 0:
        set swap_enabled to 1 for player
        message player "&aSwap mode enabled!"
    else:
        set swap_enabled to 0 for player
        message player "&cSwap mode disabled."
```

Each player has their own `swap_enabled` value. Enabling swap mode for one player does not affect others.

## Accessing Another Reference's Value

Use `for <scope>` to read or write a specific scope reference's value:

```luma
global scoped coins with default 0

command earn:
    set bal to get coins for player
    add 10 to bal
    set coins to bal for player
    message player "&a+10 coins! Balance: {bal}"

command pay:
    if args size < 1:
        message player "&cUsage: /pay <player>"
    else:
        set target to get args at index 0
        set my_bal to get coins for player
        set target_bal to get coins for target
        subtract 10 from my_bal
        add 10 to target_bal
        set coins to my_bal for player
        set coins to target_bal for target
        message player "&eSent 10 coins to {target}!"
```

## No Default

If you use `no <type>` as the default value, the variable starts unset. You can check this with `is set` / `is not set`:

```luma
global scoped pos1 with default no location

command select:
    set pos1 to get player's location for player
    message player "&aPosition set!"

command check:
    set p1 to get pos1 for player
    if p1 is not set:
        message player "&cNo position selected."
    else:
        message player "&aPosition is set."
```
