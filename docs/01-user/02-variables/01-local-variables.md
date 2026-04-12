---
description: "Local variables store temporary values inside a block using the set statement."
---

# Local Variables

Local variables hold values inside the block where they are created. When the block finishes, the variable is gone.

## Creating and Assigning

Use `set` to create a variable:

```luma
on join:
    set greeting to "Welcome!"
    message player greeting
```

The variable `greeting` lives only inside this event handler. You can also assign expressions:

```luma
on join:
    set hp to player health
    set roll to random between 1 and 100
    message player "&aYour health is {hp}, and you rolled {roll}."
```

## Reassignment

Assigning again with `set` changes the value. The type cannot change after the first assignment:

```luma
command test:
    set score to 0
    set score to 10
```

## Modifying Numbers

Numeric variables can be changed in place:

```luma
command counter:
    set count to 10
    add 5 to count
    subtract 3 from count
    multiply count by 2
    divide count by 4
```

## Block Scoping

A variable only exists inside the block that created it:

```luma
command test:
    set x to 10
    if x > 5:
        set y to 20
        message player "y = {y}"
    # y does not exist here, x still does
    message player "x = {x}"
```

## Inserting into Text

Wrap the variable name in curly braces inside a string:

```luma
on join:
    set name to player_name
    message player "&aHello, {name}!"
```

## Nullable Variables

Prefix any type with `nullable` to declare a variable that may hold no value:

```luma
command example:
    set target to nullable player
    set label to nullable string
```

Both `target` and `label` start as `null`. You can also provide an initial value as an expression after the type:

```luma
command example:
    set target to nullable player get player by name "Notch"
    set label to nullable string "default label"
```

Nullable collections automatically default to an empty instance instead of `null`:

```luma
command example:
    set items to nullable list of string
    set scores to nullable map of string to int
```

Here `items` starts as a new empty list and `scores` starts as a new empty map. To override this and start as `null`, use `none`:

```luma
command example:
    set items to nullable list of string none
    set scores to nullable map of string to int none
```

All complex types are supported, including nested types:

```luma
command example:
    set data to nullable map of string to int
    set entries to nullable list of player
    set nested to nullable list of nullable string
```

## Type Mismatches

Once a variable is declared, its type is locked. Assigning an incompatible value produces a compile error:

```luma
command example:
    set count to 10
    set count to "hello"  # error: cannot assign string to int
```

## None and Non-Nullable Variables

Only nullable variables can hold `none`. Setting a regular variable to `none` is a compile error:

```luma
command example:
    set count to 10
    set count to none  # error: cannot assign 'none' to non-nullable variable
```

To clear a value, declare the variable as nullable:

```luma
command example:
    set target to nullable player get player by name "Notch"
    set target to none  # works, target is nullable
```
