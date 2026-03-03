---
description: "The & color code format for colors and text styling."
---

# Color Codes

Lumen supports the `&` color code format that is widely used in Minecraft servers. These codes work in any text that Lumen sends to players, including messages, titles, action bars, item names, and lore.

## Colors

| Code | Color |
|---|---|
| `&0` | Black |
| `&1` | Dark Blue |
| `&2` | Dark Green |
| `&3` | Dark Aqua |
| `&4` | Dark Red |
| `&5` | Dark Purple |
| `&6` | Gold |
| `&7` | Gray |
| `&8` | Dark Gray |
| `&9` | Blue |
| `&a` | Green |
| `&b` | Aqua |
| `&c` | Red |
| `&d` | Light Purple |
| `&e` | Yellow |
| `&f` | White |

## Formatting

| Code | Style |
|---|---|
| `&l` | **Bold** |
| `&o` | *Italic* |
| `&n` | Underline |
| `&m` | Strikethrough |
| `&k` | Obfuscated (scrambled text) |
| `&r` | Reset all formatting |

## Combining Codes

You can chain multiple codes together. For example, `&6&l` makes bold gold text:

```luma
message player "&6&lGold Bold &r&7then gray normal"
```

Always place color codes before formatting codes. Use `&r` to reset all formatting before starting a new style.

## Usage in Scripts

Color codes work anywhere text is displayed:

```luma
command welcome:
    send title "&6&lWelcome!" with subtitle "&eEnjoy your stay" to player
    send actionbar "&a+Speed +Regen" to player
    message player "&7Use /help to get started."
```

## Enabling and Disabling

Color codes are enabled by default. You can toggle them in the Lumen `config.yml`:

```yaml
features:
  use-legacy-colors: true
```

When enabled, `&` codes are automatically translated into formatted text before being sent to the player. They can be used alongside MiniColorize tags in the same string.
