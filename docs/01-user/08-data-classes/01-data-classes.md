---
description: "How to define data classes, create instances, access fields, and use them with lists and maps."
---

# Data Classes

Data classes let you define custom types with named, typed fields. They are useful for grouping related values together, like a warp point with coordinates, a reward with a label and amount, or any structured data your script needs.

## Defining a Data Class

A data class is defined with the `data` keyword followed by the type name, then a colon. Each line inside defines a field with a name and a type.

```luma
data reward:
    label text
    amount number
    given_by text
```

This creates a data type called `reward` with three fields.

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
var r = new reward
```

This creates a reward with all fields empty.

### Setting Fields on Creation

Use `with` to set fields when creating the instance. Fields are written as `fieldName value` pairs.

```luma
var r = new reward with label "Diamond Package" amount 500 given_by "Server"
```

You can pass variables as values too:

```luma
var name = get args at index 0
var value = get args at index 1
var r = new reward with label name amount value given_by "{player_name}"
```

## Accessing Fields

There are two ways to read a field from a data instance.

### Prefix Syntax

```luma
var name = get field "label" of r
var amount = get field "amount" of r
```

### Postfix Syntax

```luma
var name = r field "label"
```

Both syntaxes return the same result. Use whichever reads better in context.

## Setting Fields

Use `set field` to change a field on an existing instance.

```luma
set field "amount" of r to 1000
set field "label" of r to "Gold Package"
```

## Using Data Classes with Lists

You can create typed lists that hold data instances. This is one of the most powerful features of data classes.

### Typed Lists

```luma
data warp:
    name text
    x number
    y number
    z number

global stored var warps default new list of warp
```

The `new list of warp` creates a list that knows it contains `warp` instances. This means when you loop over it, the loop variable automatically has access to field operations.

### Adding to a List

```luma
var entry = new warp with name "Spawn" x 0 y 64 z 0
add entry to warps
```

### Looping and Accessing Fields

```luma
loop entry in warps:
    var warpname = get field "name" of entry
    var x = get field "x" of entry
    var y = get field "y" of entry
    var z = get field "z" of entry
    message player "&e{warpname} &7at ({x}, {y}, {z})"
```

### Removing from a List

```luma
loop entry in warps:
    var warpname = get field "name" of entry
    if warpname is "Spawn":
        remove entry from warps
```

### Checking if Empty

```luma
if warps is empty:
    message player "&7No warps defined."
```

## Using Data Classes with Maps

Maps store key-value pairs. You can store data instances as values in a map.

### Creating a Map

```luma
global stored var stats for ref type player default new map
```

### Storing and Retrieving Values

```luma
set stats at key "score" to 0
var score = get stats at key "score"
```

### Per-player Maps

Maps can be scoped per player using `for`:

```luma
set stats at key "score" to 100 for player
var score = get stats at key "score" for player
```

### Removing Keys

```luma
remove key "score" from stats
remove key "score" from stats for player
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
global stored var active_reward default none

# list of data
global stored var warps default new list of warp

# per-player map
global stored var inventory for ref type player default new map
```

Changes made to stored variables are saved automatically.
