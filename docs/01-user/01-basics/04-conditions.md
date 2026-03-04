---
description: "Conditions produce a true or false result for use in if blocks and other control flow."
---

# Conditions

Conditions produce a true or false result. They are used with `if`, `else if`, and other control flow blocks to decide which code runs.

```luma
if args size < 1:
    message player "&cNot enough arguments!"
```

## Comparison

Compare values using operators. Both symbols and English text are supported:

| Symbols | English | Meaning |
|---|---|---|
| `==` | `equals`, `is`, `equal to` | Equal to |
| `!=` | `not equal`, `not equal to`, `is not`, `is not equal to` | Not equal to |
| `<` | `less than`, `is less than` | Less than |
| `>` | `greater than`, `is greater than` | Greater than |
| `<=` | `less than or equal to`, `is less than or equal to` | Less than or equal to |
| `>=` | `greater than or equal to`, `is greater than or equal to` | Greater than or equal to |

You can use either form. These are all equivalent:

```luma
if count > 0:
    message player "&aCount is positive."

if count is greater than 0:
    message player "&aCount is positive."
```

Use whichever is more readable to you:

```luma
if health <= 5:
    message player "&cLow health!"

if health is less than or equal to 5:
    message player "&cLow health!"
```

## Equality Checks

Use `is` and `is not` to check if two values are equal:

```luma
if action is "add":
    message player "&aAdding!"

if tag is not "none":
    broadcast "&7Tagged player!"
```

## Set Checks

Check whether a variable has a value at all:

```luma
if killer is set:
    message killer "&aKill confirmed!"

if current_bounty is not set:
    message player "&7No active bounty."
```

## Combining Conditions

Use `and` / `or` to combine multiple conditions on a single line:

```luma
if px >= ax1 and px <= ax2 and py >= ay1 and py <= ay2:
    message player "&eYou are inside the zone!"
```

:::alert info
All available conditions, including addon provided ones, can be found on the [reference documentation](https://lumenlang.dev/conditions).
:::
