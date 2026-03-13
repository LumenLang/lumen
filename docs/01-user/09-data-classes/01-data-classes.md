---
description: "How to define data classes, create instances, access fields, and use them with lists and maps."
---

# Data Classes

Data classes let you define custom types with named, typed fields. They are useful for grouping related values together, like an arena with coordinates, a bounty with a target and reward, or any structured data your script needs.

## Defining a Data Class

A data class is defined with the `data` keyword followed by the type name, then a colon. Each line inside defines a field with a name and a type.

```luma
data bounty:
    target text
    reward number
    placed_by text
```

This creates a data type called `bounty` with three fields.

### Field Types

| Type      | Aliases                              | What it stores               |
|-----------|--------------------------------------|------------------------------|
| `text`    | `string`, `str`                      | Text values                  |
| `number`  | `num`, `double`, `float`, `decimal`  | Decimal numbers (e.g. 3.14)  |
| `integer` | `int`                                | Whole numbers (e.g. 42)      |
| `boolean` | `bool`                               | `true` or `false`            |
| `any`     | `object`                             | Any value                    |

You can use whichever alias you prefer. For example, `text` and `string` are the same.

## Creating Instances

Use `new` followed by the data type name to create an instance.

```luma
var b = new bounty
```

This creates a bounty with all fields empty.

### Setting Fields on Creation

Use `with` to set fields when creating the instance. Fields are written as `fieldName value` pairs.

```luma
var b = new bounty with target "Player1" reward 500 placed_by "Player5"
```

You can pass variables as values too:

```luma
var name = get args at index 0
var amount = get args at index 1
var b = new bounty with target name reward amount placed_by "{player_name}"
```

## Accessing Fields

There are two ways to read a field from a data instance.

### Prefix Syntax

```luma
var name = get field "target" of b
var reward = get field "reward" of b
```

### Postfix Syntax

```luma
var name = b field "target"
```

Both syntaxes return the same result. Use whichever reads better in context.

## Setting Fields

Use `set field` to change a field on an existing instance.

```luma
set field "reward" of b to 1000
set field "target" of b to "Player2"
```

## Using Data Classes with Lists

You can create typed lists that hold data instances. This is one of the most powerful features of data classes.

### Typed Lists

```luma
data arena:
    name text
    x1 number
    y1 number
    z1 number
    x2 number
    y2 number
    z2 number

global stored var arenas default new list of arena
```

The `new list of arena` creates a list that knows it contains `arena` instances. This means when you loop over it, the loop variable automatically has access to field operations.

### Adding to a List

```luma
var entry = new arena with name "Arena1" x1 10 y1 60 z1 10 x2 100 y2 80 z2 100
add entry to arenas
```

### Looping and Accessing Fields

```luma
loop entry in arenas:
    var arenaname = get field "name" of entry
    var x1 = get field "x1" of entry
    var y1 = get field "y1" of entry
    var z1 = get field "z1" of entry
    message player "&e{arenaname} &7at ({x1}, {y1}, {z1})"
```

### Removing from a List

```luma
loop entry in arenas:
    var arenaname = get field "name" of entry
    if arenaname is "PvP Pit":
        remove entry from arenas
```

### Checking if Empty

```luma
if arenas is empty:
    message player "&7No arenas defined."
```

## Using Data Classes with Maps

Maps store key-value pairs. You can store data instances as values in a map.

### Creating a Map

```luma
global stored var stats for ref type player default new map
```

### Storing and Retrieving Values

```luma
set stats at key "kills" to 0
var kills = get stats at key "kills"
```

### Per-player Maps

Maps can be scoped per player using `for`:

```luma
set stats at key "kills" to kills for killer
var kills = get stats at key "kills" for killer
```

### Removing Keys

```luma
remove key "kills" from stats
remove key "kills" from stats for player
```

### Looping Through a Map

```luma
loop key val in stats:
    message player "&f{key}: &e{val}"
```

### Getting Keys and Values

```luma
var allKeys = keys of stats
var allValues = values of stats
```

### Map Size

```luma
var count = stats size
```

## Persistence

Data instances, lists, and maps can all be persisted across server restarts using `global stored var`.

```luma
# single data instance
global stored var current_bounty default none

# list of data
global stored var arenas default new list of arena

# per-player map
global stored var stats for ref type player default new map
```

Changes made to stored variables are saved automatically.
