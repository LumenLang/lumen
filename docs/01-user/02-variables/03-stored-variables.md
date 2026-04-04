---
description: "Stored variables that persist across server restarts and script reloads."
---

# Stored Variables

Stored variables persist across server restarts. Their values are saved to disk automatically, so data is never lost when the server shuts down or the script reloads.

## Global Stored Variables

Use `global stored` to create a variable that is both shared across all blocks and saved permanently:

```luma
global stored total_joins with default 0

on join:
    add 1 to total_joins
    broadcast "&7[Server] Total joins: {total_joins}"
```

Even after a server restart, `total_joins` retains its value.

## Per-Player Stored Variables

Combine `global stored` with `for ref type player` so each player has their own persistent value:

```luma
global stored coins for ref type player with default 0

command earn:
    add 10 to coins
    message player "&a+10 coins! Total: {coins}"

command balance:
    message player "&6Your coins: {coins}"
```

Every player's coins are saved independently and survive restarts.

## Inline Load

For event-scoped persistent data, use `load` directly inside an event or command. This creates a stored variable reference on the spot:

```luma
on block_break:
    if block type is stone:
        load mined for player with default 0
        add 1 to mined
        send actionbar "&7Blocks mined: {mined}" to player
```

The value of `mined` is saved per player and persists across restarts.

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
global stored notes for ref type player with default new list
global stored balances for ref type player with default new map
global stored warps with default new list of warp
```

All entries in these collections are saved to disk along with the variable itself.
