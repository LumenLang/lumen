---
description: "How to read, modify, and check entity attributes such as max health, movement speed, and attack damage."
---

# Attributes

Attributes are properties on living entities that control stats like health, speed, armor, and more. Lumen provides statements, expressions, and conditions for working with them.

Attribute names are flexible. You can write `max_health`, `max health`, `max-health`, or even the full Bukkit enum name like `GENERIC_MAX_HEALTH`. Lumen resolves the correct version automatically.

For the full list of attribute patterns, see the reference pages:
- [Attribute Statements](https://lumenlang.dev/statements?cats=Attribute)
- [Attribute Expressions](https://lumenlang.dev/expressions?cats=Attribute)
- [Attribute Conditions](https://lumenlang.dev/conditions?cats=Attribute)

## Setting Attributes

```luma
set mob's max_health to 40
set player's movement_speed to 0.3
set player's attack_damage to 15
```

## Adding to Attributes

```luma
add 10 to mob's max_health
add 0.1 to player's movement_speed
```

## Resetting Attributes

```luma
reset mob's max_health
reset player's attack_speed
```

This sets the attribute back to its default value.

## Reading Attributes

Get the base value (before modifiers):

```luma
set hp to get mob's max_health
message player "Base health: {hp}"
```

Get the effective value (with all modifiers applied):

```luma
set totalHp to get mob's max_health effective
message player "Effective health: {totalHp}"
```

Get the default value:

```luma
set defaultHp to get mob's max_health default
message player "Default health: {defaultHp}"
```

## Conditions

Compare an attribute value:

```luma
if mob's max_health > 20:
    message player "This mob is tanky!"
```

Check whether an entity has an attribute:

```luma
if mob has attack_damage:
    message player "This mob can deal damage."
```

Check whether an entity lacks an attribute:

```luma
if mob lacks attack_damage:
    message player "This mob cannot deal damage."
```

## Version Compatibility

Some attributes were added in later versions. If your server is older than the version an attribute was introduced in, it will not be available.

## Available Attributes

### Since 1.20

| Name |
|------|
| `max_health` |
| `follow_range` |
| `knockback_resistance` |
| `movement_speed` |
| `flying_speed` |
| `attack_damage` |
| `attack_knockback` |
| `attack_speed` |
| `armor` |
| `armor_toughness` |
| `luck` |
| `jump_strength` |
| `spawn_reinforcements` |

### Since 1.20.5

| Name |
|------|
| `max_absorption` |
| `scale` |
| `step_height` |
| `gravity` |
| `safe_fall_distance` |
| `fall_damage_multiplier` |
| `block_interaction_range` |
| `entity_interaction_range` |
| `block_break_speed` |

### Since 1.21

| Name |
|------|
| `burning_time` |
| `explosion_knockback_resistance` |
| `mining_efficiency` |
| `movement_efficiency` |
| `oxygen_bonus` |
| `sneaking_speed` |
| `submerged_mining_speed` |
| `sweeping_damage_ratio` |
| `water_movement_efficiency` |
| `tempt_range` |
