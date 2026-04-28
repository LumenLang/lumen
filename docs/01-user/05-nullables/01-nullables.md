---
description: "Working with values that might be missing using nullable types."
---

# Nullables

Some values might not exist. A lookup for an offline player, a world that isn't loaded, an item slot that's empty. In
Lumen these are nullable values: they may hold a real value or they may hold `none`.

Every variable is either nullable or it isn't. The type system keeps them separate so a missing value can't silently
sneak into code that expects a real one.

## Declaring Nullables

Prefix a type with `nullable` to allow `none`:

```luma
global:
    last_killer: nullable player
    prize: nullable itemstack
```

Local variables follow the same rule:

```luma
set target to nullable player
```

A regular, non-nullable declaration rejects `none` outright. Stored variables that don't have a default must either
provide one or be declared nullable.

## Assigning none

Use `none` to clear a nullable:

```luma
set last_killer to none
```

## Checking Before Use

Before reading a nullable value, verify it exists:

```luma
if last_killer is set:
    message last_killer "You killed someone!"

if last_killer is not set:
    message player "Nobody has died yet."
```

Inside the `if last_killer is set:` body, the type system treats `last_killer` as non-nullable, so you can pass it to
anything that expects a player. This narrowing only applies inside the guarded branch.

## Asserting Non-null

When you're certain a nullable value is present and would rather fail loudly than branch, use `require`:

```luma
require last_killer or fail
message last_killer "You killed someone!"
```

`require` throws at runtime if the value is null, and narrows it to non-null for the rest of the block.

## Nullable Fields

Data class fields can be nullable too:

```luma
data reward:
    label text
    winner nullable player
```

The same rules apply: read through an `is set` check or a `require` before using the field as a non-null value.
