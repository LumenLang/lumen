---
description: "Working with maps: creating, setting, getting, removing, looping, and scoped maps."
---

# Maps

A map stores entries by name. Each entry is a key (always text) pointing to a value. This is useful for things like per player stats, where each stat has a name and a number.

## Creating a Map

```luma
set stats to new map
```

For a global or stored map:

```luma
global stored scoped stats with default new map
```

This creates a per player map that is saved across restarts, starting empty for each new player.

## Setting Values

```luma
set stats at key "kills" to 0
set stats at key "deaths" to 0
```

## Getting Values

```luma
set kills to get stats at key "kills"
message player "&eKills: {kills}"
```

## Checking if a Key Exists

```luma
if stats contains key "kills":
    message player "&aKills tracked."

if stats does not contain key "kills":
    message player "&cNo kills tracked."
```

## Checking if Empty

```luma
if stats is empty:
    message player "&7No stats yet."
```

## Removing an Entry

```luma
remove key "deaths" from stats
```

## Clearing

```luma
clear stats
```

## Looping

Loop over every key value pair in the map:

```luma
loop key val in stats:
    message player "&e{key}: {val}"
```

## Scoped Map Operations

When a map is declared as a scoped global (with `scoped`), you can operate on it directly using `for <scope>` without loading it into a local variable first. The scope must be a typed variable like `player` or another entity.

```luma
global stored scoped stats with default new map
```

```luma
set stats at key "kills" to 0 for player

set kills to get stats at key "kills" for player

if stats is empty for player:
    message player "&7No stats yet."

loop key val in stats for player:
    message player "&e{key}: {val}"

remove key "kills" from stats for player

clear stats for player
```
