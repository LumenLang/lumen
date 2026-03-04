---
description: "Expressions produce values that can be stored, compared, or passed to statements."
---

# Expressions

An expression is something that produces a value. Expressions appear on the right side of variable assignments, inside conditions, and as arguments to statements.

```luma
var result = random between 1 and 100
var name = get args at index 0
var count = todos size
var upper = text to uppercase
var sword = new item diamond_sword
```

Everything after the `=` in each line above is an expression. It gets evaluated and the result is stored in the variable.

:::alert info
All available expressions, including addon provided ones, can be found on the [reference documentation](https://lumenlang.dev/expressions).
:::
