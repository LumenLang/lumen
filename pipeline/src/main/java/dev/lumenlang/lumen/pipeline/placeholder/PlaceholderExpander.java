package dev.lumenlang.lumen.pipeline.placeholder;

import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.placeholder.PlaceholderType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Expands placeholder expressions embedded in strings and var expressions at compile time.
 *
 * <p>Two source-tracking entry points exist so unknown placeholders can be reported
 * with precise source columns:
 * <ul>
 *   <li>{@link #expandString} for placeholders embedded inside a single STRING token,
 *       where the surrounding token's source bounds give the column origin.</li>
 *   <li>{@link #expandTokens} for placeholders that appear as separate {@code SYMBOL}
 *       and {@code IDENT} tokens, where each placeholder token already carries its
 *       own column.</li>
 * </ul>
 */
public final class PlaceholderExpander {

    /**
     * Expands placeholders embedded inside a single STRING token's content.
     *
     * <p>The original source slice for the string is recovered from the current block's
     * raw line text and the token's column bounds, so {@code {placeholder}} positions
     * are reported with their true source columns even when the content contains
     * escape sequences that differ in length from the source.
     *
     * @param stringToken the originating STRING token whose content holds placeholders
     * @param env         the type environment for variable lookups and warning emission
     * @return a Java expression string (may use concatenation for embedded placeholders)
     */
    public static @NotNull String expandString(@NotNull Token stringToken, @NotNull TypeEnvImpl env) {
        String raw = stringToken.text();
        if (raw.indexOf('{') < 0 || raw.indexOf('}') < 0) {
            return "\"" + escapeJava(raw) + "\"";
        }

        String sourceLine = env.block().raw();
        int sourceLineNumber = stringToken.line();
        int contentStartCol = stringToken.start() + 1;
        int contentEndCol = stringToken.end() - 1;
        String sourceContent = (contentStartCol >= 0 && contentEndCol <= sourceLine.length() && contentStartCol <= contentEndCol)
                ? sourceLine.substring(contentStartCol, contentEndCol)
                : raw;

        StringBuilder result = new StringBuilder();
        int sourcePos = 0;
        int contentPos = 0;
        boolean first = true;

        while (sourcePos < sourceContent.length()) {
            int srcOpen = sourceContent.indexOf('{', sourcePos);
            if (srcOpen < 0) {
                if (contentPos < raw.length()) {
                    appendLiteral(result, raw.substring(contentPos), first);
                }
                break;
            }
            int srcClose = sourceContent.indexOf('}', srcOpen);
            if (srcClose < 0) {
                if (contentPos < raw.length()) {
                    appendLiteral(result, raw.substring(contentPos), first);
                }
                break;
            }

            int contentOpen = mapSourceToContent(sourceContent, raw, srcOpen);
            int contentClose = mapSourceToContent(sourceContent, raw, srcClose);

            if (contentOpen > contentPos) {
                appendLiteral(result, raw.substring(contentPos, contentOpen), first);
                first = false;
            }

            String placeholder = raw.substring(contentOpen + 1, contentClose);
            int absColStart = contentStartCol + srcOpen;
            int absColEnd = contentStartCol + srcClose + 1;
            String javaExpr = resolveForString(placeholder, env, sourceLineNumber, sourceLine, absColStart, absColEnd);
            if (!first) result.append(" + ");
            result.append(javaExpr);
            first = false;

            sourcePos = srcClose + 1;
            contentPos = contentClose + 1;
        }

        return result.toString();
    }

    /**
     * Expands placeholders that appear as discrete tokens in the input list.
     *
     * <p>Used when the source has been tokenized so each {@code {}, identifier, }} is
     * its own token; columns come straight from those tokens. Non-placeholder tokens
     * are concatenated as string literals using their source text.
     *
     * @param tokens the source tokens forming the value
     * @param env    the type environment for variable lookups and warning emission
     * @return a Java expression string
     */
    public static @NotNull String expandTokens(@NotNull List<Token> tokens, @NotNull TypeEnvImpl env) {
        if (tokens.isEmpty()) return "\"\"";

        String sourceLine = env.block().raw();
        int sourceLineNumber = tokens.get(0).line();

        StringBuilder result = new StringBuilder();
        StringBuilder literalBuf = new StringBuilder();
        boolean first = true;
        int i = 0;
        while (i < tokens.size()) {
            Token t = tokens.get(i);
            if (t.kind() == TokenKind.SYMBOL && t.text().equals("{")
                    && i + 2 < tokens.size()
                    && tokens.get(i + 1).kind() == TokenKind.IDENT
                    && tokens.get(i + 2).kind() == TokenKind.SYMBOL && tokens.get(i + 2).text().equals("}")) {
                if (literalBuf.length() > 0) {
                    appendLiteral(result, literalBuf.toString(), first);
                    first = false;
                    literalBuf.setLength(0);
                }
                String placeholder = tokens.get(i + 1).text();
                int absColStart = t.start();
                int absColEnd = tokens.get(i + 2).end();
                String javaExpr = resolveForString(placeholder, env, sourceLineNumber, sourceLine, absColStart, absColEnd);
                if (!first) result.append(" + ");
                result.append(javaExpr);
                first = false;
                i += 3;
                continue;
            }
            if (literalBuf.length() > 0) literalBuf.append(' ');
            literalBuf.append(t.text());
            i++;
        }
        if (literalBuf.length() > 0) {
            appendLiteral(result, literalBuf.toString(), first);
        }
        if (result.length() == 0) {
            return "\"\"";
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
    public static @Nullable String resolveForExpression(@NotNull String placeholder, @NotNull TypeEnvImpl env) {
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
    public static @Nullable PlaceholderType resolveType(@NotNull String placeholder, @NotNull TypeEnvImpl env) {
        placeholder = placeholder.replace(' ', '_');
        int underscore = placeholder.indexOf('_');
        if (underscore == -1) return null;

        String varName = placeholder.substring(0, underscore);
        String property = placeholder.substring(underscore + 1);

        VarRef ref = env.lookupVar(varName);
        if (ref == null || ref.objectType() == null) return null;

        return PlaceholderRegistry.getPropertyType(ref.objectType(), property);
    }

    /**
     * Returns the {@link LumenType} of a placeholder expression.
     *
     * <p>Defaults to {@link PrimitiveType#STRING} when the placeholder type cannot be determined.
     *
     * @param placeholder the placeholder text without braces (e.g. "player_y")
     * @param env         the type environment for variable lookups
     * @return the lumen type
     */
    public static @NotNull LumenType resolveExpressionType(@NotNull String placeholder, @NotNull TypeEnvImpl env) {
        PlaceholderType phType = resolveType(placeholder, env);
        if (phType == null) return PrimitiveType.STRING;
        return switch (phType) {
            case STRING -> PrimitiveType.STRING;
            case NUMBER -> PrimitiveType.DOUBLE;
            case BOOLEAN -> PrimitiveType.BOOLEAN;
        };
    }

    private static @NotNull String resolveForString(@NotNull String placeholder, @NotNull TypeEnvImpl env, int line, @NotNull String sourceLine, int absColStart, int absColEnd) {
        String java = resolveInternal(placeholder, env);
        if (java == null) {
            env.addWarning(LumenDiagnostic.warning("Unknown placeholder '" + placeholder + "'")
                    .at(line, sourceLine)
                    .highlight(absColStart, absColEnd)
                    .label("placeholder cannot be resolved")
                    .note("the value will be emitted as the literal text \"<unknown:" + placeholder + ">\" at runtime")
                    .help("check the spelling, ensure the variable exists in scope, and confirm the property is registered for that type")
                    .build());
            return "\"<unknown:" + placeholder + ">\"";
        }

        PlaceholderType type = resolveType(placeholder, env);
        if (type == PlaceholderType.NUMBER) {
            return "String.valueOf(" + java + ")";
        }
        return java;
    }

    private static @Nullable String resolveInternal(@NotNull String placeholder, @NotNull TypeEnvImpl env) {
        placeholder = placeholder.replace(' ', '_');
        VarRef fullRef = env.lookupVar(placeholder);
        if (fullRef != null) {
            ObjectType type = fullRef.objectType();
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
            return PlaceholderRegistry.expand(template, fullRef.java());
        }

        int underscore = placeholder.indexOf('_');
        if (underscore == -1) {
            return null;
        }

        String varName = placeholder.substring(0, underscore);
        String property = placeholder.substring(underscore + 1);

        VarRef ref = env.lookupVar(varName);
        if (ref == null) {
            return null;
        }

        ObjectType type = ref.objectType();
        if (type == null) {
            return null;
        }

        String template = PlaceholderRegistry.getProperty(type, property);
        if (template == null) {
            return null;
        }

        return PlaceholderRegistry.expand(template, ref.java());
    }

    private static int mapSourceToContent(@NotNull String sourceContent, @NotNull String content, int sourceIdx) {
        int s = 0;
        int c = 0;
        while (s < sourceIdx && s < sourceContent.length() && c < content.length()) {
            if (sourceContent.charAt(s) == '\\' && s + 1 < sourceContent.length()) {
                s += 2;
            } else {
                s++;
            }
            c++;
        }
        return c;
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
