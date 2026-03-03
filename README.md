# Lumen

### Simple scripting for Minecraft servers, built for modern performance

Lumen is a high-performance scripting engine for Minecraft servers that compiles your scripts directly into native Java code, with full hot reload and on-the-fly updates.

Visit the [documentation](https://docs.lumenlang.dev/) for guides, examples, and how-to's.

Visit the [reference documentation](https://lumenlang.dev) for a full list of features, syntax, and examples.

---

## What Makes Lumen Different?

Lumen does not interpret scripts at runtime.

Instead, it:

1. Parses your `.luma` files on load
2. Generates native Java code
3. Executes that code like a normal plugin

There is no continuously running interpreter.
There are no runtime logic trees evaluated on every event.

Once processed, scripts behave like compiled server code, enabling near-native performance.

### What That Means

- No script engine running every tick or event
- No persistent execution graphs in memory
- Significantly lower memory usage
- JVM optimizations apply naturally

Which means you can write complex scripts without worrying about performance degradation as your server grows.

---

## Example Script

```luma
on join:
    message player "Welcome to the server!"

    if player is op:
        message player "Hello, admin!"

    if chance 50%:
        give player diamond 1
```

- Save it as a `.luma` file in the `scripts` folder
- It will automatically detect a new file creation, and run it without any additional setup.

That is it.

The syntax is designed to be simple and readable, and the behavior does not require you to run commands like `/reload`, restart, or script reloads.
It will also detect changes to the file and update the behavior on the fly, so you can iterate quickly.

---

## Current Status

Lumen is currently in beta.

The core system is stable, but there are still edge cases where a script may fail to compile unexpectedly. In many situations, these should instead be reported as parse errors. Receiving a raw compiler error is considered a bug at this stage.

If you encounter compilation failures, incorrect behavior, patterns not working as expected, or any unexpected issues, please report them through the issue tracker. Even if the script can be manually fixed, those cases are still valuable to report.

We also appreciate any feedback on the syntax, features, or anything else related to the project. The goal is to make Lumen as user-friendly and powerful as possible, and your input is crucial in achieving that.

---

## Tradeoffs...
Tradeoffs are inevitable with Lumen, as no scripting engine is perfect.

- **Less IDE Support:** Traditional scripting engines often have plugins for syntax highlighting and autocompletion. Lumen currently lacks this support, making it more challenging to write, though we plan to address this in the future as soon as possible.