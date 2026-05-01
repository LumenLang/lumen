<h1 align="center">Lumen</h1>
<p align="center">
  <a href="https://marketplace.visualstudio.com/items?itemName=lumenlang.lumenlang">
    <img src="https://img.shields.io/badge/VS%20Code-007ACC?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyByb2xlPSJpbWciIHZpZXdCb3g9IjAgMCAyNCAyNCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48dGl0bGU+VmlzdWFsIFN0dWRpbyBDb2RlPC90aXRsZT48cGF0aCBmaWxsPSJ3aGl0ZSIgZD0iTTIzLjE1IDIuNTg3TDE4LjIxLjIxYTEuNDk0IDEuNDk0IDAgMCAwLTEuNzA1LjI5bC05LjQ2IDguNjMtNC4xMi0zLjEyOGEuOTk5Ljk5OSAwIDAgMC0xLjI3Ni4wNTdMLjMyNyA3LjI2MUExIDEgMCAwIDAgLjMyNiA4Ljc0TDMuODk5IDEyIC4zMjYgMTUuMjZhMSAxIDAgMCAwIC4wMDEgMS40NzlMMS42NSAxNy45NGEuOTk5Ljk5OSAwIDAgMCAxLjI3Ni4wNTdsNC4xMi0zLjEyOCA5LjQ2IDguNjNhMS40OTIgMS40OTIgMCAwIDAgMS43MDQuMjlsNC45NDItMi4zNzdBMS41IDEuNSAwIDAgMCAyNCAyMC4wNlYzLjkzOWExLjUgMS41IDAgMCAwLS44NS0xLjM1MnptLTUuMTQ2IDE0Ljg2MUwxMC44MjYgMTJsNy4xNzgtNS40NDh2MTAuODk2eiIvPjwvc3ZnPg=="/>
  </a>
  <a href="https://lumenlang.dev">
    <img src="https://img.shields.io/badge/Website-000000?style=for-the-badge&logo=googlechrome&logoColor=white"/>
  </a>
  <a href="https://www.spigotmc.org/resources/133091/">
    <img src="https://img.shields.io/badge/SpigotMC-ED8106?style=for-the-badge&logo=spigotmc&logoColor=white"/>
  </a>
  <a href="https://builtbybit.com/resources/96917/">
    <img src="https://img.shields.io/badge/BuiltByBit-0066cc?style=for-the-badge&logo=builtbybit&logoColor=white"/>
  </a>
</p>
<p align="center">Lumen is a high-performance scripting platform for Minecraft servers that compiles your scripts directly into native Java code, with full hot reload and on-the-fly updates.</p>

> [!IMPORTANT]\
> Lumen is undergoing a phase with major breaking changes.
>
> The scripting side will change heavily. Patterns and syntax will be reworked, and many existing patterns may be
> modified or removed as part of this transition.
>
> A lot of internal systems will also be updated to improve consistency, diagnostics, and remove limitations.
>
> Because of this, things may break between versions for a while. This is expected during this phase.

---

## What Makes Lumen Different?

Lumen does not interpret scripts at runtime.

Instead, it:

1. Parses your `.luma` files on load
2. Generates native Java code
3. Makes that code execute like a normal plugin

There is no continuously running interpreter.
There are no runtime logic trees evaluated on every event.

Once processed, scripts are compiled server code, enabling near-native performance.

Which means you can write complex scripts without worrying about performance degradation as your server grows.

---

## Example Script

```luma
on join:
    message player "Welcome to the server!"

    if player is a op:
        message player "Hello, admin!"

    if a chance of 50%:
        give player diamond 1
```

- Save it as a `.luma` file in the `scripts` folder
- It will automatically detect a new file creation, and run it without any additional setup.

That is it.

It will also detect changes to the file and update the behavior on the fly, so you can iterate quickly.

---

## Independent Design

Lumen is built from the ground up and is **not a fork, wrapper, or extension of any existing scripting engine**.

The language, compiler pipeline, and runtime were designed specifically for Lumen. Scripts are not interpreted or translated directly into Java API calls. They go through a full compilation pipeline before they run.

---

## Current Status

Lumen is currently in beta.

If you encounter compilation failures, incorrect behavior, patterns not working as expected, or any unexpected issues, please report them through the issue tracker. Even if the script can be manually fixed, those cases are still valuable to report.

We also appreciate any feedback on the syntax, features, or anything else related to the project. The goal is to make Lumen as user-friendly and powerful as possible, and your input is crucial in achieving that.

---

## Real Comparison Vs Skript
| Category                    | Skript                                                  | Lumen                                                                                                         |
|-----------------------------|---------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| **🔧 Developer Experience** |                                                         |                                                                                                               |
| Hot Reload                  | 🟡 Requires manual command                              | 🟢 **Instant on save (no command needed)**                                                                    |
| GUI Iteration               | ❌ Reopen / re-trigger required                          | 🟢 **Edit → Save → Instantly visible in-game with no closing (state can be preserved)**                       |
| IDE Support                 | 🟡 Basic / limited                                      | 🟢 **Advanced autocomplete, hover docs, real errors**                                                         |
| Error Feedback              | 🟡 Mostly runtime                                       | 🟢 **Same parse/compile errors as runtime, shown directly in the IDE**                                        |
| Migration Path              | ❌ Manual rewrite required                               | 🟢 **AI-assisted conversion + validation + runtime verification**                                             |
| **🤖 AI Capabilities**      |                                                         |                                                                                                               |
| Hallucinations              | ❌ Very common                                           | 🟢 **Low (validated using real docs, patterns, and identical errors to the plugin)**                          |
| Validation                  | ❌ You must run and test manually                        | 🟢 **Automatic, real compiler validation (via MCP tools)**                                                    |
| Large Script Generation     | ❌ Breaks past ~50–100 lines                             | 🟢 **Can make or handle large systems using the MCP, including once with over 1-5K+ lines (model-dependent)** |
| Iteration Loop              | ❌ Manual: copy errors, paste, retry, often breaks again | 🟢 **AI generates → validates → fixes automatically using MCP**                                               |
| Live Execution              | ❌ Not possible                                          | 🟢 **Run snippets dynamically through the MCP and inspect real behavior via the Debugger**                    |
| Runtime Understanding       | ❌ Guesswork                                             | 🟢 **Debugger designed for AI, execute snippets dynamically, see value of variables, and modify logic live**  |
| Environment Interaction     | ❌ None                                                  | 🟢 **AI can interact with the minecraft server (players, worlds, etc.) through the MCP and snippets**         |
| **⚙️ Core Experience**      |                                                         |                                                                                                               |
| Runtime Surprises           | ❌ Common                                                | 🟢 **Far less unexpected runtime behavior (mainly caused by external plugins)**                               |
| Syntax                      | ❌ Can get messy                                         | 🟢 **Readable and structured**                                                                                |
| Performance                 | ❌ Interpreted and dynamic, many times slower than Java  | 🟢 **Compiled to Java, meaning near native speed**                                                            |
| Ecosystem                   | 🟢 **Large ecosystem**                                  | ❌ Much more limited ecosystem                                                                                 |
| Stability                   | 🟢 **Matured over a decade**                            | ❌ Relatively new, but next-gen                                                                                |
| Iteration Speed             | 🟡 Decent                                               | 🟢 **Instant feedback**                                                                                       |
All of the following AI claims above are based on real testing and experience with the models, and are not just theoretical.
Check out https://docs.lumenlang.dev/01-user/01-migration/01-skript-to-lumen for more information on why-so.

---

## Tradeoffs

Tradeoffs are inevitable with Lumen, as no scripting system is perfect.

### **Smaller Ecosystem**
Lumen is still a relatively new project. Compared to more established scripting systems, the ecosystem is still growing.

### **Strictly Typed Language Model**

Lumen is a fully statically typed scripting language. Every value has a known type at compile time, and type mismatches are reported immediately in the editor with precise diagnostics. There is no implicit coercion or “guessing” at runtime, what you write is exactly what gets executed.
Skript is a dynamically typed language. Types are determined at runtime, and there is a lot of implicit coercion and guessing involved. This can lead to unexpected behavior, terribly unclear error messages, things just "not working" without any indication of why, and a generally frustrating experience.

While Lumen may feel somewhat familiar to users coming from systems like Skript, its design is fundamentally different. Scripts are compiled with strict type checking, explicit nullability, and well-defined patterns. This results in more predictable behavior, clearer errors, and significantly fewer runtime issues.

### **Documentation Is Still Expanding**
Documentation is continuously improving. If you encounter missing information, unclear behavior, or patterns that are not properly documented, please report it through the project's issue tracker so it can be addressed.
