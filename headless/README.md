# headless

Headless validation and tooling for Lumen scripts, without a running Minecraft server.

---

## What it is

[Lumen](https://lumenlang.dev) is a high-performance scripting platform for Minecraft servers that compiles scripts directly into native Java code. Normally, validating a script requires a live Bukkit/Paper server to load the plugin.

This module removes that requirement. It bootstraps the full Lumen registration system, including all registered patterns, type bindings, and events from the plugin module, and exposes them as a lightweight local process without a Minecraft server running.

Ideal for:

- Editor integrations that show diagnostics as you type
- AI tooling that needs to reason about Lumen and validate code
- The Lumen test harness for catching regressions in patterns, diagnostics, and codegen

The VS Code extension for Lumen uses this module to fully validate scripts.

---

## What it provides

**Script validation and compilation.** Send a `.luma` script source string, get back the generated Java and any errors. The same pipeline that runs on a live server runs here.

**Pattern search.** Query the full set of registered patterns with real pattern matching or fuzzy scoring.

**Type binding checks.** Test whether a given input token matches a specific Lumen type binding, with support for simulating a variable environment.

Everything runs in a single long-lived process accessed via a JSON line protocol over stdin/stdout.

## How does it do it?

It embeds the Spigot API and the full Lumen plugin into a shaded jar, then runs an extracted version of the JavaPlugin in-process.

---

## Build

From the repo root:

```
./gradlew :headless:shadowJar
```

The shaded jar is written to `headless/build/libs/LumenHeadless.jar`.

---

## Requirements

- Java 17 or newer (JRE is sufficient)
