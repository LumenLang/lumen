---
description: "Define custom structured types with named, typed fields."
---

# Data Classes

Data classes group related fields into a single type. Use them for structured data like warp points, rewards, or stat
trackers.

```luma
data reward:
    label text
    amount number
    given_by text
```

Field types accept any type from: https://lumenlang.dev/types

## Creating Instances

```luma
set r to new reward
set r to new reward with label "Diamond" amount 500 given_by "Server"
```

Field values accept variables and placeholders:

```luma
set r to new reward with label "{player_name}" amount score given_by "Game"
```

## Reading and Writing Fields

Two syntaxes read the same field:

```luma
set name to get field "label" of r
set name to r field "label"
```

Write with `set field`:

```luma
set field "amount" of r to 1000
```
