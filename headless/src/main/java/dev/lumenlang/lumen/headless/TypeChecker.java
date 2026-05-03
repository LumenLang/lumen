package dev.lumenlang.lumen.headless;

import com.google.gson.JsonObject;
import dev.lumenlang.lumen.api.exceptions.ParseFailureException;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.codegen.BlockContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.TypeBinding;
import dev.lumenlang.lumen.pipeline.language.tokenization.Line;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.Tokenizer;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulates type binding parsing against arbitrary input strings.
 * Populates a TypeEnvImpl with declared variables so that type bindings
 * that depend on variable lookups (like PLAYER) work correctly.
 */
public final class TypeChecker {

    private TypeChecker() {
    }

    /**
     * Checks whether the given input text matches the specified type binding.
     * Variables can be declared via the vars map so that type bindings
     * requiring variable lookups (e.g. PLAYER, ENTITY) resolve correctly.
     *
     * @param typeId   the type binding identifier (e.g. "PLAYER", "INT", "MATERIAL")
     * @param input    the text to test against the type binding
     * @param vars     declared variables as name to ref type id (e.g. {"p": "PLAYER"})
     * @param registry the type registry containing all registered bindings
     * @return a JSON object with the match result
     */
    public static @NotNull JsonObject check(@NotNull String typeId, @NotNull String input, @Nullable Map<String, String> vars, @NotNull TypeRegistry registry) {
        JsonObject result = new JsonObject();
        result.addProperty("type", typeId);
        result.addProperty("input", input);

        TypeBinding binding = registry.get(typeId);
        if (binding == null) {
            result.addProperty("ok", false);
            result.addProperty("error", "Unknown type binding: " + typeId);
            return result;
        }

        List<Token> tokens;
        try {
            List<Line> lines = new Tokenizer().tokenize(input);
            tokens = lines.isEmpty() ? List.of() : lines.get(0).tokens();
        } catch (Exception e) {
            result.addProperty("ok", false);
            result.addProperty("error", "Tokenization failed: " + e.getMessage());
            return result;
        }

        var varErrors = validateVars(vars);
        if (!varErrors.isEmpty()) {
            JsonObject errors = new JsonObject();
            for (Map.Entry<String, String> entry : varErrors.entrySet()) {
                errors.addProperty(entry.getKey(), entry.getValue());
            }
            result.add("varErrors", errors);
        }

        TypeEnvImpl env = populatedEnv(vars);

        int consumeCount;
        try {
            consumeCount = binding.consumeCount(tokens, env);
        } catch (ParseFailureException e) {
            result.addProperty("ok", true);
            result.addProperty("matches", false);
            result.addProperty("reason", "consumeCount rejected: " + e.getMessage());
            return result;
        } catch (Exception e) {
            result.addProperty("ok", false);
            result.addProperty("error", "consumeCount threw: " + e.getMessage());
            return result;
        }

        result.addProperty("consumed", consumeCount);

        List<Token> toPass;
        if (consumeCount == -1) {
            toPass = tokens;
        } else if (consumeCount > tokens.size()) {
            result.addProperty("ok", true);
            result.addProperty("matches", false);
            result.addProperty("reason", "Needs " + consumeCount + " tokens but input has " + tokens.size());
            return result;
        } else {
            toPass = tokens.subList(0, consumeCount);
        }

        try {
            Object parsed = binding.parse(toPass, env);
            result.addProperty("ok", true);
            result.addProperty("matches", true);
            if (parsed != null) result.addProperty("parsed", parsed.getClass().getSimpleName());
            if (consumeCount != -1 && consumeCount < tokens.size())
                result.addProperty("unconsumedTokens", tokens.size() - consumeCount);
        } catch (ParseFailureException e) {
            result.addProperty("ok", true);
            result.addProperty("matches", false);
            result.addProperty("reason", "parse rejected: " + e.getMessage());
        } catch (Exception e) {
            result.addProperty("ok", true);
            result.addProperty("matches", false);
            result.addProperty("reason", "parse failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Creates a TypeEnvImpl pre-populated with the declared variables.
     * Each variable is registered with its RefType so type bindings
     * that call lookupVar can resolve them.
     */
    static @NotNull TypeEnvImpl populatedEnv(@Nullable Map<String, String> vars) {
        TypeEnvImpl env = new TypeEnvImpl();
        env.enterBlock(new BlockContextImpl(null, null, List.of(), 0));
        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                LumenType lumenType = LumenType.fromName(entry.getValue());
                if (lumenType != null) {
                    env.defineVar(entry.getKey(), new VarRef(entry.getKey(), lumenType, entry.getKey()));
                }
            }
        }
        return env;
    }

    static @NotNull Map<String, String> validateVars(@Nullable Map<String, String> vars) {
        Map<String, String> errors = new HashMap<>();
        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                if (LumenType.fromName(entry.getValue()) == null) {
                    errors.put(entry.getKey(), "Unknown type: " + entry.getValue());
                }
            }
        }
        return errors;
    }
}
