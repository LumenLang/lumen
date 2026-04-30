package dev.lumenlang.lumen.debug.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.lumenlang.lumen.debug.hook.ScriptHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes and deserializes the JSON protocol messages exchanged.
 */
public final class DebugProtocol {

    private static final Gson GSON = new Gson();

    private DebugProtocol() {
    }

    /**
     * Builds a breakpoint hit event message.
     *
     * @param script         the script name
     * @param line           the 1-based script line number
     * @param vars           the variable snapshot
     * @param dumpFields     whether to include field introspection for each variable
     * @param conditionTrace all condition evaluations that ran before this breakpoint, in order
     * @return the JSON string
     */
    public static @NotNull String breakpointHit(@NotNull String script, int line, @NotNull Map<String, Object> vars, boolean dumpFields, @NotNull List<ScriptHooks.ConditionRecord> conditionTrace) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "breakpoint");
        msg.addProperty("script", script);
        msg.addProperty("line", line);
        msg.add("vars", buildVars(vars, dumpFields));
        msg.add("conditionTrace", buildConditionTrace(conditionTrace));
        return GSON.toJson(msg);
    }

    /**
     * Builds an expressions list event message.
     *
     * @param script      the script name
     * @param expressions the expression metadata list
     * @param overrides   currently active compile-time overrides
     * @return the JSON string
     */
    public static @NotNull String expressionsList(@NotNull String script, @NotNull List<ExprInfo> expressions, @NotNull Map<String, String> overrides) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "expressions");
        msg.addProperty("script", script);
        JsonArray list = new JsonArray();
        for (ExprInfo e : expressions) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", e.id);
            entry.addProperty("expression", e.expression);
            entry.addProperty("type", e.type);
            entry.addProperty("line", e.line);
            entry.addProperty("overridden", overrides.containsKey(e.id));
            String ov = overrides.get(e.id);
            if (ov != null) entry.addProperty("overrideValue", ov);
            list.add(entry);
        }
        msg.add("list", list);
        return GSON.toJson(msg);
    }

    /**
     * Builds a response confirming that a script was enabled for instrumentation.
     *
     * @param script          the script name
     * @param expressionCount the number of registered expressions
     * @param breakpointCount the number of active breakpoints for this script
     * @return the JSON string
     */
    public static @NotNull String enabled(@NotNull String script, int expressionCount, int breakpointCount) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "enabled");
        msg.addProperty("script", script);
        msg.addProperty("expressionCount", expressionCount);
        msg.addProperty("breakpointCount", breakpointCount);
        return GSON.toJson(msg);
    }

    /**
     * Builds an error response.
     *
     * @param message the error message
     * @return the JSON string
     */
    public static @NotNull String error(@NotNull String message) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "error");
        msg.addProperty("message", message);
        return GSON.toJson(msg);
    }

    /**
     * Builds an authorized response carrying the issued session token.
     *
     * @param sessionToken   opaque token the client should keep
     * @param clientId       echoed client identifier
     * @param expiresAt      epoch millis when the token expires
     * @param sessionMinutes token lifetime in minutes
     */
    public static @NotNull String authorized(@NotNull String sessionToken, @NotNull String clientId, long expiresAt, int sessionMinutes) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "authorized");
        msg.addProperty("sessionToken", sessionToken);
        msg.addProperty("clientId", clientId);
        msg.addProperty("expiresAt", expiresAt);
        msg.addProperty("sessionMinutes", sessionMinutes);
        return GSON.toJson(msg);
    }

    /**
     * Builds a pairing-required response telling the client to wait for operator approval.
     *
     * @param pairingId  unique id for this pairing attempt
     * @param ttlSeconds seconds until the pairing request expires
     */
    public static @NotNull String pairingRequired(@NotNull String pairingId, long ttlSeconds) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "pairing_required");
        msg.addProperty("pairingId", pairingId);
        msg.addProperty("ttlSeconds", ttlSeconds);
        return GSON.toJson(msg);
    }

    /**
     * Builds an auth-failed response.
     *
     * @param reason short explanation shown to the user
     */
    public static @NotNull String authFailed(@NotNull String reason) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "auth_failed");
        msg.addProperty("reason", reason);
        return GSON.toJson(msg);
    }

    /**
     * Builds a snippet execution result message with generated source and captured output.
     *
     * @param success         whether the snippet compiled and executed successfully
     * @param error           the error message if failed, or null
     * @param generatedSource the generated Java source code, or null
     * @param stdout          captured standard output during execution, or null
     * @param stderr          captured standard error during execution, or null
     * @param conditionTrace  all condition evaluations that ran during snippet execution, in order
     * @return the JSON string
     */
    public static @NotNull String snippetResult(boolean success, @Nullable String error, @Nullable String generatedSource, @Nullable String stdout, @Nullable String stderr, @NotNull List<ScriptHooks.ConditionRecord> conditionTrace) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "snippetResult");
        msg.addProperty("success", success);
        if (error != null) msg.addProperty("error", error);
        if (generatedSource != null) msg.addProperty("generatedSource", generatedSource);
        if (stdout != null && !stdout.isEmpty()) msg.addProperty("stdout", stdout);
        if (stderr != null && !stderr.isEmpty()) msg.addProperty("stderr", stderr);
        msg.add("conditionTrace", buildConditionTrace(conditionTrace));
        return GSON.toJson(msg);
    }

    /**
     * Builds a conditions list event message.
     *
     * @param script     the script name
     * @param conditions the condition metadata list
     * @param overrides  currently active condition overrides
     * @return the JSON string
     */
    public static @NotNull String conditionsList(@NotNull String script, @NotNull List<CondInfo> conditions, @NotNull Map<String, String> overrides) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "conditions");
        msg.addProperty("script", script);
        JsonArray list = new JsonArray();
        for (CondInfo c : conditions) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", c.id);
            entry.addProperty("source", c.source);
            entry.addProperty("line", c.line);
            entry.addProperty("overridden", overrides.containsKey(c.id));
            String ov = overrides.get(c.id);
            if (ov != null) entry.addProperty("overrideMode", ov);
            list.add(entry);
        }
        msg.add("list", list);
        return GSON.toJson(msg);
    }

    /**
     * Builds a poll response containing all buffered events.
     *
     * @param events the buffered event JSON strings
     * @return the JSON string
     */
    public static @NotNull String pollResult(@NotNull List<String> events) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "poll");
        JsonArray arr = new JsonArray();
        for (String e : events) arr.add(JsonParser.parseString(e));
        msg.add("events", arr);
        return GSON.toJson(msg);
    }

    /**
     * Parses an incoming JSON message into a map. Preserves Java types: strings stay
     * strings, JSON integers become {@link Integer}, decimals become {@link Double},
     * booleans become {@link Boolean}, and arrays become {@link List}.
     *
     * @param json the JSON string
     * @return parsed key-value pairs, empty map if parsing fails
     */
    public static @NotNull Map<String, Object> parseMessage(@NotNull String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                result.put(entry.getKey(), convertElement(entry.getValue()));
            }
            return result;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static @Nullable Object convertElement(@NotNull JsonElement el) {
        if (el.isJsonNull()) return null;
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isString()) return p.getAsString();
            if (p.isNumber()) {
                double d = p.getAsDouble();
                if (d == Math.floor(d) && !Double.isInfinite(d) && d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE)
                    return (int) d;
                return d;
            }
        }
        if (el.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement item : el.getAsJsonArray()) list.add(convertElement(item));
            return list;
        }
        if (el.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet())
                map.put(e.getKey(), convertElement(e.getValue()));
            return map;
        }
        return null;
    }

    private static @NotNull JsonArray buildVars(@NotNull Map<String, Object> vars, boolean dumpFields) {
        JsonArray arr = new JsonArray();
        for (var entry : vars.entrySet()) {
            JsonObject varObj = new JsonObject();
            varObj.addProperty("name", entry.getKey());
            Object val = entry.getValue();
            varObj.addProperty("value", val != null ? String.valueOf(val) : "null");
            varObj.addProperty("type", val != null ? val.getClass().getSimpleName() : "null");
            if (dumpFields && val != null && !isPrimitive(val)) varObj.add("fields", buildFieldDump(val));
            arr.add(varObj);
        }
        return arr;
    }

    private static boolean isPrimitive(@NotNull Object val) {
        return val instanceof String || val instanceof Number || val instanceof Boolean || val instanceof Character;
    }

    private static @NotNull JsonArray buildFieldDump(@NotNull Object val) {
        JsonArray arr = new JsonArray();
        for (Class<?> cls = val.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            for (Field field : cls.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object fieldVal = field.get(val);
                    JsonObject fieldObj = new JsonObject();
                    fieldObj.addProperty("name", field.getName());
                    fieldObj.addProperty("value", fieldVal != null ? String.valueOf(fieldVal) : "null");
                    fieldObj.addProperty("type", fieldVal != null ? fieldVal.getClass().getSimpleName() : "null");
                    arr.add(fieldObj);
                } catch (Exception ignored) {
                }
            }
        }
        return arr;
    }

    private static @NotNull JsonArray buildObjectInfoArray(@NotNull List<ObjectInfo> list) {
        JsonArray arr = new JsonArray();
        for (ObjectInfo o : list) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", o.name);
            obj.addProperty("uuid", o.uuid);
            obj.addProperty("type", o.type);
            obj.addProperty("world", o.world);
            obj.addProperty("x", o.x);
            obj.addProperty("y", o.y);
            obj.addProperty("z", o.z);
            arr.add(obj);
        }
        return arr;
    }

    private static @NotNull JsonArray buildConditionTrace(@NotNull List<ScriptHooks.ConditionRecord> trace) {
        JsonArray arr = new JsonArray();
        for (ScriptHooks.ConditionRecord r : trace) {
            JsonObject obj = new JsonObject();
            obj.addProperty("condId", r.condId());
            obj.addProperty("source", r.source());
            obj.addProperty("line", r.line());
            obj.addProperty("result", r.result());
            arr.add(obj);
        }
        return arr;
    }

    /**
     * Info about a live object (player or entity).
     *
     * @param name  the display name
     * @param uuid  the UUID string
     * @param type  the type name (e.g. "Player", "Zombie")
     * @param world the world name
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param z     the z coordinate
     */
    public record ObjectInfo(@NotNull String name, @NotNull String uuid, @NotNull String type, @NotNull String world,
                             double x, double y, double z) {
    }

    /**
     * Info about a registered expression.
     *
     * @param id         the stable expression identifier
     * @param expression the original script source text for this assignment
     * @param type       the Java type name
     * @param line       the 1-based script line number
     */
    public record ExprInfo(@NotNull String id, @NotNull String expression, @NotNull String type, int line) {
    }

    /**
     * Info about a discovered condition.
     *
     * @param id     the stable condition identifier (script:line:cond or script:line:else)
     * @param source the original Lumen source text
     * @param line   the 1-based script line number
     */
    public record CondInfo(@NotNull String id, @NotNull String source, int line) {
    }
}
