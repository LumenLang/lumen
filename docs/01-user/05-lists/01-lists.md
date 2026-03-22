---
description: "Working with lists: creating, adding, removing, looping, and typed lists."
---

# Lists

Lists are ordered collections of items. You can store text, numbers, or other values in a list, then add to it, remove from it, loop over it, and check its contents.

## Creating a List

```luma
var items = new list
```

For a global or stored list:

```luma
global stored var notes for ref type player default new list
```

This creates a per-player list that is saved across restarts, starting empty for each new player.

## Adding Items

Use `add ... to` to append an item:

```luma
var fruits = new list
add "apple" to fruits
add "banana" to fruits
add "cherry" to fruits
```

## Getting Items by Index

Retrieve an item at a specific position (indices start at 0):

```luma
var first = get fruits at index 0
var second = get fruits at index 1
```

## Size

```luma
var count = fruits size
message player "&eYou have {count} items."
```

## Checking if Empty

```luma
if notes is empty:
    message player "&7No notes yet."
```

## Checking if a List Contains a Value

```luma
if currencies contains "gold":
    message player "&eGold exists!"

if currencies does not contain "silver":
    message player "&7No silver."
```

## Removing Items

Remove by value (removes the first occurrence):

```luma
remove "banana" from fruits
```

Remove by index:

```luma
remove index 0 from fruits
```

Practical example where a player provides a 1-based number that needs converting to a 0-based index:

```luma
var num = get args at index 1
var idx = num as integer
subtract 1 from idx
remove index idx from notes
message player "&aRemoved note #{num}."
```

## Clearing

Remove all items at once:

```luma
clear notes
```

## Looping

You can loop over a list and remove items during iteration:

```luma
loop entry in warps:
    var name = get field "name" of entry
    if name is target:
        remove entry from warps
```

For more on looping syntax, including loop sources and maps, see the [Loops](../01-basics/05-loops.md) page.

## Typed Lists

Lists can hold custom data types. When defining a list for a specific type, use `new list of <type>`:

:::alert warning
Typed lists are usually recommended over untyped lists for safety, support for untyped lists may be removed in the future.
:::

```luma
data warp:
    name text
    x number
    y number
    z number

global stored var warps default new list of warp
```

You can then add data instances and loop over them, reading fields:

```luma
var entry = new warp with name "Spawn" x 0 y 64 z 0
add entry to warps

loop entry in warps:
    var warpname = get field "name" of entry
    message player "&e{warpname}"
```
