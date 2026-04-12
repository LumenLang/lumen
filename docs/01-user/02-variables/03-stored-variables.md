---
description: "Stored variables survive server restarts and script reloads by persisting to disk."
---

# Stored Variables

Regular globals are lost when the server stops or the script reloads. Adding `stored` makes a variable persistent.

## Declaring Stored Globals

Add `stored` before the rest of the declaration:

```luma
global:
    stored total_joins: int with default 0

on join:
    add 1 to total_joins
    message player "You are join #{total_joins}!"
```

`total_joins` keeps its value across restarts.

## Combining Stored and Scoped

`stored` and `scoped to` work together:

```luma
global:
    stored scoped to player deaths: int with default 0
    stored scoped to player streak: int with default 0

on entity_death:
    if killer is set:
        add 1 to streak for killer

on player_death:
    add 1 to deaths for player
    set streak to 0 for player
```

Each player gets their own values, and those values survive restarts.

## Stored Collections

Lists and maps can be stored too:

```luma
global:
    stored notes: list of string
    stored scoped to player achievements: list of string
```
