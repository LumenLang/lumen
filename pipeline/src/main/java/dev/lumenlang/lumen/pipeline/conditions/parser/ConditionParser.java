package dev.lumenlang.lumen.pipeline.conditions.parser;

import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.conditions.ConditionAnd;
import dev.lumenlang.lumen.pipeline.conditions.ConditionAtom;
import dev.lumenlang.lumen.pipeline.conditions.ConditionExpr;
import dev.lumenlang.lumen.pipeline.conditions.ConditionInline;
import dev.lumenlang.lumen.pipeline.conditions.ConditionOr;
import dev.lumenlang.lumen.pipeline.conditions.registry.ConditionRegistry;
import dev.lumenlang.lumen.pipeline.conditions.registry.RegisteredConditionMatch;
import dev.lumenlang.lumen.pipeline.language.exceptions.TokenCarryingException;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.resolve.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.pipeline.var.VarRef;

import java.util.ArrayList;
import java.util.List;

import static dev.lumenlang.lumen.pipeline.language.resolve.ExprResolver.joinTokens;

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

    private static List<List<Token>> splitOn(List<Token> tokens, String keyword) {
        List<List<Token>> segments = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int braceDepth = 0;

        for (Token token : tokens) {
            if (token.kind() == TokenKind.SYMBOL && token.text().equals("{")) {
                braceDepth++;
                current.add(token);
            } else if (token.kind() == TokenKind.SYMBOL && token.text().equals("}")) {
                braceDepth = Math.max(0, braceDepth - 1);
                current.add(token);
            } else if (braceDepth == 0 && token.text().equalsIgnoreCase(keyword)) {
                segments.add(current);
                current = new ArrayList<>();
            } else {
                current.add(token);
            }
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
    public ConditionExpr parse(List<Token> tokens, TypeEnv env) {
        List<List<Token>> orSegments = splitOn(tokens, "or");

        if (orSegments.size() == 1) {
            return parseAndChain(orSegments.get(0), env);
        }

        List<ConditionExpr> orParts = new ArrayList<>();
        for (List<Token> segment : orSegments) {
            orParts.add(parseAndChain(segment, env));
        }
        return new ConditionOr(orParts);
    }

    private ConditionExpr parseAndChain(List<Token> tokens, TypeEnv env) {
        List<List<Token>> andSegments = splitOn(tokens, "and");

        if (andSegments.size() == 1) {
            return parseAtom(andSegments.get(0), env);
        }

        List<ConditionExpr> andParts = new ArrayList<>();
        for (List<Token> segment : andSegments) {
            andParts.add(parseAtom(segment, env));
        }
        return new ConditionAnd(andParts);
    }

    private ConditionExpr parseAtom(List<Token> tokens, TypeEnv env) {
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
            String expanded = PlaceholderExpander.expand(tokens.get(0).text(), env);
            return new ConditionInline(expanded);
        }

        RegisteredConditionMatch slowM = registry.matchSlow(tokens, env);
        if (slowM != null) {
            return new ConditionAtom(slowM);
        }

        List<PatternSimulator.Suggestion> suggestions = PatternSimulator.suggestConditions(tokens, PatternRegistry.instance(), env);
        throw new TokenCarryingException("Unknown condition: " + joinTokens(tokens), tokens, suggestions);
    }
}
