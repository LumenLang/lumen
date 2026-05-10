package dev.lumenlang.build.validate;

import dev.lumenlang.build.result.Diagnostic;
import dev.lumenlang.build.result.Severity;
import dev.lumenlang.build.scan.handler.ScannedHandler;
import dev.lumenlang.build.scan.param.InjectParam;
import dev.lumenlang.build.source.ParsedHandlerSource;
import dev.lumenlang.build.validate.inject.BindingTypeTable;
import dev.lumenlang.build.validate.inject.Placeholders;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-checks every {@code @Inject} parameter on a scanned handler against
 * the JVMS field descriptor declared by its placeholder's type binding.
 *
 * <p>Each handler may carry many patterns; the binding for a given
 * placeholder name must agree across all of them. Disagreement is itself a
 * diagnostic.
 *
 * <p>Unknown binding ids (not present in the supplied {@link BindingTypeTable})
 * are skipped without diagnostic. The table is best-effort and other addons
 * register their own bindings the build plugin cannot see.
 */
public final class InjectTypeValidator {

    private InjectTypeValidator() {
    }

    public static @NotNull List<Diagnostic> validate(@NotNull ScannedHandler scanned, @NotNull ParsedHandlerSource parsed, @NotNull BindingTypeTable table) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        Map<String, String> placeholderBindings = mergePlaceholders(scanned.patterns(), parsed, diagnostics);
        if (placeholderBindings.isEmpty()) return diagnostics;

        for (InjectParam param : scanned.injectParams()) {
            String name = param.placeholderName();
            String bindingId = placeholderBindings.get(name);
            if (bindingId == null) {
                diagnostics.add(new Diagnostic(Severity.ERROR, "@Inject parameter '" + name + "' does not match any placeholder in the handler's pattern(s)", parsed.sourceFile(), parsed.method().line(), 0));
                continue;
            }

            String expected = table.descriptorOf(bindingId);
            if (expected == null) continue;

            if (!expected.equals(param.javaType())) {
                diagnostics.add(new Diagnostic(Severity.ERROR, "@Inject parameter '" + name + "' has type '" + param.javaType() + "' but binding '" + bindingId + "' produces '" + expected + "'", parsed.sourceFile(), parsed.method().line(), 0));
            }
        }

        return diagnostics;
    }

    private static @NotNull Map<String, String> mergePlaceholders(@NotNull List<String> patterns, @NotNull ParsedHandlerSource parsed, @NotNull List<Diagnostic> diagnostics) {
        Map<String, String> merged = new HashMap<>();
        for (String pattern : patterns) {
            for (Map.Entry<String, String> entry : Placeholders.bindings(pattern).entrySet()) {
                String name = entry.getKey();
                String binding = entry.getValue();
                String existing = merged.putIfAbsent(name, binding);
                if (existing != null && !existing.equals(binding)) {
                    diagnostics.add(new Diagnostic(Severity.ERROR, "Placeholder '" + name + "' uses conflicting bindings across patterns: '" + existing + "' and '" + binding + "'", parsed.sourceFile(), parsed.method().line(), 0));
                }
            }
        }
        return merged;
    }
}
