---
description: "Stored variables that persist across server restarts and script reloads."
---

# Stored Variables

Stored variables persist across server restarts. Their values are saved to disk automatically, so data is never lost when the server shuts down or the script reloads.

## Global Stored Variables

Use `global stored var` to create a variable that is both shared across all blocks and saved permanently:

```luma
global stored var current_bounty default none

command bounty:
    if current_bounty is not set:
        message player "&7No active bounty."
    else:
        message player "&6There is an active bounty!"
```

Even after a server restart, `current_bounty` retains its value.

## Per-Player Stored Variables

Combine `global stored var` with `for ref type player` so each player has their own persistent value:

```luma
global stored var streak for ref type player default 0

on entity_death:
    if killer is set:
        add 1 to streak for killer
        message killer "&aKill streak: {streak}"

on player_death:
    if streak >= 3:
        message player "&cYour streak of {streak} has been ended!"
    set streak to 0 for player
```

Every player's streak is saved independently and survives restarts.

## Inline Store

For event-scoped persistent data, use the `store` keyword directly inside an event or command. This is a shorthand that creates a stored variable on the spot:

```luma
on block_break:
    if block type is stone:
        store mined for player default 0
        add 1 to mined
        send actionbar "&7Blocks mined: {mined}" to player
```

The value of `mined` is saved per-player and persists across restarts.

## Deleting Stored Data

When you need to reset stored data, use `delete stored`:

```luma
every 24000 ticks as "quest_reset":
    broadcast "&eDaily quests have been reset!"
    delete stored mined
```

This removes the stored values for all players, giving everyone a fresh start.

:::alert info
Deleting stored data is permanent. Once deleted, the values cannot be recovered.
:::

## Stored Lists and Maps

Stored variables can hold complex types too, including lists and maps:

```luma
global stored var todos for ref type player default new list
global stored var balances for ref type player default new map
global stored var arenas default new list of arena
```

All entries in these collections are saved to disk along with the variable itself.
