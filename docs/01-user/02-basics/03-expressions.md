---
description: "Expressions produce values that can be stored, compared, or passed to statements."
---

# Expressions

An expression is something that produces a value. Expressions appear on the right side of variable assignments, inside conditions, and as arguments to statements.

```luma
set result to random between 1 and 100
set name to get args at index 0
set count to todos size
set upper to text to uppercase
set sword to new item diamond_sword
```

Everything after `to` in each line above is an expression. It gets evaluated and the result is stored in the variable.

:::alert info
All available expressions, including addon provided ones, can be found on the [reference documentation](https://lumenlang.dev/expressions).
:::
