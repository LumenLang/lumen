---
description: "Basic arithmetic operations, in-place modification, and comparisons."
---

# Arithmetic

Lumen supports standard arithmetic for working with numbers.

## Basic Operations

```luma
command math:
    var a = 42
    var b = 8
    var sum = a + b
    var diff = a - b
    var product = a * b
    var quotient = a / b
    message player "a + b = &e{sum}"
    message player "a - b = &e{diff}"
    message player "a * b = &e{product}"
    message player "a / b = &e{quotient}"
```

## Modifying Variables In Place

Use `add ... to` and `subtract ... from` to change a number variable directly:

```luma
var count = 10
add 5 to count
subtract 3 from count
```

After this, `count` is 12.

You can also multiply:

```luma
multiply count by 2
```

These work with per-player variables too:

```luma
global var points for ref type player default 0

command earn:
    add 25 to points
    message player "&a+25 points! Total: {points}"
```

## Comparison Operators

Use these to compare numbers in conditions:

| Operator | Meaning |
|---|---|
| `==` | Equal to |
| `>` | Greater than |
| `<` | Less than |
| `>=` | Greater than or equal to |
| `<=` | Less than or equal to |

```luma
if score >= 100:
    message player "&aYou reached 100!"

if health <= 0:
    message player "&cYou are out of health!"
```

## Negative Numbers

Lumen does not have a negative sign, so express negatives with subtraction:

```luma
var neg = 0 - 15
```
