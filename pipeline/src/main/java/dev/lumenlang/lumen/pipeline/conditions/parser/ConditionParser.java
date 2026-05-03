package dev.lumenlang.lumen.pipeline.conditions.parser;

import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.conditions.ConditionAnd;
import dev.lumenlang.lumen.pipeline.conditions.ConditionAtom;
import dev.lumenlang.lumen.pipeline.conditions.ConditionExpr;
import dev.lumenlang.lumen.pipeline.conditions.ConditionInline;
import dev.lumenlang.lumen.pipeline.conditions.ConditionOr;
import dev.lumenlang.lumen.pipeline.conditions.registry.ConditionRegistry;
import dev.lumenlang.lumen.pipeline.conditions.registry.RegisteredConditionMatch;
import dev.lumenlang.lumen.pipeline.language.exceptions.TokenCarryingException;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Parses a flat token list representing a boolean condition into a {@link ConditionExpr} tree.
 *
 * <p>The parser respects standard boolean operator precedence:
 * <ol>
 *   <li>{@code or} has the lowest precedence  -  the token stream is first split on {@code or}</li>
 *   <li>{@code and} has higher precedence  -  each {@code or}-segment is then split on {@code and}</li>
 * </ol>
 *
 * <p>Examples:
 * <pre>
 * // Single atom
 * player health &gt; 5
 *   → ConditionAtom
 *
 * // 'and' only
 * player health &gt; 5 and player is sneaking
 *   → ConditionAnd([atom1, atom2])
 *
 * // 'or' only
 * player is sneaking or player is flying
 *   → ConditionOr([atom1, atom2])
 *
 * // Mixed  -  'and' binds tighter than 'or'
 * player health &gt; 5 and player is sneaking or player is flying
 *   → ConditionOr([ConditionAnd([atom1, atom2]), atom3])
 * </pre>
 *
 * @see ConditionExpr
 * @see ConditionRegistry
 */
public final class ConditionParser {

    private final ConditionRegistry registry;

    /**
     * Creates a new {@code ConditionParser} backed by the given condition registry.
     *
     * @param registry the registry used to match individual condition atoms
     */
    public ConditionParser(ConditionRegistry registry) {
        this.registry = registry;
    }

    private List<List<Token>> splitOn(List<Token> tokens, String keyword) {
        List<List<Token>> segments = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int braceDepth = 0;
        int pendingAbsorbed = 0;
        Map<String, ConditionRegistry.KeywordAbsorption> protectedMap = registry.splitProtectedKeywords();
        boolean isAndSplit = keyword.equalsIgnoreCase("and");
        boolean isOrSplit = keyword.equalsIgnoreCase("or");

        for (Token token : tokens) {
            if (token.kind() == TokenKind.SYMBOL && token.text().equals("{")) {
                braceDepth++;
                current.add(token);
                continue;
            }
            if (token.kind() == TokenKind.SYMBOL && token.text().equals("}")) {
                braceDepth = Math.max(0, braceDepth - 1);
                current.add(token);
                continue;
            }
            if (braceDepth != 0) {
                current.add(token);
                continue;
            }
            String lower = token.text().toLowerCase(Locale.ROOT);
            if (token.text().equalsIgnoreCase(keyword)) {
                if (pendingAbsorbed > 0) {
                    pendingAbsorbed--;
                    current.add(token);
                } else {
                    segments.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            ConditionRegistry.KeywordAbsorption absorption = protectedMap.get(lower);
            if (absorption != null) {
                pendingAbsorbed += isAndSplit ? absorption.absorbsAnd() : isOrSplit ? absorption.absorbsOr() : 0;
            }
            current.add(token);
        }
        segments.add(current);
        return segments;
    }

    /**
     * Attempts to extract a variable name from the token list.
     *
     * <p>Handles two forms:
     * <ul>
     *   <li>A single IDENT token: {@code myVar}</li>
     *   <li>A curly-brace wrapped IDENT: <code>{myVar}</code> (3 tokens)</li>
     * </ul>
     *
     * @param tokens the tokens to inspect
     * @return the variable name, or {@code null} if the tokens do not form a variable reference
     */
    private static String extractVarName(List<Token> tokens) {
        if (tokens.size() == 1 && tokens.get(0).kind() == TokenKind.IDENT) {
            return tokens.get(0).text();
        }
        if (tokens.size() == 3
                && tokens.get(0).kind() == TokenKind.SYMBOL && tokens.get(0).text().equals("{")
                && tokens.get(1).kind() == TokenKind.IDENT
                && tokens.get(2).kind() == TokenKind.SYMBOL && tokens.get(2).text().equals("}")) {
            return tokens.get(1).text();
        }
        return null;
    }

    /**
     * Parses the given token list into a {@link ConditionExpr}.
     *
     * <p>The token list is first split on {@code or} keywords. Each segment is then split on
     * {@code and} keywords and matched against the condition registry. Precedence follows
     * standard boolean logic: {@code and} binds tighter than {@code or}.
     *
     * @param tokens the condition tokens (e.g. from after the {@code if} keyword)
     * @param env    the current type environment for variable resolution
     * @return the parsed condition expression
     * @throws RuntimeException if any atom segment does not match a registered condition pattern
     */
    public ConditionExpr parse(List<Token> tokens, TypeEnvImpl env) {
        List<List<Token>> orSegments = splitOn(tokens, "or");

        if (orSegments.size() == 1) {
            return parseAndChain(orSegments.get(0), env);
        }

        validateNonEmptySegments(tokens, orSegments, "or", env);
        List<ConditionExpr> orParts = new ArrayList<>();
        for (List<Token> segment : orSegments) {
            orParts.add(parseAndChain(segment, env));
        }
        return new ConditionOr(orParts);
    }

    private ConditionExpr parseAndChain(List<Token> tokens, TypeEnvImpl env) {
        List<List<Token>> andSegments = splitOn(tokens, "and");

        if (andSegments.size() == 1) {
            return parseAtom(andSegments.get(0), env);
        }

        validateNonEmptySegments(tokens, andSegments, "and", env);
        List<ConditionExpr> andParts = new ArrayList<>();
        for (List<Token> segment : andSegments) {
            andParts.add(parseAtom(segment, env));
        }
        return new ConditionAnd(andParts);
    }

    private void validateNonEmptySegments(@NotNull List<Token> tokens, @NotNull List<List<Token>> segments, @NotNull String keyword, @NotNull TypeEnvImpl env) {
        for (int i = 0; i < segments.size(); i++) {
            if (!segments.get(i).isEmpty()) continue;
            Token sepToken = findSeparatorToken(tokens, keyword, i, segments.size());
            String message;
            String label;
            if (i == 0) {
                message = "Missing condition before '" + keyword + "'";
                label = "expected a condition before this '" + keyword + "'";
            } else if (i == segments.size() - 1) {
                message = "Missing condition after '" + keyword + "'";
                label = "expected a condition after this '" + keyword + "'";
            } else {
                message = "Missing condition between two '" + keyword + "' keywords";
                label = "expected a condition before this '" + keyword + "'";
            }
            LumenDiagnostic.Builder builder = LumenDiagnostic.error(message)
                    .at(env.blockContext().node().line(), env.blockContext().node().raw());
            if (sepToken != null) builder.highlight(sepToken.start(), sepToken.end());
            builder.label(label);
            List<PatternSimulator.Suggestion> suggestions = PatternSimulator.suggestConditions(tokens, PatternRegistry.instance(), env);
            if (!suggestions.isEmpty()) {
                String top = suggestions.get(0).pattern().raw();
                builder.help("did you mean to use a condition like: " + top);
                if (suggestions.size() >= 2) builder.help("also consider: " + suggestions.get(1).pattern().raw());
            } else {
                builder.help("write a condition such as 'x > 0' or 'player has permission \"...\"'");
            }
            throw new DiagnosticException(builder.build());
        }
    }

    private static @Nullable Token findSeparatorToken(@NotNull List<Token> tokens, @NotNull String keyword, int segmentIndex, int totalSegments) {
        int seen = 0;
        int targetSep = segmentIndex == totalSegments - 1 ? segmentIndex - 1 : segmentIndex;
        int braceDepth = 0;
        for (Token t : tokens) {
            if (t.kind() == TokenKind.SYMBOL && t.text().equals("{")) braceDepth++;
            else if (t.kind() == TokenKind.SYMBOL && t.text().equals("}")) braceDepth = Math.max(0, braceDepth - 1);
            else if (braceDepth == 0 && t.text().equalsIgnoreCase(keyword)) {
                if (seen == targetSep) return t;
                seen++;
            }
        }
        return null;
    }

    private ConditionExpr parseAtom(List<Token> tokens, TypeEnvImpl env) {
        RegisteredConditionMatch m = registry.match(tokens, env);
        if (m != null) {
            return new ConditionAtom(m);
        }

        String varName = extractVarName(tokens);
        if (varName != null) {
            VarRef ref = env.lookupVar(varName);
            if (ref != null) {
                return new ConditionInline(ref.java());
            }
            throw new TokenCarryingException(
                    "Variable '" + varName + "' does not exist",
                    tokens
            );
        }

        if (tokens.size() == 1 && tokens.get(0).kind() == TokenKind.STRING) {
            String expanded = PlaceholderExpander.expandString(tokens.get(0), env);
            return new ConditionInline(expanded);
        }

        RegisteredConditionMatch slowM = registry.matchSlow(tokens, env);
        if (slowM != null) {
            return new ConditionAtom(slowM);
        }

        List<PatternSimulator.Suggestion> suggestions = PatternSimulator.suggestConditions(tokens, PatternRegistry.instance(), env);
        throw new TokenCarryingException("Unknown condition", tokens, suggestions);
    }
}
