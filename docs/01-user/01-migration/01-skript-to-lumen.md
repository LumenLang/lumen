---
description: "A practical guide for Skript users moving to Lumen, covering the architectural and syntactic differences, the migration mindset, and why Lumen is built the way it is."
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

That changes a lot, and most of it is for the better. Here are the parts that matter most when you're migrating.

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
back. Lumen doesn't. Every conversion is explicit, every argument has the type the pattern declares, and there are no
surprise coercions sliding into your script. This is what makes the rest of the language possible: tooling, diagnostics,
and AI assistants can only be precise if the meaning of every token is unambiguous.

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
```applescript
command /heal:
    trigger:
        heal the player
```

**Lumen:**
```luma
command heal:
    heal the player
```

For a deeper walk through script structure, blocks, and indentation rules,
see [Scripts and Blocks](../02-basics/01-scripts.md). For the full set of statement, expression, and command syntax, see
the rest of the [Basics](../02-basics/) section.

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

For the full picture,
see [Local Variables](../03-variables/01-local-variables.md), [Global Variables](../03-variables/02-global-variables.md),
and [Stored Variables](../03-variables/03-stored-variables.md). Null handling is covered in detail
in [Nullables](../05-nullables/01-nullables.md).

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

For all the operations available on lists and maps (iteration, sorting, filtering, contains checks, etc.),
see [Lists](../07-collections/01-lists.md) and [Maps](../07-collections/02-maps.md).

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

You can store data classes inside lists, maps, or scoped globals like any other type.
See [Data Classes](../10-data-classes/01-data-classes.md) for field syntax, construction, and access patterns.

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

The interesting part: when you save the script while a player has the inventory open, the inventory **updates in place
**. Items get replaced, the title refreshes, click handlers stay wired up, and the player sees the changes live without
the inventory closing or flickering. This is genuinely rare in the Minecraft scripting world and is worth trying once on
a test server just to feel the difference in iteration speed.

:::video https://s3api.vansencool.net/vansen/inventory_reload.mp4
:::

For inventory and item syntax in detail, see [Items](../11-inventories/01-items.md)
and [Inventories](../11-inventories/02-inventories.md).

## Why Lumen Is Like This

If you're reading this and thinking "this is a lot harder than Skript", you're right, and it's worth understanding
*why*. Lumen is harder on purpose, and the reasons compound on each other.

### Static typing comes first

In Skript, the language has no idea whether a variable is a player, a number, or a string until the script actually
runs. That's dynamic typing, and it's why Skript is forgiving for beginners but unpredictable in practice. Lumen knows
the type of every value before execution. That's stricter for beginners, but once you're actually building real things,
you get far fewer runtime surprises, and the bugs that do appear are much easier to debug.

### No coercion

Skript silently changes one type into another to make a line "work": a player turns into the player's name when
something expects a string, a string turns into a number when something expects a number, a world becomes its name, and
so on. It feels helpful, but it's also one of the biggest sources of bugs in Skript scripts, because the script never
errors, it just behaves wrong, and the failure can be wildly far from the line that caused it.

Lumen has no coercion at all. If a pattern wants a string, you give it a string. If it wants a player, you give it a
player. The compiler refuses anything else, loudly, at compile time.

Lumen does have **widening** (e.g. an `int` is accepted where a `double` is expected, since it fits), but that's a
different concept: widening is safe and lossless, coercion is a guess.

### Variables don't change type

In Skript you can assign a variable to a string, then later to a player, then to a number, and the language plays along.
In Lumen, once a variable has a type, that's its type. You can't reassign it to something incompatible. This eliminates
an entire class of bugs that you don't even know you have until you've spent a week debugging one.

---

Lumen is harder upfront because the things that make Skript easy upfront are also what make Skript scripts break in
mysterious ways later. The tradeoff is more discipline now, far fewer "why did this just stop working" moments later,
and tooling that actually helps you while you write.
