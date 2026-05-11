---
description: "Apply the Lumen build plugin so annotated handlers are scanned, validated, and bundled into the addon jar."
---

# Setup

Apply the build plugin in the addon's `build.gradle`:

```groovy
plugins {
    id 'java'
    id 'dev.lumenlang.lumen-build' version '1.3.0'
}
```

The plugin registers a `lumenBuild` task that runs after `compileJava` and writes two sidecars into the addon's resources:

- `META-INF/lumen/handlers.json` — kind, patterns, params, metadata for every annotated method.
- `META-INF/lumen/sources.gson.gz` — preserved source text the runtime emits at script codegen.

`jar` and `processResources` already depend on `lumenBuild`, so a normal `./gradlew build` runs everything in order.
