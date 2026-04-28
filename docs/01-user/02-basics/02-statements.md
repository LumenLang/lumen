---
description: "Statements are the action lines in a Lumen script."
---

# Statements

A statement is a line that performs an action. Most lines in a Lumen script are statements.

```luma
message player "&aHello!"
broadcast "&eServer announcement!"
give player diamond 3
apply "speed" 1 to player for 200 ticks
set tag to "warrior"
add 1 to count
```

Each of these does something: sends a message, gives an item, applies an effect, sets a value. Statements form the core of your script logic.

## How Statements Work

Statements are matched against registered patterns. Lumen finds the best matching pattern and executes the associated logic. You do not need to worry about how this works internally, just write the statement and Lumen handles the rest.

Some statements take simple values like text or numbers. Others accept expressions (values that are computed at runtime).

:::alert info
All available statements, including addon provided ones, can be found on the [reference documentation](https://lumenlang.dev/statements).
:::
