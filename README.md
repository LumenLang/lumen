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
<p align="center">Lumen is a high-performance scripting platform for Minecraft servers that compiles your scripts directly into Java for native performance, with full hot reload and on-the-fly updates.</p>

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

Once processed, scripts are compiled server code, enabling native performance.

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
| Category                    | Skript                                                  | Lumen                                                                                                             |
|-----------------------------|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| **🔧 Developer Experience** |                                                         |                                                                                                                   |
| Hot Reload                  | 🟡 Requires manual command                              | 🟢 **Instant on save, no command needed**                                                                         |
| GUI Iteration               | ❌ Reopen or re-trigger required                         | 🟢 **Edit, save, live in-game with no closing, state can be preserved**                                           |
| IDE Support                 | 🟡 Basic, limited                                       | 🟢 **Advanced autocomplete, hover docs, real errors**                                                             |
| Error Feedback              | 🟡 Mostly runtime                                       | 🟢 **Same parse and compile errors as runtime, shown directly in the IDE**                                        |
| Migration Path              | ❌ Manual rewrite required                               | 🟢 **AI-assisted conversion, validation, runtime verification**                                                   |
| **🤖 AI Capabilities**      |                                                         |                                                                                                                   |
| Hallucinations              | ❌ Very common                                           | 🟢 **Low, validated using real docs, patterns, and identical errors to the plugin**                               |
| Validation                  | ❌ Run and test manually                                 | 🟢 **Automatic, real compiler validation via MCP tools**                                                          |
| Large Script Generation     | ❌ Breaks past ~50 to 100 lines                          | 🟢 **Handles 1000 to 5000 line scripts through the MCP, depending on the model and how the request is phrased**   |
| Iteration Loop              | ❌ Manual, copy errors, paste, retry, often breaks again | 🟢 **AI generates, validates, and fixes automatically via MCP**                                                   |
| Live Execution              | ❌ Not possible                                          | 🟢 **Run snippets dynamically via MCP and inspect real behavior in the debugger**                                 |
| Runtime Understanding       | ❌ Guesswork                                             | 🟢 **Debugger designed for AI, run snippets, see variable values, modify expressions and conditions dynamically** |
| Environment Interaction     | ❌ None                                                  | 🟢 **AI can interact with the Minecraft server, players, worlds, and more, via MCP and snippets**                 |
| **⚙️ Core Experience**      |                                                         |                                                                                                                   |
| Runtime Surprises           | 🟡 Common, mostly from silent coercion                  | 🟢 **Far less, since most mistakes are caught at compile time**                                                   |
| Syntax                      | 🟡 Familiar, can get messy on larger scripts            | 🟢 **Familiar feel, structured for larger scripts**                                                               |
| Performance                 | 🟡 Interpreted, many times slower than Java             | 🟢 **Compiled to Java, native speed**                                                                             |
| Beginner Friendliness       | 🟡 Forgiving up front, harder to debug later            | 🟡 Decisions made up front, easier to debug later                                                                 |
| Ecosystem                   | 🟢 **Large, with many addons**                          | 🟡 Much smaller, growing                                                                                          |
| Stability                   | 🟢 **Matured over a decade**                            | 🟡 Beta, but improving fast                                                                                       |
| Iteration Speed             | 🟡 Decent                                               | 🟢 **Instant feedback**                                                                                           |
All AI claims above are based on real testing and experience with the models, not theoretical.
Check out [Skript To Lumen Migration](https://docs.lumenlang.dev/01-user/01-migration/01-skript-to-lumen) for more information on why-so.
