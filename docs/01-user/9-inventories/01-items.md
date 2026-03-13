---
description: "How to create items, modify their properties, give them to players, and check item conditions."
---

# Items

Items are created from materials and can have custom names, lore, durability, and more. All text properties support MiniColorize formatting.

For the full list of item patterns, see the reference pages:
- [Item Statements](https://lumenlang.dev/statements?cats=Item)
- [Item Expressions](https://lumenlang.dev/expressions?cats=Item)
- [Item Conditions](https://lumenlang.dev/conditions?cats=Item)

## Creating Items

```luma
var pick = new item diamond_pickaxe
var arrows = new item arrow 64
```

The material name matches Minecraft's material names (lowercase, underscores).

## Display Name

```luma
var pick = new item diamond_pickaxe
set pick's name to "<gradient:#4facfe:#00f2fe>Frostbite</gradient>"
```

Reading the name:

```luma
var name = get pick's name
message player "<gray>Item name: <white>{name}"
```

## Lore

Set multiple lore lines separated by `|`:

```luma
set pick's lore to "<gray>Mined from the frozen caves|<aqua>+5 Mining Speed|<dark_gray>Soulbound"
```

Add a single line:

```luma
add lore "<yellow>Cannot be traded" to pick
```

Clear all lore:

```luma
clear pick's lore
```

Reading lore:

```luma
var lines = get pick's lore
```

## Amount

```luma
set pick's amount to 3
var count = get pick's amount
```

## Material Type

```luma
set pick's type to netherite_pickaxe
var mat = get pick's type
```

## Durability

```luma
set pick's durability to 50
var dmg = get pick's durability
var max = get pick's max durability
```

## Unbreakable

```luma
make pick unbreakable
make pick breakable
```

## Custom Model Data

```luma
set pick's custom model data to 1001
var cmd = get pick's custom model data
```

## Giving Items

```luma
give player diamond 64
give player pick
```

## Item Conditions

```luma
if pick has name:
    message player "<green>This item has a custom name"

if pick has lore:
    message player "<green>This item has lore"

if pick has enchantments:
    message player "<green>This item is enchanted"

if pick is unbreakable:
    message player "<gold>This item is indestructible"
```
