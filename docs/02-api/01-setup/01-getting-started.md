---
description: "How to create a Lumen addon, register it, and understand the addon lifecycle."
---

# Getting Started

Lumen addons extend the language by registering custom patterns, events, types, placeholders, and more. Everything built into Lumen uses the same API that addons use, so addons have the same capabilities as the core language.

## Adding the Dependency

Before creating an addon, add the Lumen API as a dependency in your project.

### Gradle

```groovy
repositories {
    maven { url = "https://repo.lumenlang.dev" }
}

dependencies {
    compileOnly 'dev.lumenlang:lumen-api:VERSION'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>lumen</id>
        <url>https://repo.lumenlang.dev</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>dev.lumenlang</groupId>
        <artifactId>lumen-api</artifactId>
        <version>VERSION</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Replace `VERSION` with the latest Lumen API version.

## Creating an Addon

An addon is a class that implements `LumenAddon`. It provides a name, description, version, and an `onEnable` method where you register your features.

```java
public class MyAddon implements LumenAddon {

    @Override
    public @NotNull String name() {
        return "MyAddon";
    }

    @Override
    public @NotNull String description() {
        return "Adds custom patterns and events";
    }

    @Override
    public @NotNull String version() {
        return "1.0.0";
    }

    @Override
    public void onEnable(@NotNull LumenAPI api) {
        api.patterns().statement(
            "heal %who:PLAYER%",
            (line, ctx, out) -> out.line(ctx.java("who") + ".setHealth(20);")
        );
    }
}
```

## Registering an Addon

There are two ways to register an addon with Lumen.

### Plugin Based

If your addon is a Bukkit plugin, declare `depend: [Lumen]` in your `plugin.yml` and call `LumenProvider.registerAddon()` from your plugin's `onLoad()`:

```java
public class MyPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        LumenProvider.registerAddon(new MyAddon());
    }
}
```

It is recommended to register the addon during the plugin's `onLoad()` phase. Registering later may cause issues if `enable-all-scripts-immediately-on-startup` is enabled, since scripts may begin loading before the addon is registered.

### Jar Based

Place a jar in the `plugins/Lumen/addons/` directory. The jar needs a service file at `META-INF/services/dev.lumenlang.lumen.api.LumenAddon` that lists your addon class:

```
com.example.myaddon.MyAddon
```

Lumen discovers jar based addons automatically on startup.

#### Gradle Setup

If you use Gradle, the easiest way to generate the service file is with the `com.google.auto.service` annotation processor. Add it to your dependencies:

```groovy
annotationProcessor 'com.google.auto.service:auto-service:1.1.1'
compileOnly 'com.google.auto.service:auto-service-annotations:1.1.1'
```

Then annotate your addon class:

```java
@AutoService(LumenAddon.class)
public class MyAddon implements LumenAddon {
    // ...
}
```

This generates the `META-INF/services` file automatically at compile time.

If you prefer to do it manually, create the file yourself at `src/main/resources/META-INF/services/dev.lumenlang.lumen.api.LumenAddon` with your addon's fully qualified class name inside.

#### Maven Setup

With Maven, you can use the same `auto-service` approach:

```xml
<dependency>
    <groupId>com.google.auto.service</groupId>
    <artifactId>auto-service-annotations</artifactId>
    <version>1.1.1</version>
    <scope>provided</scope>
</dependency>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>com.google.auto.service</groupId>
                        <artifactId>auto-service</artifactId>
                        <version>1.1.1</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Or manually create `src/main/resources/META-INF/services/dev.lumenlang.lumen.api.LumenAddon` with your class name.

## Accessing the API Directly

If you do not need the full addon lifecycle, you can access the API directly from any Bukkit plugin that depends on Lumen:

```java
LumenAPI api = LumenProvider.api();
if (api != null) {
    api.patterns().statement(
        "heal %who:PLAYER%",
        (line, ctx, out) -> out.line(ctx.java("who") + ".setHealth(20);")
    );
}
```

`LumenProvider.api()` returns `null` if Lumen has not finished enabling yet. Plugins that declare `depend: [Lumen]` are guaranteed to get a non null result in their own `onEnable()`.

## The LumenAPI Handle

The `LumenAPI` handle is how you access all of Lumen's registrars. Through it you can register patterns, types, events, ref types, placeholders, and emit handlers. Each registrar is covered in its own documentation page.

```java
@Override
public void onEnable(@NotNull LumenAPI api) {
    PatternRegistrar patterns = api.patterns();
    TypeRegistrar types = api.types();
    EventRegistrar events = api.events();
    RefTypeRegistrar refTypes = api.refTypes();
    PlaceholderRegistrar placeholders = api.placeholders();
    EmitRegistrar emitters = api.emitters();
    ScriptBinderRegistrar binders = api.binders();
}
```
