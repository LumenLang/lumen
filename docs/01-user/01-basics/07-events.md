---
description: "How to listen for game events like joins, block breaks, clicks, and more."
---

# Events

Events let your scripts react to things that happen in the game. When a player joins, breaks a block, clicks something, or moves around, your script can respond.

## Listening to an Event

Use `on` followed by the event name and a colon:

```luma
on join:
    message player "&eWelcome to the server!"
```

## Common Events

### Player Join and Quit

```luma
on join:
    broadcast "&a{player_name} joined the server!"

on quit:
    broadcast "&c{player_name} left the server."
```

`broadcast` sends a message to every online player. `message player` sends only to the player that triggered the event.

### Block Break

```luma
on block_break:
    if block type is stone:
        message player "&7You mined a stone block."
```

### Player Death

```luma
on player_death:
    message player "&cYou died!"

    if killer is set:
        message killer "&aYou killed {player_name}!"
```

### Entity Death

```luma
on entity_death:
    if killer is set:
        message killer "&a+1 Kill!"
```

### Interact

Fires when a player clicks in the world. The `action` tells you what kind of click it was.

```luma
on interact:
    if item is set:
        if action is "RIGHT_CLICK_AIR":
            set name to get item's display name
            message player "&7You right clicked with: {name}"
```

Actions: `RIGHT_CLICK_AIR`, `RIGHT_CLICK_BLOCK`, `LEFT_CLICK_AIR`, `LEFT_CLICK_BLOCK`.

:::alert info
All of the events, including their variables, can be found at the [reference documentation](https://lumenlang.dev/events).
:::

## Cancelling Events

Some events can be cancelled, preventing the default game behavior:

```luma
on interact:
    if action is "LEFT_CLICK_BLOCK":
        cancel event
        message player "&cBlocked!"
```
