---
description: "A practical guide for Skript users moving to Lumen, covering the architectural and syntactic differences, AI story, the migration mindset, and why Lumen is built the way it is."
---

# The Skript to Lumen Migration Guide

If you're coming from Skript, the surface of Lumen will look familiar. `on join:`, `command heal:`, `set x to 5`,
`broadcast "hi"`. The shapes are similar. But underneath, Lumen is a very different language, and pretending otherwise
will only frustrate you when things don't translate one-to-one.

Skript is an interpreter. Every time your main block gets called, it walks your tree of effects and conditions,
evaluating values on the fly. Effects, conditions, and expressions are looked up at runtime against tables registered by
Skript and its addons. That's why it's flexible, and also why it's slow, and why its tooling is fundamentally limited.

Lumen takes the opposite approach. Your `.luma` file is parsed once and compiled directly into Java bytecode. The
running server just executes plain JVM code. By the time the server is running, your script *is* the code. There is no
Skript-style runtime evaluating your script behind the scenes, and there's no per-line handler lookup on every event.

That changes a lot, and most of it is for the better.

## Real Comparison Vs Skript

Before you dive into the syntax-by-syntax differences, it's worth seeing what you actually gain by migrating. The rest of this guide explains the *changes*, this table explains the *payoff*.

:::compare left=Skript right=Lumen
:::group Developer Experience
:::row Hot Reload
left warn: Requires manual command
right win: Instant on save, no command needed
:::
:::row GUI Iteration
left bad: Reopen or re-trigger required
right win: Edit, save, live in-game with no closing, state can be preserved
:::
:::row IDE Support
left warn: Basic, limited
right win: Advanced autocomplete, hover docs, real errors
:::
:::row Error Feedback
left warn: Mostly runtime
right win: Same parse and compile errors as runtime, shown directly in the IDE
:::
:::row Migration Path
left bad: Manual rewrite required
right win: AI-assisted conversion, validation, runtime verification
:::
:::

:::group AI Capabilities
:::row Hallucinations
left bad: Very common
right win: Low, validated using real docs, patterns, and identical errors to the plugin.
:::
:::row Validation
left bad: Run and test manually
right win: Automatic, real compiler validation via MCP tools
:::
:::row Large Script Generation
left bad: Breaks past ~50 to 100 lines
right win: Handles large systems via the MCP, 1k to 5k+ lines proven, model-dependent
:::
:::row Iteration Loop
left bad: Manual, copy errors, paste, retry, often breaks again
right win: AI generates, validates, and fixes automatically via MCP
:::
:::row Live Execution
left bad: Not possible
right win: Run snippets dynamically via MCP and inspect real behavior in the debugger
:::
:::row Runtime Understanding
left bad: Guesswork
right win: Debugger designed for AI, run snippets, see variable values, modify expressions and conditions dynamically
:::
:::row Environment Interaction
left bad: None
right win: AI can interact with the Minecraft server, players, worlds, and more, via MCP and snippets
:::
:::

:::group Core Experience
:::row Runtime Surprises
left bad: Common
right win: Far less unexpected runtime behavior, mostly only from external plugins
:::
:::row Syntax
left bad: Can get messy
right win: Readable and structured
:::
:::row Performance
left bad: Interpreted, many times slower than Java
right win: Compiled to Java, near-native speed
:::
:::row Ecosystem
left win: Large ecosystem
right bad: Much more limited
:::
:::row Stability
left win: Matured over a decade
right bad: Relatively new, but next-gen
:::
:::row Iteration Speed
left warn: Decent
right win: Instant feedback
:::
:::
:::

All AI claims above are based on real testing and experience with the models, not theoretical.

## Before You Read Any Further: The AI Story

:::alert info
**TL;DR.** Lumen ships with a first-class MCP server. AI assistants don't guess at syntax, they run the real compiler.
They don't ask you what happened, they drive your live server as you. Hand it your old `.sk` file and let it migrate,
compile, and test for you. **You don't have to do the migration yourself.**
:::

The rest of this guide explains what changes between Skript and Lumen, syntax-by-syntax. Before you start reading that
and feeling like the migration is a mountain, read this first. The mountain is mostly already done for you.

### The AI doesn't guess. It runs the compiler.

When you ask an AI to convert a Skript snippet, write a new feature, or fix a broken script, it isn't scraping the web
or hallucinating syntax. It calls the real Lumen compiler through MCP tools and iterates until the script actually
compiles.

What the AI has direct access to:

- **All 500+ patterns**, the full reference, type bindings, and example scripts, as callable tools.
- **Pattern search ranked by the real compiler**, the same ranker lumen uses. The AI finds the pattern you actually
  meant, not one that shares a keyword.
- **Live diagnostics from LumenHeadless**, so no running server is required to know if the script compiles.

This is why Lumen's AI tooling produces scripts at scales that aren't realistic on Skript.

> A 2000-line, 9-page shop GUI generated by Sonnet on the first try in about twelve minutes.
>
> The MCP has shipped 5000+ line scripts without a single human correction. On cheap models.

### It debugs *as you*. Not around you.

Once a script is running, the same MCP gives the AI direct access to your live server. **You don't paste logs. You don't
copy variables. You don't explain the bug.**

The AI can:

- Trigger events on your server as if they happened to you
- Capture full variable snapshots on any line without pausing the tick
- Override expressions or branches in memory, revertible in one call
- Force a branch to `true`, `false`, `skip`, or a replacement, to walk any code path
- Inherit your exact player, item, and event context for follow-up snippets
- Execute arbitrary Lumen statements, expressions, or `java:` blocks on the running server, as you, while you stand on
  it

No restart. No redeploy. No manual copy and pasting. When debugging is enabled, Lumen instruments the parsed script (
every expression and condition gets an ID), and the AI uses that instrumentation directly.

### What this means for your migration

You don't grind through translation tables line by line.

1. Hand the AI your old `.sk` file.
2. Let it produce a Lumen script.
3. Let it run that script through the compiler until it cleanly compiles.
4. Let it test on your live server through the same connection.
5. If something behaves wrong, let it debug and simulate it for you

Everything below is still worth reading so you understand what your scripts will look like and why. But the actual
line-by-line porting is not the work you're signing up for.

To get started, install LumenMCP for your AI client. Instructions are in
the [LumenMCP repository](https://github.com/LumenLang/lumenmcp).

## What's Actually Different

### Static typing is the foundation

Every variable, parameter, and field has a known type at compile time. `int`, `string`, `player`, `list of string`,
`nullable item`. The compiler checks every assignment, comparison, and pattern match against those types before the
script ever runs. This is why Lumen can't be "Skript but faster": the entire architecture is built around knowing the
type of every value at compile time.

### Null is part of the type system

In Skript, when something is unset (an offline player, a missing variable, an empty slot), the line involving it tends
to silently no-op or produce odd results, often without a warning. In Lumen, a value is either non-null and guaranteed
present, or it's marked `nullable` and the compiler refuses to let you use it without checking first. `if x is set:`
narrows the type inside the branch so you can use the value safely. There is no silent failure path.

### Tooling is genuine, because the language was built for it

Skript's editor tooling (LSP servers, completions, hover docs) generally requires either a running Minecraft server, a
special server fork, or an additional plugin to feed structural information out to the editor. Even then it's
approximate, because the runtime is the source of truth and the editor is guessing.

Lumen flips this. The compiler itself is the source of truth, and it runs the same way in your editor as it does on the
server. Every diagnostic, every completion, every hover, every "go to definition" is the real compiler reporting real
results. If the editor says your script will compile, it will compile. If it flags an error, that error is real. There
is no approximation layer, because the language was architecturally designed around making this possible.

### Almost everything is a pattern

Skript hardcodes effects, conditions, and expressions inside the runtime. In Lumen, the syntax you write is matched
against patterns that resolve at compile time to a specific Java handler. Addons add new patterns. The compiler has full
visibility into all of them, which is why it can suggest fixes when you typo something, validate argument types, and
generate accurate completions. If no pattern matches your line, you get a real error with suggestions, not a silent
skip.

### No implicit arguments or conversions

Skript will happily turn a player into a string, a string into a number, a name into a player, and so on, behind your
back. Lumen doesn't. Every argument has the type the pattern declares, and there are no surprise coercions sliding into
your script. Conversions between types still exist, but you write them explicitly, so the conversion is part of your
code instead of guesswork. This is what makes the rest of the language possible: tooling, diagnostics, and AI assistants
can only be precise if the meaning of every token is unambiguous.

---

So while the syntax often looks similar, treat Lumen as its own language with its own rules. The translations below will
get you started, but expect to think differently once you're past the basics.

## 1. File Structure and Indentation

Lumen uses indentation to define structure, the way Python does, but it doesn't care about a fixed number of spaces.
What matters is **relative** indentation: a line indented more than the block header above it belongs to that block. You
can use 2 spaces, 4 spaces, a tab, whatever, as long as parents and children can be told apart.

A line ending in `:` opens a block. Indented lines below it are the body. Skript's `trigger:` block under a command is
unnecessary in Lumen; the body sits directly under the `command` line:

**Skript:**
```skript
command /heal:
    trigger:
        heal the player
```

**Lumen:**
```luma
command heal:
    heal the player
```

## 2. Variables and Scope

This is the area that changes the most. In Skript, `{var}` is global and persistent by default, `{_var}` is local, and
you don't think about types. Lumen requires explicit type, scope, and persistence for any global, declared inside a
`globals:` block.

**Local variables** live inside the block they're set in. Their type is inferred:

```luma
set name to "Steve"
set count to 5
```

**Global variables** are declared inside a `globals:` block. There is one such block per script, and every global lives
in it:

```luma
globals:
    multiplier: double with default 2.0
    prefix: string with default "&8[&bServer&8]"
```

**Persistent variables** add `stored`, so the value survives restarts:

```luma
globals:
    stored total_joins: int with default 0
```

**Per-player variables** add `scoped to player`. Each player gets their own copy:

```luma
globals:
    stored scoped to player coins: int with default 0

command earn:
    add 10 to coins for player
```

Reads and writes for scoped variables need a `for <entity>` clause:

```luma
set coins to 100 for player
set c to get coins for player
```

## 3. Lists and Maps (Dictionaries)

Skript's `{items::*}` lists are gone. Lumen has typed `list` and `map` collections, and the type of the elements has to
be declared.

**Lists:**
```luma
set fruits to new list of string
add "apple" to fruits
remove "apple" from fruits
```

In a `globals:` block, lists default to empty so `with default` is optional:

```luma
globals:
    fruits: list of string
```

**Maps:**
```luma
set stats to new map of string to int
set stats at key "kills" to 10
set k to get stats at key "kills"
```

## 4. Custom Data Types (Data Classes)

If you've ever stored complex objects in Skript with something like `{data::%uuid%::name}`, Lumen's `data` classes are a
relief. A `data` declaration creates a real typed structure with named, typed fields.

```luma
data warp:
    name: string
    x: double
    y: double
    z: double

globals:
    stored warps: list of warp

command setwarp:
    set w to new warp with name "Spawn" x 0 y 64 z 0
    add w to warps
```

## 5. GUIs / Inventories

Lumen has a built-in inventory system, and its hot-reload behaviour is one of the most distinctive features of the
platform. You don't need TuSKe, Skript-GUI, or anything similar.

```luma
register inventory "menu":
    set gui to new inventory "menu" with rows 3 titled "Menu"

    set icon to new item diamond
    set icon's name to "Click me"
    set slot 13 of gui to icon

    open gui for player

slot 13 click in "menu":
    message player "You clicked the diamond!"
    close player's inventory

command menu:
    open inventory named "menu" for player
```

The interesting part: when you save the script while a player has the inventory open, the inventory updates in place. Items get replaced, the title refreshes, click handlers are updated, and the player sees the changes live without
the inventory closing or flickering. This is genuinely rare in the Minecraft scripting world and is worth trying once on
a test server just to feel the difference in iteration speed. Here's a preview you can see:

:::video https://s3api.vansencool.net/vansen/inventory_reload.mp4
:::

## Why Lumen Is Like This

If you're reading this and thinking "this is a lot harder than Skript", you're right, and it's worth understanding
*why*. Lumen is harder on purpose, and the reasons compound on each other.

### Static typing comes first

In Skript, the language has no idea whether a variable is a player, a number, or a string until the script actually
runs. That's dynamic typing, and it's why Skript is forgiving for beginners but unpredictable in practice. Lumen knows
the type of every value before execution. That's stricter for beginners, but once you're actually building real things,
you get far fewer runtime surprises, and the bugs that do appear are much easier to debug.

### No implicit coercion

Skript silently changes one type into another to make a line "work": a player turns into the player's name when
something expects a string, a string turns into a number when something expects a number, a world becomes its name, and
so on. It feels helpful, but it's also one of the biggest sources of bugs in Skript scripts, because the script never
errors, it just behaves wrong, and the failure can be wildly far from the line that caused it.

Lumen has no implicit coercion. If a pattern wants a string, you give it a string. If it wants a player, you give it a
player. The compiler refuses anything else, loudly, at compile time. Conversions between types still exist, but you
write them out yourself, so the conversion is part of the script, visible to you and to the compiler, instead of
happening behind your back.

### Variables don't change type

In Skript you can assign a variable to a string, then later to a player, then to a number, and the language plays along.
In Lumen, once a variable has a type, that's its type. You can't reassign it to something incompatible. This eliminates
an entire class of bugs that you don't even know you have until you've spent a week debugging one.

### Diagnostics are a first-class goal

The other big reason for static typing isn't the typing itself, it's the *errors*. Lumen's diagnostics are heavily inspired by Rust's: precise underlines on the exact tokens that are wrong, labels explaining what the compiler expected, sub-highlights for related parts of the line, fuzzy "did you mean" suggestions, and notes that point at *why* something is wrong rather than just *that* it is wrong.

That kind of diagnostic is only possible when the compiler actually knows what every token means. With dynamic typing and silent coercions, an error message can't say much more than "this didn't work", because the compiler doesn't have the information to be specific. With Lumen's stricter rules, the compiler can point at the exact field, the exact mismatched type, the exact pattern that almost matched.

Here's what that looks like in practice:

```
error: Unknown statement
  -> line 95: broadcast "Hi" right now
   |
95 | broadcast "Hi" right now
   |                ~~~~~~~~~ remove 2 extra tokens
   |
  = note: confidence: 84% (high)
  = help: closest pattern: (broadcast|announce) %text:STRING%
```

Typo in the verb? It tells you what to replace it with:

```
error: Unknown statement
  -> line 99: brooadcast "Hi" now
   |
99 | brooadcast "Hi" now
   | ~~~~~~~~~~ replace with 'broadcast'
   |                 ~~~ extra token
   |
  = note: confidence: 87% (high)
```

Multiple problems on one line? It points at all of them in a single error, with the right label on each:

```
error: Unknown statement
  -> line 103: send title "<gold>Hi" with subtitl "<gray>Sub" to plays extra
    |
103 | send title "<gold>Hi" with subtitl "<gray>Sub" to plays extra
    |                            ~~~~~~~ did you mean 'subtitle'?
    |                                                   ~~~~~~ 'plays' does not exist in this scope
    |                                                          ~~~~~ extra token
    |
  = note: confidence: 73% (high)
  = help: closest pattern: send title %title:STRING% with subtitle %sub:STRING% to %who:PLAYER%
```

Rust's reputation for great error messages is a big part of why people enjoy writing it. Lumen aims for the same feeling on Minecraft scripts.

---

Lumen is harder upfront because the things that make Skript easy upfront are also what make Skript scripts break in
mysterious ways later. The tradeoff is more discipline now, far fewer "why did this just stop working" moments later,
and tooling that actually helps you while you write.
