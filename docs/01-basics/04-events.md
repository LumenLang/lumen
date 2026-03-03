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
            var name = get item's display name
            message player "&7You right clicked with: {name}"
```

Actions: `RIGHT_CLICK_AIR`, `RIGHT_CLICK_BLOCK`, `LEFT_CLICK_AIR`, `LEFT_CLICK_BLOCK`.

### Move

```luma
on move:
    var px = to x
    var py = to y
    var pz = to z
```

:::alert info
This event fires very frequently. Avoid doing heavy work inside it.
:::

### Entity Interact

```luma
on entity_interact:
    if entity is a player:
        var target_name = get entity's name
        message player "&7You interacted with {target_name}!"
```

### Fish

```luma
on fish:
    if state is "CAUGHT_FISH":
        message player "&aNice catch!"
```

States include `FISHING`, `BITE`, `CAUGHT_FISH`, `CAUGHT_ENTITY`, and others.

## Cancelling Events

Some events can be cancelled, preventing the default game behavior:

```luma
on interact:
    if action is "LEFT_CLICK_BLOCK":
        cancel event
        message player "&cBlocked!"
```

## Mixing Events and Commands

A single script can have any number of events and commands. They all work independently and share the same global variables.

```luma
global stored var tag for ref type player default "none"

command tag:
    description "Set your chat tag"

    if args size < 1:
        message player "&7Use /tag <name> to set a tag."
    else:
        var name = get args at index 0
        set tag to name
        message player "&eTag set to [{name}]"

on join:
    if tag is not "none":
        broadcast "&7[{tag}] &e{player_name} &7joined!"
```
