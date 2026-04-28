---
description: "Lists hold ordered collections of values that can be added to, removed from, and iterated."
---

# Lists

A list holds an ordered sequence of values. You declare typed lists in a `global:` block:

```luma
global:
    fruits: list of string
    scores: list of int
```

Lists start empty by default.

## Adding and Removing

```luma
add "apple" to fruits
add "banana" to fruits
remove "apple" from fruits
```

You can also remove by index (zero based):

```luma
remove index 0 from fruits
```

To replace a value at a specific position:

```luma
set fruits at index 0 to "cherry"
```

To remove everything:

```luma
clear fruits
```

## Reading Values

Get an item by index:

```luma
set first to get fruits at index 0
```

Get the number of items:

```luma
set count to fruits size
set count to size of fruits
```

Find where a value appears:

```luma
set pos to fruits index of "banana"
```

## Conditions

```luma
if fruits contains "apple":
    message player "Has apple"

if fruits is empty:
    message player "No fruits"
```

## Looping

```luma
loop fruit in fruits:
    message player "&e- {fruit}"
```

See [Loops](../02-basics/05-loops.md) for more on loop syntax.

## Scoped Lists

When a list is `scoped to player` (or another entity), every operation needs `for <entity>`:

```luma
global:
    scoped to player inventory_log: list of string

on interact:
    add "used item" to inventory_log for player

command mylog:
    loop entry in inventory_log for player:
        message player "&7- {entry}"
```

All list operations support the `for <entity>` suffix when working with scoped lists.
