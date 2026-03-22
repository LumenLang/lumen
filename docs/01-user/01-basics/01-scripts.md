---
description: "How Lumen scripts are structured, including blocks, indentation, comments, and file layout."
---

# Scripts and Blocks

:::alert note
For the best experience writing Lumen scripts, install the [Lumen VSCode extension](https://marketplace.visualstudio.com/items?itemName=lumenlang.lumenlang). It provides completions, hovers, syntax highlighting, error checking, and other tools that make development significantly better. Writing scripts without it is possible but noticeably harder.
:::

Lumen scripts are plain text files with the `.luma` extension. You place them in the `scripts` folder inside your Lumen plugin directory, and they are automatically loaded when the server starts. If hot-reloading is enabled (the default), saving a script file will instantly reload it without restarting the server.

## Indentation and Blocks

Lumen uses indentation to define structure. A line that ends with `:` starts a new block, and all the lines with more indentation beneath it belong to that block.

```luma
command hello:
    message player "&aHello!"
    message player "&7Welcome to the server."
```

The two `message` lines are inside the `command hello:` block because they have more spaces than the `command` line.

Blocks can be nested. For example, an `if` inside a command:

```luma
command greet:
    if args size < 1:
        message player "&cPlease provide a name!"
    else:
        var name = get args at index 0
        message player "&aHello, {name}!"
```

The `message player "&cPlease provide a name!"` line belongs to the `if` block, which itself is inside the `command` block. The `else:` starts a sibling block at the same depth as the `if`.

### How Indentation Works

Lumen does not enforce a specific number of spaces per indent level. What matters is the relative amount of leading spaces between lines. A line with more spaces than the block header above it is considered part of that block. A line with fewer spaces exits back to a parent block. A line with no spaces at all is at the root level.

You can mix 1, 2, 3, 4, or any number of spaces freely within the same file. Lumen will warn you about inconsistent indentation, but it still works.

:::alert info
If you want to clean up inconsistent indentation, you can use the formatting tools available at https://lumenlang.dev.
:::

## Comments

Lines starting with `#` and `//` are comments. The compiler ignores them entirely.

```luma
# This is a comment
command test:
    # This is also a comment
    // Another comment style
    message player "&aHello!" // This is a comment too
```

## Script Lifecycle

When a script file is loaded, all top-level structures (commands, events, schedules, global variables, config blocks) are registered. The optional `load` block runs once immediately after loading:

```luma
load:
    broadcast "&aScript loaded!"
```

There is also a `preload` block that runs even earlier in the loading process, before events and commands are fully registered. Most scripts will not need `preload`.
