---
description: "Hot reload inventory designs in real time as you edit scripts, with instant visual feedback."
---

# Instant Reload of Inventories

When you modify and reload a Lumen script that opens an inventory, players who have that inventory open see the changes instantly without the inventory closing and reopening. This lets you experiment with designs, layouts, and content side-by-side, seeing your edits take effect immediately.

:::video https://s3api.vansencool.net/vansen/inventory_reload.mp4
:::

## How It Works

When a script defining a `register inventory` block is reloaded, Lumen checks if any players have that inventory open. If they do, instead of closing and reopening the GUI, the following updates are applied in place:

1. **Item replacement** - items in the inventory are replaced in place
2. **Title update** - the inventory title is updated
3. **No flicker** - the inventory stays open and updates smoothly
4. **Click handlers work fine** - any click handlers defined in the inventory work just fine

## Configuration

Toggle the feature in `config.yml` under the inventories section:

```yaml
features:
  inventories:
    hot-reload: true # enable (default)
```
