---
description: "How to create inventory GUIs, set slots, fill ranges, open them for players, and handle click events."
---

# Inventories

Lumen provides a full inventory GUI system. You can create custom inventories, place items in slots, open them for players, and respond to clicks.

For the full list of inventory patterns, see the reference pages:
- [Inventory Statements](https://lumenlang.dev/statements?cats=Inventory)
- [Inventory Expressions](https://lumenlang.dev/expressions?cats=Inventory)
- [Inventory Conditions](https://lumenlang.dev/conditions?cats=Inventory)
- [Inventory Blocks](https://lumenlang.dev/blocks?cats=Inventory)

## Creating an Inventory

Inventories need a name, a size (multiple of 9), and optionally a title.

```luma
var gui = new inventory "punishments" with size 27 titled "<red><b>Punish Player"
```

You can also use rows instead of raw size:

```luma
var gui = new inventory "punishments" with rows 3 titled "<red><b>Punish Player"
```

Without a title:

```luma
var gui = new inventory "storage" with rows 6
```

## Setting Slots

Place items in specific slots (0-indexed, left to right, top to bottom):

```luma
var icon = new item barrier
set icon's name to "<red>Ban"
set icon's lore to "<gray>Permanently removes the player|<dark_red>This action is irreversible"
set slot 13 of gui to icon
```

You can also place raw materials:

```luma
set slot 0 of gui to emerald
set slot 1 of gui to gold_ingot 32
```

## Filling Ranges

Fill a range of slots with the same item. Useful for borders and backgrounds.

```luma
var filler = new item gray_stained_glass_pane
set filler's name to " "
fill slots 0 to 8 of gui with filler
fill slots 18 to 26 of gui with filler
```

Or with a raw material:

```luma
fill slots 0 to 8 of gui with black_stained_glass_pane
```

## Clearing Slots

```luma
clear slot 13 of gui
clear gui inventory
```

## Opening for Players

```luma
open gui for player
```

## Closing Inventories

```luma
close player's inventory
```

## Querying Inventories

```luma
var item = get item in slot 0 of gui
var sz = get gui inventory size
var free = get first empty slot of gui
var gui_name = get name of inventory
```

## Inventory Conditions

```luma
if slot 0 of gui is empty:
    message player "<gray>Slot is empty"

if gui inventory is empty:
    message player "<gray>Inventory is completely empty"

if gui inventory contains diamond:
    message player "<aqua>Found diamonds"

if inventory is a lumen inventory:
    message player "<green>This is a Lumen GUI"
```

## Click Handlers

Click handlers respond to player clicks in your inventory. They must be at root level (not nested inside other blocks).

### Slot Click

Handle a click on a specific slot:

```luma
slot 13 click in "punishments":
    message player "<red>Banned target player."
```

### Any Click

Handle any click in the inventory:

```luma
click in "punishments":
    message player "<gray>You clicked slot {slot}"
```

### Close Handler

```luma
close of "punishments":
    message player "<gray>Menu closed."
```

### Open Handler

```luma
open of "punishments":
    message player "<red>Viewing punishment options."
```

Close and open handlers have access to `player`, `world`, `inventory`, and `title`.

## Register Inventory

Register a named inventory builder that can be opened from anywhere in the script (or from other scripts).

```luma
register inventory "warp_menu":
    var gui = new inventory "warp_menu" with rows 3 titled "<dark_aqua><b>Warp Menu"

    var filler = new item gray_stained_glass_pane
    set filler's name to " "
    fill slots 0 to 8 of gui with filler
    fill slots 18 to 26 of gui with filler

    var spawn_icon = new item ender_pearl
    set spawn_icon's name to "<green>Spawn"
    set spawn_icon's lore to "<gray>Teleport to world spawn"
    set slot 11 of gui to spawn_icon

    var nether_icon = new item magma_cream
    set nether_icon's name to "<red>Nether Hub"
    set nether_icon's lore to "<gray>Teleport to the nether hub"
    set slot 13 of gui to nether_icon

    var end_icon = new item ender_eye
    set end_icon's name to "<dark_purple>The End"
    set end_icon's lore to "<gray>Teleport to the end portal"
    set slot 15 of gui to end_icon

    open gui for player
```

Open it from anywhere:

```luma
command warps:
    description "Open the warp menu"
    open inventory named "warp_menu" for player
```

The `player` variable is available inside the `register inventory` block because the builder is called with the player who triggered it.

## Full Example: Punishment Menu

```luma
register inventory "punish":
    var gui = new inventory "punish" with rows 5 titled "<red><b>Moderation"

    var border = new item red_stained_glass_pane
    set border's name to " "
    fill slots 0 to 8 of gui with border
    fill slots 36 to 44 of gui with border

    var mute_btn = new item paper
    set mute_btn's name to "<yellow>Mute"
    set mute_btn's lore to "<gray>Prevents the player from chatting|<yellow>Duration: 30 minutes"
    set slot 20 of gui to mute_btn

    var kick_btn = new item iron_door
    set kick_btn's name to "<gold>Kick"
    set kick_btn's lore to "<gray>Removes the player from the server|<gold>They can rejoin immediately"
    set slot 22 of gui to kick_btn

    var ban_btn = new item barrier
    set ban_btn's name to "<red>Ban"
    set ban_btn's lore to "<gray>Permanently removes the player|<dark_red>This action is irreversible (it deletes all data of the user)"
    set slot 24 of gui to ban_btn

    var close_btn = new item arrow
    set close_btn's name to "<white>Close"
    set slot 40 of gui to close_btn

    open gui for player

slot 20 click in "punish":
    close player's inventory
    # Mute user
    message player "<yellow>Player has been muted for 30 minutes."

slot 22 click in "punish":
    close player's inventory
    # Kick user
    message player "<gold>Player has been kicked."

slot 24 click in "punish":
    close player's inventory
    # Ban user
    message player "<red>Player has been banned."

slot 40 click in "punish":
    close player's inventory

command punish:
    permission "mod.punish"
    description "Open the punishment menu"
    open inventory named "punish" for player
```
