---
description: "Built-in math functions"
---

# Math Functions

Lumen provides several built-in math functions for common operations.

## Min and Max

Find the smaller or larger of two values:

```luma
set a to 42
set b to 8
set smallest to min of a and b
set largest to max of a and b
```

`smallest` is 8 and `largest` is 42.

## Absolute Value

Get the non-negative version of a number:

```luma
set neg to 0 - 15
set absolute to abs of neg
```

`absolute` is 15.

## Clamp

Restrict a value to a specific range. Values below the minimum become the minimum, values above the maximum become the maximum:

```luma
set clamped to clamp 150 between 0 and 100
```

`clamped` is 100, since 150 exceeds the upper bound.

## Random Numbers

Generate a random number within a range (both are inclusive):

```luma
command roll:
    set result to random between 1 and 100
    broadcast "&e{player_name} rolled &6{result}&f!"
```

## Chance

A probability-based condition. The value is a percentage (0 to 100):

```luma
if chance 10:
    message player "&dLegendary drop!"
else if chance 40:
    message player "&9Rare drop!"
else:
    message player "&7Common drop."
```

There is a 10% chance for the legendary message. If that fails, a 40% chance for rare. Otherwise, common.