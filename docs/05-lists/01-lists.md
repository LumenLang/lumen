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
global stored var todos for ref type player default new list
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
if todos is empty:
    message player "&7No tasks yet."
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
remove index idx from todos
message player "&aRemoved task #{num}."
```

## Clearing

Remove all items at once:

```luma
clear todos
```

## Looping

Iterate over every item:

```luma
loop item in todos:
    message player "&e- {item}"
```

With a counter for numbered output:

```luma
var i = 1
loop item in todos:
    message player "  &e{i}. &f{item}"
    add 1 to i
```

You can also remove items during a loop:

```luma
loop entry in arenas:
    var name = get field "name" of entry
    if name is target:
        remove entry from arenas
```

## Typed Lists

Lists can hold custom data types. When defining a list for a specific type, use `new list of <type>`:

:::alert info
In most cases you do not need to specify the type. Lumen figures it out automatically from the values you add, but it is usually recommended to do it.
:::

```luma
data arena:
    name text
    x1 number
    y1 number
    z1 number

global stored var arenas default new list of arena
```

You can then add data instances and loop over them, reading fields:

```luma
var entry = new arena with name "PvP Pit" x1 10 y1 60 z1 10
add entry to arenas

loop entry in arenas:
    var arenaname = get field "name" of entry
    message player "&e{arenaname}"
```
