package dev.lumenlang.lumen.headless;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lumenlang.lumen.pipeline.events.EventDefRegistry;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOption;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOptions;
import dev.lumenlang.lumen.pipeline.language.tokenization.Tokenizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JSON line protocol server that reads requests from stdin and writes
 * responses to stdout.
 */
public final class JsonProtocol {

    private static final Gson GSON = new Gson();

    private JsonProtocol() {
    }

    /**
     * Starts the JSON line protocol server, blocking on stdin until exit.
     *
     * @param headless the bootstrapped headless instance
     */
    public static void run(@NotNull HeadlessLumen headless) {
        respond(readyMessage(headless));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                handle(headless, line.trim());
            }
        } catch (IOException e) {
            System.err.println("IO error: " + e.getMessage());
        }
    }

    /**
     * Dispatches a single JSON request to the appropriate handler.
     */
    private static void handle(@NotNull HeadlessLumen headless, @NotNull String line) {
        JsonObject request;
        try {
            request = JsonParser.parseString(line).getAsJsonObject();
        } catch (Exception e) {
            respond(error("Invalid JSON: " + e.getMessage()));
            return;
        }

        if (!request.has("op") || request.get("op").isJsonNull()) {
            respond(error("missing op field"));
            return;
        }

        switch (request.get("op").getAsString()) {
            case "validate" -> validate(headless, request);
            case "info" -> respond(infoMessage(headless));
            case "search" -> findPatterns(headless, request);
            case "check" -> typeCheck(headless, request);
            case "exit" -> System.exit(0);
            default -> respond(error("unknown op: " + request.get("op").getAsString()));
        }
    }

    private static void validate(@NotNull HeadlessLumen headless, @NotNull JsonObject request) {
        String source = stringField(request, "source");
        if (source == null) {
            respond(error("missing source field"));
            return;
        }

        try {
            headless.parse(source, scriptName(request));
            JsonObject response = new JsonObject();
            response.addProperty("ok", true);
            respond(response);
        } catch (LumenScriptException e) {
            respond(scriptError(e));
        } catch (Exception e) {
            respond(genericError(e));
        }
    }

    private static void findPatterns(@NotNull HeadlessLumen headless, @NotNull JsonObject request) {
        String query = stringField(request, "query");
        if (query == null) {
            respond(error("missing query field"));
            return;
        }

        int limit = request.has("limit") ? request.get("limit").getAsInt() : 10;

        SimulatorOptions opts = SimulatorOptions.builder()
                .set(SimulatorOption.MAX_SUGGESTIONS, limit)
                .build();

        try {
            var vars = varsMap(request);
            var varErrors = TypeChecker.validateVars(vars);
            var tokens = new Tokenizer().tokenize(query).get(0).tokens();
            var env = TypeChecker.populatedEnv(vars);

            var patternRegistry = headless.patternRegistry();
            var statementPatterns = patternRegistry.getStatements().stream().map(s -> s.pattern().raw()).collect(Collectors.toSet());
            var expressionPatterns = patternRegistry.getExpressions().stream().map(e -> e.pattern().raw()).collect(Collectors.toSet());

            JsonArray statementsResults = new JsonArray();
            JsonArray expressionsResults = new JsonArray();
            JsonArray statementsAndExpressionsResults = new JsonArray();
            JsonArray conditionsResults = new JsonArray();
            JsonArray blocksResults = new JsonArray();

            for (var suggestion : PatternSimulator.suggestStatementsAndExpressions(tokens, patternRegistry, env, opts)) {
                String raw = suggestion.pattern().raw();
                JsonObject obj = new JsonObject();
                obj.addProperty("pattern", raw);
                obj.addProperty("score", suggestion.confidence());
                boolean isStatement = statementPatterns.contains(raw);
                boolean isExpression = expressionPatterns.contains(raw);
                if (isStatement && isExpression) statementsAndExpressionsResults.add(obj);
                else if (isStatement) statementsResults.add(obj);
                else if (isExpression) expressionsResults.add(obj);
            }
            for (var suggestion : PatternSimulator.suggestConditions(tokens, patternRegistry, env, opts)) {
                JsonObject obj = new JsonObject();
                obj.addProperty("pattern", suggestion.pattern().raw());
                obj.addProperty("score", suggestion.confidence());
                conditionsResults.add(obj);
            }
            for (var suggestion : PatternSimulator.suggestBlocks(tokens, patternRegistry, env, opts)) {
                JsonObject obj = new JsonObject();
                obj.addProperty("pattern", suggestion.pattern().raw());
                obj.addProperty("score", suggestion.confidence());
                blocksResults.add(obj);
            }

            JsonObject response = new JsonObject();
            response.addProperty("ok", true);
            response.add("statements", statementsResults);
            response.add("expressions", expressionsResults);
            response.add("statementsAndExpressions", statementsAndExpressionsResults);
            response.add("conditions", conditionsResults);
            response.add("blocks", blocksResults);
            if (!varErrors.isEmpty()) {
                JsonObject errors = new JsonObject();
                for (Map.Entry<String, String> entry : varErrors.entrySet()) {
                    errors.addProperty(entry.getKey(), entry.getValue());
                }
                response.add("varErrors", errors);
            }
            respond(response);
        } catch (Exception e) {
            respond(error("search error: " + e.getMessage()));
        }
    }

    /**
     * Handles the check op: tests whether input text matches a type binding
     * with an optional vars map for simulating a variable environment.
     */
    private static void typeCheck(@NotNull HeadlessLumen headless, @NotNull JsonObject request) {
        String type = stringField(request, "type");
        String input = stringField(request, "input");
        if (type == null) {
            respond(error("missing type field"));
            return;
        }
        if (input == null) {
            respond(error("missing input field"));
            return;
        }

        respond(TypeChecker.check(type, input, varsMap(request), headless.typeRegistry()));
    }

    /**
     * Converts a LumenScriptException into a error response.
     */
    private static @NotNull JsonObject scriptError(@NotNull LumenScriptException e) {
        JsonObject response = new JsonObject();
        response.addProperty("ok", false);
        response.addProperty("errors", e.getMessage());
        return response;
    }

    /**
     * Converts a generic exception into a structured error response.
     */
    private static @NotNull JsonObject genericError(@NotNull Exception e) {
        JsonObject response = new JsonObject();
        response.addProperty("ok", false);
        response.addProperty("message", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        return response;
    }

    /**
     * Builds the ready message sent on server startup.
     */
    private static @NotNull JsonObject readyMessage(@NotNull HeadlessLumen headless) {
        JsonObject msg = new JsonObject();
        msg.addProperty("status", "ready");
        msg.addProperty("patterns", headless.patternCount());
        msg.addProperty("types", headless.typeRegistry().allBindings().size());
        msg.addProperty("events", EventDefRegistry.defs().size());
        return msg;
    }

    /**
     * Builds the info response.
     */
    private static @NotNull JsonObject infoMessage(@NotNull HeadlessLumen headless) {
        JsonObject msg = new JsonObject();
        msg.addProperty("ok", true);
        msg.addProperty("patterns", headless.patternCount());
        msg.addProperty("types", headless.typeRegistry().allBindings().size());
        msg.addProperty("events", EventDefRegistry.defs().size());
        return msg;
    }

    /**
     * Creates a simple error JSON response.
     */
    private static @NotNull JsonObject error(@NotNull String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("ok", false);
        obj.addProperty("error", message);
        return obj;
    }

    /**
     * Extracts a string field from a JSON object, returning null if absent.
     */
    private static String stringField(@NotNull JsonObject obj, @NotNull String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    /**
     * Extracts the script name from a request, defaulting to "script.luma".
     */
    private static @NotNull String scriptName(@NotNull JsonObject request) {
        String name = stringField(request, "name");
        return name != null ? name : "script.luma";
    }

    /**
     * Extracts the optional "vars" object from a request as a name to ref type id map.
     * Returns null if the field is absent.
     */
    private static @Nullable Map<String, String> varsMap(@NotNull JsonObject request) {
        if (!request.has("vars") || request.get("vars").isJsonNull()) return null;
        Map<String, String> vars = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : request.getAsJsonObject("vars").entrySet()) {
            vars.put(entry.getKey(), entry.getValue().getAsString());
        }
        return vars;
    }

    /**
     * Writes a JSON object as a single line to stdout.
     */
    private static synchronized void respond(@NotNull JsonObject json) {
        System.out.println(GSON.toJson(json));
        System.out.flush();
    }
}
