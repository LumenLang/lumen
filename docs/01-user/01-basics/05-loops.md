---
description: "Looping over lists, maps, and loop sources like all players or all worlds."
---

# Loops

Loops let you iterate over collections of items and run code for each one. Lumen supports looping over lists, maps, and loop sources.

## Looping Over Lists

Use `loop <var> in <list>:` to iterate over every item in a list:

```luma
set fruits to new list
add "apple" to fruits
add "banana" to fruits
add "cherry" to fruits

loop fruit in fruits:
    message player "&e- {fruit}"
```

The variable `fruit` takes on each value in order. The block runs once per item.

## Looping Over Maps

For maps, you get both the key and the value:

```luma
loop key val in stats:
    message player "  &f{key}: &e{val}"
```

Each iteration gives you one key and value pair from the map.

## Loop Sources

Loop sources are built-in iterables that you use with the same `loop ... in ...` syntax. Instead of a variable, you write a source name after `in`.

### All Players

Iterate over every online player on the server:

```luma
command listplayers:
    loop p in all players:
        message player "&e- {p}"
```

`all online players` works the same way.

### All Entities in a World

Iterate over every entity in a specific world:

```luma
command countentities:
    loop e in all entities in world:
        message player "&7- Entity found"
```

The `world` at the end is the world expression. You could also use a variable that holds a world.

### All Worlds

Iterate over every loaded world on the server:

```luma
command worlds:
    loop w in all worlds:
        message player "&a- {w}"
```

:::alert info
Addons can register their own loop sources, so more may be available depending on what is installed. Check the [reference documentation](https://lumenlang.dev/loops) for a full list.
:::
