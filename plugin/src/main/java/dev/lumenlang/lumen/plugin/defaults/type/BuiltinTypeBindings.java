package dev.lumenlang.lumen.plugin.defaults.type;

import dev.lumenlang.lumen.api.exceptions.ParseFailureException;
import dev.lumenlang.lumen.api.type.TypeBindingMeta;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.TypeBinding;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Registers the built-in EXPR and STRING type bindings directly on the
 * {@link TypeRegistry} so they have access to full {@link Token} information
 * (including {@link TokenKind}). This is necessary because
 * the addon-level {@code AddonTypeBinding} only receives stripped text, losing
 * string-literal quoting.
 */
public final class BuiltinTypeBindings {

    private BuiltinTypeBindings() {
    }

    /**
     * Registers the EXPR, STRING, and QSTRING type bindings on the given registry.
     *
     * @param types the type registry to populate
     */
    public static void register(@NotNull TypeRegistry types) {
        registerExpr(types);
        registerString(types);
        registerQString(types);
        registerIdent(types);

        types.registerMeta("EXPR", new TypeBindingMeta(
                "Captures all remaining tokens as a raw expression. Preserves string literal quoting and is used for arbitrary sub-expressions.",
                "String",
                List.of("set %var:EXPR% to %val:EXPR%"),
                "1.0.0",
                false));
        types.registerMeta("STRING", new TypeBindingMeta(
                "Captures a single token as a string value. Resolves variable references and supports placeholders expansion.",
                "String",
                List.of("message player %text:STRING%"),
                "1.0.0",
                false));
        types.registerMeta("QSTRING", new TypeBindingMeta(
                "Captures a single quoted string literal, variable reference, or placeholder token. Rejects bare identifiers to prevent accidental matches with other patterns.",
                "String",
                List.of("%a:STRING% (is|equals) %b:QSTRING%"),
                "1.0.0",
                false));
        types.registerMeta("IDENT", new TypeBindingMeta(
                "Captures a single identifier token. Only matches bare identifiers, rejecting string literals, numbers, and other token kinds.",
                "String",
                List.of("set %name:IDENT% to %val:EXPR%"),
                "1.0.0",
                false));
    }

    private static void registerExpr(@NotNull TypeRegistry types) {
        types.register(new TypeBinding() {
            @Override
            public @NotNull String id() {
                return "EXPR";
            }

            @Override
            public Object parse(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
                if (tokens.isEmpty()) {
                    throw new ParseFailureException("expected an expression here");
                }
                return new TokenList(List.copyOf(tokens));
            }

            @Override
            public int consumeCount(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
                return -1;
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                TokenList tl = (TokenList) value;
                return tl.tokens().stream()
                        .map(t -> t.kind() == TokenKind.STRING
                                ? "\"" + escapeJava(t.text()) + "\""
                                : t.text())
                        .collect(Collectors.joining(" "));
            }
        });
    }

    private static void registerString(@NotNull TypeRegistry types) {
        types.register(new TypeBinding() {
            @Override
            public @NotNull String id() {
                return "STRING";
            }

            @Override
            public int consumeCount(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("expected a string value here");
                return 1;
            }

            @Override
            public Object parse(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
                if (tokens.size() == 1) {
                    Token t = tokens.get(0);
                    if (t.kind() != TokenKind.STRING) {
                        VarRef ref = env.lookupVar(t.text());
                        if (ref != null)
                            return ref;
                    }
                }
                return new TokenList(List.copyOf(tokens));
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (value instanceof VarRef ref) {
                    return "String.valueOf(" + ref.java() + ")";
                }
                TokenList tl = (TokenList) value;
                String raw = tl.tokens().stream().map(Token::text).collect(Collectors.joining(" "));
                return PlaceholderExpander.expand(raw, env);
            }
        });
    }

    private static void registerQString(@NotNull TypeRegistry types) {
        types.register(new TypeBinding() {
            @Override
            public @NotNull String id() {
                return "QSTRING";
            }

            @Override
            public int consumeCount(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("expected a quoted string value here");
                Token first = tokens.get(0);
                if (first.kind() == TokenKind.STRING) return 1;
                if (env.lookupVar(first.text()) != null) return 1;
                if (first.text().contains("{")) return 1;
                throw new ParseFailureException("expected a quoted string, variable, or placeholder");
            }

            @Override
            public Object parse(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
                if (tokens.size() == 1) {
                    Token t = tokens.get(0);
                    if (t.kind() != TokenKind.STRING) {
                        VarRef ref = env.lookupVar(t.text());
                        if (ref != null)
                            return ref;
                    }
                }
                return new TokenList(List.copyOf(tokens));
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (value instanceof VarRef ref) {
                    return "String.valueOf(" + ref.java() + ")";
                }
                TokenList tl = (TokenList) value;
                String raw = tl.tokens().stream().map(Token::text).collect(Collectors.joining(" "));
                return PlaceholderExpander.expand(raw, env);
            }
        });
    }

    private static @NotNull String escapeJava(@NotNull String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void registerIdent(@NotNull TypeRegistry types) {
        types.register(new TypeBinding() {
            @Override
            public @NotNull String id() {
                return "IDENT";
            }

            @Override
            public int consumeCount(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
                if (tokens.isEmpty()) throw new ParseFailureException("expected an identifier");
                if (tokens.get(0).kind() != TokenKind.IDENT) throw new ParseFailureException("expected an identifier");
                return 1;
            }

            @Override
            public Object parse(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
                return tokens.get(0).text();
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                return (String) value;
            }
        });
    }

    /**
     * A thin wrapper around a list of tokens that provides a human-readable
     * {@link #toString()} returning the joined raw text of its tokens.
     *
     * @param tokens the original tokens
     */
    record TokenList(@NotNull List<Token> tokens) {
        @Override
        public @NotNull String toString() {
            return tokens.stream().map(Token::text).collect(Collectors.joining(" "));
        }
    }
}
