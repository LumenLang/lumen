---
description: "Local variables created with var, scoping, and type conversion."
---

# Local Variables

Local variables are the simplest kind of variable in Lumen. They exist only inside the block where they are created and are gone once that block finishes executing.

## Creating Local Variables

Use `var` followed by a name and a value:

```luma
command greet:
    var name = "World"
    var count = 10
    var items = new list
    message player "&aHello, {name}! Count: {count}"
```

Each variable only lives within the `command greet:` block. Other commands and events cannot see it.

## Changing Values

Use `set ... to` to update a variable:

```luma
command example:
    var count = 0
    set count to 5
    message player "&eCount is {count}"
```

For numbers, you can also use `add ... to` and `subtract ... from`:

```luma
command counter:
    var count = 10
    add 5 to count
    subtract 3 from count
    message player "&eCount is now {count}"
```

After this, `count` is 12.

## Type Conversion

Sometimes you need to convert a value from one type to another. The most common case is converting a text argument to a number:

```luma
command remove:
    var num = get args at index 1
    var idx = num as integer
    subtract 1 from idx
```

The `as integer` expression converts the text `"3"` into the number `3`.

## Scope

Local variables are scoped to the block they are defined in. A variable created inside an `if` block is not available outside of it:

```luma
command test:
    var greeting = "Hello"

    if args size > 0:
        var name = get args at index 0
        message player "&a{greeting}, {name}!"

    # "name" is not available here, but "greeting" still is
    message player "&7{greeting}!"
```
