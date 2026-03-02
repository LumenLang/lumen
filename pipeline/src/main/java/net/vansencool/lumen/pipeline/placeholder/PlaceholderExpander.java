package net.vansencool.lumen.pipeline.placeholder;

import net.vansencool.lumen.api.placeholder.PlaceholderType;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.var.RefType;
import net.vansencool.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Expands placeholder expressions embedded in strings and var expressions at compile time.
 *
 * <p>Placeholders use the syntax {@code {variable_property}} or {@code {variable}} inside
 * string literals and var expressions. The expander scans the input, and for each placeholder:
 * <ol>
 *   <li>Splits on the first {@code _} to get the variable name and property</li>
 *   <li>Looks up the variable in the scope to get its {@link RefType}</li>
 *   <li>Looks up the property template in the {@link PlaceholderRegistry}</li>
 *   <li>Produces a Java expression</li>
 * </ol>
 *
 * <h2>Example</h2>
 * <p>Input string: {@code "Hello {player_name}, hp {player_health}"}
 * <p>Output Java: {@code "Hello " + player.getName() + ", hp " + String.valueOf(player.getHealth())}
 *
 * <p>In var expressions:
 * <pre>{@code var y = {player_y}   ->   var y = player.getLocation().getBlockY();}</pre>
 */
public final class PlaceholderExpander {

    /**
     * Expands all placeholders in the given raw string value and produces a Java expression.
     *
     * <p>If the string contains no placeholders, returns the string wrapped in quotes as-is.
     * Numeric placeholders are wrapped in {@code String.valueOf()} for string context.
     *
     * @param raw the raw string content (without surrounding quotes)
     * @param env the type environment for variable lookups
     * @return a Java expression string (may use concatenation for embedded placeholders)
     */
    public static @NotNull String expand(@NotNull String raw, @NotNull TypeEnv env) {
        if (!raw.contains("{") || !raw.contains("}")) {
            return "\"" + escapeJava(raw) + "\"";
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        boolean first = true;

        while (i < raw.length()) {
            int open = raw.indexOf('{', i);
            if (open == -1) {
                appendLiteral(result, raw.substring(i), first);
                break;
            }

            int close = raw.indexOf('}', open);
            if (close == -1) {
                appendLiteral(result, raw.substring(i), first);
                break;
            }

            if (open > i) {
                appendLiteral(result, raw.substring(i, open), first);
                first = false;
            }

            String placeholder = raw.substring(open + 1, close);
            String javaExpr = resolveForString(placeholder, env);
            if (!first) result.append(" + ");
            result.append(javaExpr);
            first = false;

            i = close + 1;
        }

        return result.toString();
    }

    /**
     * Resolves a placeholder expression (e.g. {@code "player_y"}) into a raw Java expression.
     * This is used by ExprParser and MathEngine when placeholders appear outside of strings.
     *
     * <p>Unlike {@link #resolveForString}, this does NOT wrap numeric results in
     * {@code String.valueOf()}, making the result suitable for math and var assignments.
     *
     * @param placeholder the placeholder text without braces (e.g. "player_y")
     * @param env         the type environment for variable lookups
     * @return the raw Java expression, or {@code null} if the placeholder cannot be resolved
     */
    public static @Nullable String resolveForExpression(@NotNull String placeholder, @NotNull TypeEnv env) {
        return resolveInternal(placeholder, env);
    }

    /**
     * Returns the {@link PlaceholderType} of a placeholder property, or {@code null} if
     * the placeholder cannot be resolved.
     *
     * @param placeholder the placeholder text without braces (e.g. "player_y")
     * @param env         the type environment for variable lookups
     * @return the placeholder type, or {@code null} if unresolvable
     */
    public static @Nullable PlaceholderType resolveType(@NotNull String placeholder, @NotNull TypeEnv env) {
        placeholder = placeholder.replace(' ', '_');
        int underscore = placeholder.indexOf('_');
        if (underscore == -1) return null;

        String varName = placeholder.substring(0, underscore);
        String property = placeholder.substring(underscore + 1);

        VarRef ref = env.lookupVar(varName);
        if (ref == null || ref.refType() == null) return null;

        return PlaceholderRegistry.getPropertyType(ref.refType(), property);
    }

    /**
     * Resolves a placeholder for string context. Numeric results are wrapped in
     * {@code String.valueOf()} so they can be concatenated with strings.
     */
    private static @NotNull String resolveForString(@NotNull String placeholder, @NotNull TypeEnv env) {
        String java = resolveInternal(placeholder, env);
        if (java == null) {
            return "\"<unknown:" + placeholder + ">\"";
        }

        PlaceholderType type = resolveType(placeholder, env);
        if (type == PlaceholderType.NUMBER) {
            return "String.valueOf(" + java + ")";
        }
        return java;
    }

    /**
     * Core resolution logic shared by string and expression contexts.
     */
    private static @Nullable String resolveInternal(@NotNull String placeholder, @NotNull TypeEnv env) {
        placeholder = placeholder.replace(' ', '_');
        VarRef fullRef = env.lookupVar(placeholder);
        if (fullRef != null) {
            RefType type = fullRef.refType();
            if (type == null) {
                return "String.valueOf(" + fullRef.java() + ")";
            }
            String defaultProp = PlaceholderRegistry.getDefault(type);
            if (defaultProp == null) {
                return "String.valueOf(" + fullRef.java() + ")";
            }
            String template = PlaceholderRegistry.getProperty(type, defaultProp);
            if (template == null) {
                return "String.valueOf(" + fullRef.java() + ")";
            }
            String guardedVar = fullRef.hasMeta("nullable")
                    ? fullRef.java()
                    : "NullGuard.of(" + fullRef.java() + ", \"" + placeholder + "\")";
            return PlaceholderRegistry.expand(template, guardedVar);
        }

        int underscore = placeholder.indexOf('_');
        String varName;
        String property;

        if (underscore == -1) {
            return null;
        }

        varName = placeholder.substring(0, underscore);
        property = placeholder.substring(underscore + 1);

        VarRef ref = env.lookupVar(varName);
        if (ref == null) {
            return null;
        }

        RefType type = ref.refType();
        if (type == null) {
            return null;
        }

        String template = PlaceholderRegistry.getProperty(type, property);
        if (template == null) {
            return null;
        }

        String guardedVar = ref.hasMeta("nullable")
                ? ref.java()
                : "NullGuard.of(" + ref.java() + ", \"" + varName + "\")";
        return PlaceholderRegistry.expand(template, guardedVar);
    }

    private static void appendLiteral(@NotNull StringBuilder sb, @NotNull String text, boolean first) {
        if (text.isEmpty()) return;
        if (!first) sb.append(" + ");
        sb.append("\"").append(escapeJava(text)).append("\"");
    }

    private static @NotNull String escapeJava(@NotNull String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
