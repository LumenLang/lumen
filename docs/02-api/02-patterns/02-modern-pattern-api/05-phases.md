---
description: "Split a handler body into compile-time and runtime sections with // lumen:compile and // lumen:runtime markers."
---

# Compile and runtime phases

A handler body has two phases:

- **Runtime** code is emitted into the script class and runs every time the script reaches the call site.
- **Compile** code runs at script codegen, inside the addon JVM, with access to `HandlerContext`. Use it to inspect the matched pattern (`ctx.choice(0)`, `ctx.value("name")`, ...) or emit extra Java alongside the runtime body.

A handler with no markers is entirely runtime. To opt into a compile section, add `HandlerContext` as the first parameter and split the body with `// lumen:compile` and `// lumen:runtime` line comments:

```java
@Statement
@Pattern("greet (formal|casual) %who:PLAYER%")
public static void greet(HandlerContext ctx, @Inject Player who) {
    // lumen:compile
    String greeting = ctx.choice(0).equals("formal") ? "Greetings" : "yo";
    ctx.codegen().addImport("java.lang.System");
    ctx.out().line("System.out.println(\"chosen at codegen: " + greeting + "\");");
    // lumen:runtime
    who.sendMessage(greeting);
}
```

The compile section runs once per call site; the runtime body is emitted into the script class as before.

## Scope rules

- `@Inject` parameters cannot be referenced inside a compile section. They have no value at codegen time.
- `HandlerContext` cannot be referenced inside a runtime section. It exists only at codegen.

Both rules are enforced at build time with a precise diagnostic at the offending line.

## Section ordering

Either order is allowed:

```java
// lumen:runtime
who.sendMessage("watch out");
// lumen:compile
ctx.out().line("System.out.println(\"warn fired\");");
```

The runtime section may also appear before the first marker (default phase is runtime):

```java
who.sendMessage("hello");
// lumen:compile
ctx.out().line("System.out.println(\"hello fired\");");
```

A handler may declare at most one `// lumen:compile` and one `// lumen:runtime` section. Multiple sections of the same phase are rejected at build time.
