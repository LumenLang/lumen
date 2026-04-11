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
