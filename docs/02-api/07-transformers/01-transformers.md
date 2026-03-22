---
description: "How to create and register custom code transformers that modify generated code."
---

# Code Transformers

Code transformers let addons inspect the generated Java code after emission and modify it by removing, replacing, or inserting lines. The transformer system is tag based: each transformer declares the tags it owns through `tags()` and can only modify lines emitted with those tags.

Transformers are registered through `api.transformers()`, which returns a `TransformerRegistrar`.

## How the System Works

When code generation completes for a script, the pipeline runs all registered transformers against the emitted Java builder. 

The process runs per transformer: each transformer's modifications are applied before the next transformer runs, ensuring a consistent view of the code for each pass.

## Tags and Ownership

Every emitted line can have an optional tag string. When your addon emits code using `out.taggedLine(tag, code)`, that tag marks ownership. The `tags()` method on your transformer controls which lines you are allowed to modify:

- **Non-empty list:** owns only lines whose tag appears in the list.
- **Empty list:** owns all tagged lines (any non-null tag).
- **`null`:** owns every line, tagged and untagged alike.

The pipeline enforces this. If your transformer calls `ctx.remove(index)` on a line it does not own, the operation is silently ignored.

## Creating a Transformer

Implement the `CodeTransformer` interface:

```java
public class MyTransformer implements CodeTransformer {

    public static final String TAG = "my-addon-feature";
    private static final Pattern ASSIGNMENT = Pattern.compile("^(\\w+)\\s*=\\s*.+;$");

    @Override
    public @NotNull List<String> tags() {
        return List.of(TAG);
    }

    @Override
    public void transform(@NotNull TransformContext ctx) {
        for (TaggedLine line : ctx.lines()) {
            Matcher m = ASSIGNMENT.matcher(line.code().trim());
            if (!m.matches()) {
                continue;
            }
            String varName = m.group(1);

            if (!isUsedElsewhere(varName, line.index(), ctx.lines())) {
                ctx.remove(line.index());
            }
        }
    }

    private boolean isUsedElsewhere(String name, int myIndex, List<TaggedLine> lines) {
        for (TaggedLine line : lines) {
            if (line.index() == myIndex) continue;
            if (TAG.equals(line.tag())) continue;
            if (containsIdentifier(line.code(), name)) return true;
        }
        return false;
    }

    private boolean containsIdentifier(String code, String name) {
        int idx = 0;
        while (true) {
            idx = code.indexOf(name, idx);
            if (idx < 0) return false;
            boolean startOk = idx == 0 || !Character.isJavaIdentifierPart(code.charAt(idx - 1));
            int end = idx + name.length();
            boolean endOk = end >= code.length() || !Character.isJavaIdentifierPart(code.charAt(end));
            if (startOk && endOk) return true;
            idx++;
        }
    }
}
```

## Emitting Tagged Lines

To produce lines your transformer can manage, use `taggedLine` in your emit handler:

```java
api.patterns().statement(b -> b
    .by("MyAddon")
    .pattern("prepare %target:PLAYER%")
    .handler((line, ctx, out) -> {
        out.taggedLine(MyTransformer.TAG, "int health = (int) " + ctx.java("target") + ".getHealth();");
        out.line("doSomethingWith(" + ctx.java("target") + ");");
    })
);
```

The first line is tagged with your transformer's tag. If `health` is never used elsewhere, your transformer can remove it. The second line is untagged and cannot be touched by your transformer.

## Insertion Tagging

When a transformer inserts new lines, the pipeline decides the tag of each inserted line based on the transformer's `tags()` return value:

- **Exactly one tag:** inserted lines are automatically tagged with that tag, so the transformer owns them in later passes.
- **Otherwise (null, empty, or multiple tags):** inserted lines are untagged.

If your transformer needs to insert lines and modify them in a later pass, declare exactly one tag.

## Best Practices

- Always use a unique, descriptive tag string for your transformer. A good convention is `"addon-name-feature"`.
- Be conservative with modifications. If you are unsure whether a line is safe to remove or change, keep it. A false negative (keeping an unused line) is harmless, but a false positive (removing a needed line) breaks the script.
