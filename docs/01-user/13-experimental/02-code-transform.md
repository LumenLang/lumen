---
description: "Run code transformers on the generated Java source after emission."
tags: "experimental"
---

# Code Transform

Code transform is an experimental feature that runs registered code transformers against the generated Java source after emission. Transformers can remove, replace, or insert lines.

:::alert warning
This feature is disabled by default. You must enable it in the Lumen configuration before it will work.
:::

## Enabling Code Transform

Open your Lumen `config.yml` and set `code-transform` to `true`:

```yaml
language:
  experimental:
    code-transform: true
```

:::alert warning
This is an experimental feature. Transformers may modify lines that are actually needed at runtime, particularly in scripts that use raw Java blocks or rely on side effects.
:::

## How It Works

After code generation, Lumen runs all registered code transformers against the emitted Java code.

The builtin transformers are:

### Unused Variable Transformer

This transformer removes any local variable assignment where the variable is never referenced in the rest of the enclosing method. It handles dependency chains: if variable A is only used to initialize variable B, and B is itself unused, both A and B are removed.

## Example

Given this script:

```luma
command testing:
    message player "&6Hello!"
```

Without code transform, the generated Java code for this command would include:

```java
List<String> args = new ArrayList<>(Arrays.asList(__args));
Player player = __sender instanceof Player ? (Player) __sender : null;
CommandSender sender = __sender;
World world = player != null ? player.getWorld() : null;
```

With code transform enabled, the built in unused variable transformer detects `args`, `sender`, and `world` as unused and removes them. Only `player` remains because it is used by `message player`.

## Notes

Addons can register their own transformers through the API for any purpose, not just removing unused lines. See the API documentation for details.

If you encounter issues where a script breaks after enabling this feature, disable it and report the issue.
