# Lumen

### Simple scripting for Minecraft servers, built for modern performance

Lumen is a high-performance scripting language for Minecraft servers that compiles your scripts directly into native Java code, with full hot reload and on-the-fly updates.

Visit the [Documentation](https://docs.lumenlang.dev/) for guides, examples, and how-to's.

Visit the [Reference Documentation](https://lumenlang.dev) for a full list of features, syntax, and examples.

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

## Independent Design

Lumen is built from the ground up and is **not a fork, wrapper, or extension of any existing scripting engine**.

The language, compiler pipeline, and runtime were designed specifically for Lumen. Scripts are not interpreted or translated directly into Java API calls. They go through a full compilation pipeline before they run.

At a high level, this is how a script is processed:

- **Tokenization:** Raw script text is split into tokens while indentation and comments are processed.
- **Parsing:** Tokens are assembled into an abstract syntax tree that represents the structure of the script.
- **Pattern matching:** Statements are matched against registered patterns using typed placeholders and recursive matching.
- **Statement classification:** Matched statements are categorized so the compiler knows how code should be generated.
- **Expression parsing and resolution:** Expressions are analyzed and resolved with compile time type information.
- **Type binding execution:** Placeholder tokens are validated and converted into Java source fragments.
- **Symbol tracking:** A compile time environment tracks local, global, and stored variables across scopes.
- **Code emission:** The AST is walked and Java source code is generated.

Following this, a new **Pipeline Documentation** will be released with detailed explanations of each stage for those interested in contributing to Lumen or understanding how it works internally.

---

## Tradeoffs

Tradeoffs are inevitable with Lumen, as no scripting system is perfect.

- **Smaller Ecosystem:**
Lumen is still a relatively new project. Compared to more established scripting systems, the ecosystem is still growing.

- **Different Scripting Model:**
While Lumen may feel somewhat familiar to users coming from systems like Skript, its design and usage are fundamentally different. Scripts follow Lumen's own pattern system and compilation model, which may require adjusting to a new workflow.

- **Documentation Is Still Expanding:**
Documentation is continuously improving. If you encounter missing information, unclear behavior, or patterns that are not properly documented, please report it through the project's issue tracker so it can be addressed.