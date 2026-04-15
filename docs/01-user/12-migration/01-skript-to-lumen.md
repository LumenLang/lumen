# The Skript to Lumen Migration Guide

If you're coming from Skript, Lumen is going to feel familiar but also very different. I've been writing Skript for years, and the biggest thing to understand about Lumen is that it **isn't an interpreter**. It actually parses your `.luma` files and compiles them straight into native Java bytecode.

This means you get the performance of a real Java plugin without the overhead of Skript's runtime evaluation. But it also means the syntax is a bit more rigid. Here's a quick guide on how to translate your Skript knowledge into Lumen.

## 1. File Structure and Indentation

Lumen relies heavily on indentation (like Python). You don't use `trigger:` blocks for commands anymore. Everything just flows from the colon `:`.

**Skript:**
```applescript
command /heal:
    trigger:
        heal the player
```

**Lumen:**
```luma
command heal:
    heal player
```

## 2. Variables and Scope

This is probably the biggest change. In Skript, `{var}` is global and persistent by default, while `{_var}` is local. Lumen requires you to be explicit about scope and persistence.

**Local Variables** (Only exist in the current block)
```luma
set name to "Steve"
```

**Global Variables** (Shared across the script, reset on restart)
```luma
global multiplier with default 2.0
```

**Persistent Variables** (Saved to disk, survive restarts)
```luma
global stored total_joins with default 0
```

**Per-Player Variables** (Scoped to a specific player)
```luma
global stored scoped coins with default 0

command earn:
    add 10 to coins for player
```

## 3. Lists and Maps (Dictionaries)

Skript uses list variables like `{items::*}`. Lumen uses explicit `list` and `map` types, which are much cleaner to work with.

**Lists:**
```luma
set fruits to new list
add "apple" to fruits
remove "apple" from fruits
```

**Maps (Key-Value):**
```luma
set stats to new map
set stats at key "kills" to 10
set k to get stats at key "kills"
```

## 4. Custom Data Types (Data Classes)

If you've ever tried to store complex objects in Skript using `{data::%uuid%::name}`, you'll love Lumen's `data` classes.

```luma
data warp:
    name text
    x number
    y number
    z number

global stored warps with default new list of warp

command setwarp:
    set w to new warp with name "Spawn" x 0 y 64 z 0
    add w to warps
```

## 5. GUIs / Inventories

Lumen has a built-in inventory system that supports instant hot-reloading. You don't need addons like TuSKe or Skript-GUI.

**Lumen GUI Example:**
```luma
register inventory "menu":
    set gui to new inventory "menu" with rows 3 titled "Menu"
    
    set icon to new item diamond
    set icon's name to "Click me"
    set slot 13 of gui to icon
    
    open gui for player

slot 13 click in "menu":
    message player "You clicked the diamond!"
    close player's inventory

command menu:
    open inventory named "menu" for player
```

## 6. Raw Java Integration

If you need to do something Lumen doesn't support yet, you don't need `skript-reflect`. You can literally just write Java code in your script (if enabled in config).

```luma
import org.bukkit.Bukkit

command online:
    java:
        int count = Bukkit.getOnlinePlayers().size();
        player.sendMessage("Total online: " + count);
```

## Quick Syntax Reference

| What you want to do | Skript | Lumen |
|---------------------|--------|-------|
| **Command** | `command /name:` | `command name:` |
| **Event** | `on join:` | `on join:` |
| **Local Var** | `set {_var} to 5` | `set var to 5` |
| **Persistent Var** | `set {global} to 5` | `global stored var with default 5` |
| **String Variables**| `"Hello %player%"` | `"Hello {player_name}"` |
| **Send Message** | `send "hi" to player` | `message player "hi"` |
| **Broadcast** | `broadcast "hi"` | `broadcast "hi"` |
| **Math** | `add 1 to {_x}` | `add 1 to x` |

Note: Lumen is actively adding Skript-style aliases for common patterns, so some of these may accept Skript syntax directly in future versions.
