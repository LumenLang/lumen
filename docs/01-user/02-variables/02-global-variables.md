---
description: "Global variables are shared across an entire script using a typed declaration block."
---

# Global Variables

Global variables are accessible from everywhere in a script. They persist for as long as the script is loaded. You declare them in a `global:` block with explicit types.

## Declaring Globals

```luma
global:
    score: int with default 0
    prefix: string with default "&7[Server]"
    active: boolean with default false
```

Each line follows the syntax:

```
<name>: <type> [with default <value>]
```

The type can be any type recognized by Lumen. For a full list, see the [reference documentation](https://lumenlang.dev/types).

## Using Globals

Once declared, globals are available in any block in the script:

```luma
global:
    join_count: int with default 0

on join:
    add 1 to join_count

command joins:
    message player "Total joins: {join_count}"
```

## Nullable Types

Prefix any type with `nullable` to allow it to hold no value:

```luma
global:
    target: nullable player

on join:
    set target to player

command clear_target:
    set target to none
```

Nullable types default to `null` automatically, so `with default` is not needed. Non-nullable types without a collection default require an explicit `with default`.

## Scoped Globals

A normal global is server-wide. All uses see the same value. `scoped to <type>` creates a separate value per entity:

```luma
global:
    scoped to player streak: int with default 0

on entity_death:
    if killer is set:
        add 1 to streak for killer
        set ks to get streak for killer
        message killer "&aKill streak: {ks}"

on player_death:
    set streak to 0 for player
```

Scoped variables need `for <entity>` when you read or write them:

```luma
set streak to 5 for player
set ks to get streak for player
add 1 to streak for killer
delete streak for player
```

The scope type can be pluralized (`scoped to players` is the same as `scoped to player`).

## Collections as Globals

Lists and maps get automatic empty defaults, so `with default` is optional:

```luma
global:
    names: list of string
    scores: map of string to int
```

See the [Lists](../05-collections/01-lists.md) and [Maps](../05-collections/02-maps.md) docs for how to use collection operations.

:::alert info
Global variables reset when the script reloads. For data that survives restarts, see [Stored Variables](03-stored-variables.md).
:::
