---
description: "Local variables, scoping, and type conversion."
---

# Local Variables

Local variables are the simplest kind of variable in Lumen. They exist only inside the block where they are defined and are gone once that block finishes executing.

## Declaring and Updating Variables

Use `set X to <value>` to declare a variable or update an existing one. The same statement works for both:

```luma
command greet:
    set name to "World"
    set count to 10
    set items to new list
    message player "&aHello, {name}! Count: {count}"
```

Each variable only lives within the `command greet:` block. Other commands and events cannot see it.

```luma
command example:
    set count to 0
    set count to 5
    message player "&eCount is {count}"
```

For numbers, you can also use `add ... to` and `subtract ... from`:

```luma
command counter:
    set count to 10
    add 5 to count
    subtract 3 from count
    message player "&eCount is now {count}"
```

After this, `count` is 12.

## Type Conversion

Sometimes you need to convert a value from one type to another. The most common case is converting a text argument to a number:

```luma
command remove:
    set num to get args at index 1
    set idx to num as integer
    subtract 1 from idx
```

The `as integer` expression converts the text `"3"` into the number `3`.

## Scope

Local variables are scoped to the block they are defined in. A variable created inside an `if` block is not available outside of it:

```luma
command test:
    set greeting to "Hello"

    if args size > 0:
        set name to get args at index 0
        message player "&a{greeting}, {name}!"

    # "name" is not available here, but "greeting" still is
    message player "&7{greeting}!"
```
