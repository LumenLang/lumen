---
description: "Maps store key value pairs, allowing lookup, iteration, and modification by key."
---

# Maps

A map stores key value pairs. You declare typed maps in a `global:` block:

```luma
global:
    nicknames: map of string to string
    player_scores: map of string to int
```

Maps start empty by default.

## Setting and Removing

```luma
set nicknames at key "Steve" to "Builder"
set nicknames at key "Alex" to "Explorer"
remove key "Steve" from nicknames
```

To remove everything:

```luma
clear nicknames
```

## Reading Values

Get a value by its key:

```luma
set nick to get nicknames at key "Alex"
```

Get the number of entries:

```luma
set count to nicknames size
set count to size of nicknames
```

Get all keys or all values as lists:

```luma
set all_keys to keys of nicknames
set all_values to values of nicknames
```

## Conditions

```luma
if nicknames contains key "Alex":
    message player "Alex has a nickname"

if nicknames is empty:
    message player "No nicknames set"
```

## Looping

Loop over a map to get each key and value:

```luma
loop name nick in nicknames:
    message player "&e{name} &7-> &f{nick}"
```

See [Loops](../02-basics/05-loops.md) for more on loop syntax.

## Scoped Maps

When a map is `scoped to player` (or another entity), every operation needs `for <entity>`:

```luma
global:
    scoped to player settings: map of string to string

command setpref:
    set settings at key "color" to "red" for player

command showpref:
    set color to get settings at key "color" for player
    message player "&7Your color: {color}"
```

All map operations support the `for <entity>` suffix when working with scoped maps.
