---
description: "How to register custom script annotation binders that run when scripts are loaded or unloaded."
---

# Binders

Binders let addons define custom annotations that are automatically processed when a compiled script class is loaded or unloaded. Lumen's own event binding, command binding, and inventory binding all use this system.

Binders are registered through `api.binders()`, which returns a `ScriptBinderRegistrar`.

## How Binders Work

When a Lumen script is compiled and loaded, the binder system scans the compiled class for annotations and invokes every registered `ScriptAnnotationBinder`. Each binder can look for its own annotations and wire them up however it needs.

When a script is unloaded, each binder's `unbind` method is called so it can clean up any registered resources.

## Creating a Binder

A binder implements `ScriptAnnotationBinder`:

```java
public class MyBinder implements ScriptAnnotationBinder {

    @Override
    public void bind(@NotNull Object instance, @NotNull Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            MyAnnotation ann = method.getAnnotation(MyAnnotation.class);
            if (ann == null) continue;
            // wire up the annotated method
        }
    }

    @Override
    public void unbind(@NotNull Object instance) {
        // clean up anything bound for this instance
    }
}
```

The `bind` method receives the script instance and its class. Iterate over declared methods, check for your annotation, and perform whatever setup you need.

The `unbind` method is called when the script is unloaded. Remove any listeners, registrations, or state tied to that instance.

## Registering a Binder

Register your binder in your addon's `onEnable`:

```java
@Override
public void onEnable(@NotNull LumenAPI api) {
    api.binders().register(new MyBinder());
}
```
