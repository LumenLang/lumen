---
description: "How to create custom commands with arguments, permissions, aliases, and subcommands."
---

# Commands

Commands let you define custom slash commands that players can use in game.

## Defining a Command

Use the `command` keyword followed by the command name and a colon. Everything indented beneath it becomes the command's body.

```luma
command hello:
    message player "&aHello, world!"
```

When a player types `/hello`, they see a green "Hello, world!" message.

## Description

Give your command a description that shows up in the help menu:

```luma
command greet:
    description "Sends a friendly greeting"
    message player "&eHey there, {player_name}!"
```

## Aliases

Let players use shorter or alternative names:

```luma
command serverinfo:
    description "Show server info"
    aliases si, info

    message player "&6Welcome to the server!"
```

Players can use `/serverinfo`, `/si`, or `/info`.

## Permissions

Restrict a command so only permitted players can run it:

```luma
command wand:
    permission "lumen.wand"
    description "Gives you a selection wand"

    message player "&aHere is your wand!"
```

## Arguments

The `args` variable holds everything the player typed after the command name.

```luma
command greet:
    description "Greet someone"

    if args size < 1:
        message player "&cUsage: /greet <name>"
    else:
        var name = get args at index 0
        message player "&aHello, {name}!"
```

Useful operations on `args`:

- `args size` returns the argument count
- `get args at index 0` gets the first argument (0-based)
- `args joined with " "` joins all arguments into a single string

## Subcommands

Check the first argument to branch into different actions:

```luma
command todo:
    description "Manage your todo list"

    if args size < 1:
        message player "&7Usage: /todo <add|clear>"
    else:
        var action = get args at index 0

        if action is "add":
            if args size < 2:
                message player "&cUsage: /todo add <task>"
            else:
                var task = get args at index 1
                message player "&aAdded task: {task}"

        else if action is "clear":
            message player "&eTodo list cleared!"
```
