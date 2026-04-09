---
description: "Working with lists: creating typed lists, adding, removing, looping, and scoped operations."
---

# Lists

Lists are ordered, typed collections. Every list is declared with a type so the compiler knows what kind of elements it holds.

## Creating a Typed List

Use `new list of <type>` to create a list with a known element type:

```luma
set names to new list of string
set scores to new list of int
```

For a global or stored list:

```luma
global stored warps with default new list of warp
```

This creates a list that is saved across restarts, starting empty, and only accepts `warp` entries.

## Adding Items

Use `add ... to` to append an item:

```luma
set fruits to new list of string
add "apple" to fruits
add "banana" to fruits
add "cherry" to fruits
```

Adding the wrong type to a typed list will produce a parse time error. For example, adding something that is not a `warp` to a list declared as `new list of warp` will be rejected.

## Getting Items by Index

Retrieve an item at a specific position (indices start at 0):

```luma
set first to get fruits at index 0
set second to get fruits at index 1
```

## Size

```luma
set count to fruits size
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

Practical example where a player provides a 1 based number that needs converting to a 0 based index:

```luma
set num to get args at index 1
set idx to num as integer
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
    set name to get field "name" of entry
    if name is target:
        remove entry from warps
```

For more on looping syntax, including loop sources and maps, see the [Loops](../01-basics/05-loops.md) page.

## Using Typed Lists with Data Classes

When combining typed lists with data classes, the loop variable automatically inherits the element type. This means field access works without any extra annotations:

```luma
data warp:
    name text
    x number
    y number
    z number

global stored warps with default new list of warp
```

```luma
set entry to new warp with name "Spawn" x 0 y 64 z 0
add entry to warps

loop entry in warps:
    set warpname to get field "name" of entry
    message player "&e{warpname}"
```

## Scoped List Operations

When a list is declared as a scoped global (with `scoped`), you can operate on it directly using `for <scope>` without loading it into a local variable first.

```luma
global stored scoped todos with default new list of string
```

```luma
add "Buy milk" to todos for player

set count to todos size for player

if todos is empty for player:
    message player "&7You have nothing to do."

loop item in todos for player:
    message player "&e- {item}"

remove "Buy milk" from todos for player

remove index 0 from todos for player

clear todos for player
```
