---
description: "Write raw Java code directly inside Lumen scripts for full JVM access."
tags: "experimental"
---

# Raw Java

Raw Java is an experimental feature that lets you write actual Java code directly inside your Lumen scripts. This gives you full access to the JVM, the Bukkit/Paper API, and any library on the server's classpath.

:::alert warning
This feature is disabled by default. You must enable it in the Lumen configuration before it will work.
:::

## Enabling Raw Java

Open your Lumen `config.yml` and set `raw-java` to `true`:

```yaml
language:
  experimental:
    raw-java: true
```

:::alert warning
Only enable this if you fully trust the scripts running on your server. Raw Java can do anything that normal Java code can do.

Always review scripts containing raw Java before running them. A `java:` block has the same power as a compiled plugin.
:::

## Writing a Raw Java Block

Use the `java:` keyword followed by an indented block. Every line inside is passed directly to the Java compiler without any processing by Lumen.

```luma
command test:
    java:
        System.out.println("Hello from raw Java!");
```

This prints to the server console (not to the player's chat, since `System.out.println` writes to console output).

## Available Variables

The variables available inside a `java:` block depend on which block it is nested in. Lumen generates real Java methods, and these variables are local parameters or fields on the generated class.

### Inside Commands

- `Player player` (the player who ran the command, null if run from console)
- `CommandSender sender` (the command sender)
- `World world` (the player's world, null if console)
- `List<String> args` (the command arguments)

```luma
command whoami:
    java:
        player.sendMessage("You are: " + player.getName());
        player.sendMessage("Health: " + player.getHealth());
        player.sendMessage("World: " + world.getName());
```

### Inside Events

- `event` (the Bukkit event object, typed to the specific event class)
- `Player player` (for player events)
- Other event-specific variables depending on the event type

```luma
on join:
    java:
        player.sendMessage("Welcome, " + player.getName() + "!");
        event.setJoinMessage(player.getName() + " has arrived!");
```

### Class-Level Fields

Config values and global variables are class fields, so they are accessible from any `java:` block:

```luma
global var counter default 0

command count:
    java:
        counter++;
        player.sendMessage("Counter: " + counter);
```

## Import and Use Statements

When writing raw Java, you often need classes that are not imported by default. Lumen provides two interchangeable statements for this: `import` and `use`.

```luma
import org.bukkit.Material
use org.bukkit.inventory.ItemStack
```

Both add the specified class to the generated Java file's imports. Place them anywhere in your script, typically near the top.

### Default Imports

Lumen automatically imports several common classes, so you do not need to import them yourself:

- `org.bukkit.entity.Player`
- `org.bukkit.command.CommandSender`
- `org.bukkit.plugin.Plugin`
- `org.bukkit.Bukkit`
- `org.bukkit.event.Listener`

### Example with Imports

```luma
import org.bukkit.Bukkit
import org.bukkit.entity.Player

command players:
    description "List online players using raw Java"

    java:
        for (Player p : Bukkit.getOnlinePlayers()) {
            player.sendMessage("Online: " + p.getName());
        }
```

## Mixing Lumen and Raw Java

You can mix Lumen code and `java:` blocks in the same command or event. Lumen code compiles to Java behind the scenes, so they work together:

```luma
import org.bukkit.Bukkit

command serverinfo:
    description "Show server info"

    message player "&6Server Info:"
    message player "&7Online players listed below:"

    java:
        int count = Bukkit.getOnlinePlayers().size();
        player.sendMessage("Total online: " + count);
```

## Important Notes

- Syntax errors in your Java code will cause the script to fail to compile, and Lumen will report the error.
- You have access to the full server classpath, including all loaded plugins and libraries.
- Performance is identical to native Java since it compiles down to the same bytecode.
- Changes to `java:` blocks are picked up on script reload.
