---
description: "Inspect and modify emitted Java code after code generation."
---

# Code Transformers

A transformer runs after code generation completes, before the class is assembled and compiled. It sees every line the
pipeline emitted and can remove, replace, or insert lines. Ownership is tag-based: a transformer can only modify lines
whose tag it declares.

Register transformers through `api.transformers()`.

## Tags and Ownership

Emit a line with a tag using `out.taggedLine(tag, code)`. Your transformer's `tags()` method declares what it owns:

- A non-empty list owns only lines with matching tags
- An empty list owns every tagged line
- `null` owns every line, tagged or not

Attempts to modify unowned lines are silently dropped.

## Operations

`TransformContext` exposes the full set of modifications:

```java
ctx.remove(index)
ctx.replace(index, newCode)
ctx.insertBefore(index, code)
ctx.insertAfter(index, code)
```

## Multi-Pass Execution

Transformers run in a fixed-point loop until the output stabilizes. Removing a variable may expose another as unused,
which gets removed on the next pass, and so on.

## Example

This transformer wraps every tagged call site with a timer, then injects a field and helper method into the class to
accumulate timing data.

Tag your emit lines so the transformer can find them:

```java
public static final String TAG = "my-addon-profiler";

// inside your handler:
out.taggedLine(TAG, "long result = expensiveCall(" + args + ");");
```

Then write the transformer:

```java
public class ProfilingTransformer implements CodeTransformer {

    public static final String TAG = "my-addon-profiler";
    private static final Pattern VAR_DECL = Pattern.compile(
        "^(?:final\\s+)?\\w+(?:<[^>]+>)?\\s+(\\w+)\\s*=.+;$");

    @Override
    public @NotNull List<String> tags() {
        return List.of(TAG);
    }

    @Override
    public void transform(@NotNull TransformContext ctx) {
        boolean injected = false;

        for (TaggedLine line : ctx.lines()) {
            Matcher m = VAR_DECL.matcher(line.code().trim());
            if (!m.matches()) continue;
            String varName = m.group(1);

            int i = line.index();
            ctx.insertBefore(i, "long _start_" + varName + " = System.nanoTime();");
            ctx.insertAfter(i, "recordTime(System.nanoTime() - _start_" + varName + ");");

            if (!injected) {
                ctx.codegen().addField("private long _totalNanos = 0;");
                ctx.codegen().addMethod("private void recordTime(long nanos) { _totalNanos += nanos; }");
                injected = true;
            }
        }
    }
}
```

`VAR_DECL` extracts the variable name from any tagged assignment line. For each match, the transformer wraps it with a
nanosecond timer and, on the first hit, injects a `_totalNanos` field and `recordTime` helper directly into the script
class via `codegen()`. No handler touches the class definition.

## Inserted Line Tagging

If your transformer declares exactly one tag, inserted lines inherit that tag automatically. With zero, multiple, or a
null tag list, inserted lines are untagged. Use a single tag when a later pass needs to modify code that an earlier pass
introduced.

## Configuration

Transformers only run when `language.experimental.code-transform` is enabled in config.yml.
