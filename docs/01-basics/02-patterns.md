---
description: "The three building blocks of Lumen: statements, expressions, and conditions."
---

# Expressions, Conditions, and Statements

Lumen scripts are made up of three fundamental building blocks: expressions, conditions, and statements. Understanding how they work will help you read and write scripts effectively.

## Statements

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

## Expressions

An expression is something that produces a value. Expressions appear on the right side of variable assignments, inside conditions, and as arguments to statements.

```luma
var result = random between 1 and 100
var name = get args at index 0
var count = todos size
var upper = text to uppercase
var sword = new item diamond_sword
```

Everything after the `=` in each line above is an expression. It gets evaluated and the result is stored in the variable.

Expressions can also be used directly inside statements:

```luma
message player "&eYour health is {player_health}"
give player diamond 3
```

## Conditions

Conditions produce a true or false result. They are used with `if`, `else if`, and other control flow blocks.

```luma
if args size < 1:
    message player "&cNot enough arguments!"

if killer is set:
    message killer "&aYou got a kill!"

if tag is not "none":
    broadcast "&7[{tag}] {player_name} joined!"
```

### Comparison Conditions

You can compare values using standard operators:

```luma
if count > 0:
if health <= 5:
if streak >= 10:
if score == 100:
```

### Equality Checks

Use `is` and `is not` to check if two values are equal:

```luma
if action is "add":
    message player "&aAdding!"

if block type is stone:
    message player "&7Stone block."

if tag is not "none":
    broadcast "&7Tagged player!"
```

### Set Checks

Check whether a variable has a value at all:

```luma
if killer is set:
    message killer "&aKill confirmed!"

if current_bounty is not set:
    message player "&7No active bounty."
```

### Chance

A probability check that succeeds a given percentage of the time:

```luma
if chance 10:
    message player "&dRare drop!"
```

### Combining Conditions

You can use `and` / `or` to combine multiple conditions on a single line:

```luma
if px >= ax1 and px <= ax2 and py >= ay1 and py <= ay2:
    message player "&eYou are inside the zone!"
```

## Control Flow

### If / Else If / Else

Branch your code based on conditions:

```luma
if streak >= 10:
    broadcast "&c{player_name} is on a 10+ kill streak!"
else if streak >= 5:
    broadcast "&e{player_name} is on a 5+ kill streak!"
else:
    message player "&7Keep going."
```