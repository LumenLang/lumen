package dev.lumenlang.lumen.plugin.documentation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumenlang.lumen.api.event.AdvancedEventDefinition;
import dev.lumenlang.lumen.api.event.EventDefinition;
import dev.lumenlang.lumen.api.pattern.BlockVarInfo;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.api.type.TypeBindingMeta;
import dev.lumenlang.lumen.pipeline.conditions.registry.RegisteredCondition;
import dev.lumenlang.lumen.pipeline.events.EventDefRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.RegisteredBlock;
import dev.lumenlang.lumen.pipeline.language.pattern.RegisteredExpression;
import dev.lumenlang.lumen.pipeline.language.pattern.RegisteredPattern;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.pipeline.loop.RegisteredLoop;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import dev.lumenlang.lumen.plugin.Lumen;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generates a comprehensive JSON documentation file describing every registered
 * pattern, expression, condition, block, loop source, event, and type binding in a Lumen instance.
 *
 * <p>This tool is enabled via the {@code extra.enable-documentation-tool}
 * configuration option. When active, it writes {@code documentation.json} and
 * {@code documentation-raw.json} into the documentation folder of the plugin's
 * data directory.
 *
 * <h2>JSON Schema</h2>
 *
 * <p>The root object has the following top-level keys:
 *
 * <pre>{@code
 * {
 *   "version": "1.0.0",
 *   "generatedAt": "2025-06-15T12:00:00Z",
 *   "statements": [ ... ],
 *   "expressions": [ ... ],
 *   "conditions": [ ... ],
 *   "blocks": [ ... ],
 *   "loopSources": [ ... ],
 *   "events": [ ... ],
 *   "typeBindings": [ ... ]
 * }
 * }</pre>
 *
 * <h3>Pattern entry (statements, expressions, conditions)</h3>
 *
 * <p>Each pattern entry represents a single logical registration. When a
 * registration uses multiple patterns (via {@code .patterns()} or repeated
 * {@code .pattern()} calls), they are grouped into a single entry with a
 * {@code "patterns"} array.
 *
 * <pre>{@code
 * {
 *   "patterns": ["give %who:PLAYER% %item:MATERIAL% %amt:INT%"],
 *   "by": "Lumen",
 *   "description": "Gives items to a player.",
 *   "examples": ["give player diamond 1"],
 *   "since": "1.0.0",
 *   "category": "Player",
 *   "deprecated": false
 * }
 * }</pre>
 *
 * <table>
 * <caption>Pattern entry fields</caption>
 * <tr><th>Field</th><th>Type</th><th>Description</th></tr>
 * <tr><td>{@code patterns}</td><td>{@code string[]}</td><td>One or more raw
 * pattern strings. Never empty.</td></tr>
 * <tr><td>{@code by}</td><td>{@code string | null}</td><td>The addon name that
 * registered this pattern (e.g. "Lumen").</td></tr>
 * <tr><td>{@code description}</td><td>{@code string |
 * null}</td><td>Human-readable description.</td></tr>
 * <tr><td>{@code examples}</td><td>{@code string[]}</td><td>Lumen script
 * examples. May be empty.</td></tr>
 * <tr><td>{@code since}</td><td>{@code string | null}</td><td>Version
 * introduced.</td></tr>
 * <tr><td>{@code category}</td><td>{@code string | null}</td><td>Documentation
 * category name.</td></tr>
 * <tr><td>{@code deprecated}</td><td>{@code boolean}</td><td>{@code true} if
 * this pattern is deprecated.</td></tr>
 * </table>
 *
 * <h3>Block entry</h3>
 *
 * <p>Block entries share the same base fields as other pattern entries, but add
 * {@code "supportsRootLevel"} and {@code "supportsBlock"} flags, and may include
 * a {@code "variables"} array describing variables the block provides to its
 * child statements.
 *
 * <pre>{@code
 * {
 *   "patterns": ["command %name:EXPR%"],
 *   "by": "Lumen",
 *   "description": "Declares a custom command.",
 *   "examples": ["command hello:"],
 *   "since": "1.0.0",
 *   "category": "Command",
 *   "deprecated": false,
 *   "supportsRootLevel": true,
 *   "supportsBlock": false,
 *   "variables": [
 *     { "name": "player", "type": "Player", "refType": "PLAYER", "nullable": true, "description": "The player who executed the command, or null if the console ran it" },
 *     { "name": "sender", "type": "CommandSender", "refType": "SENDER", "nullable": false, "description": "The command sender (player or console)" }
 *   ]
 * }
 * }</pre>
 *
 * <table>
 * <caption>Block entry fields</caption>
 * <tr><th>Field</th><th>Type</th><th>Description</th></tr>
 * <tr><td>{@code supportsRootLevel}</td><td>{@code boolean}</td><td>{@code true} if
 * this block can appear at the top level of a script (not nested).</td></tr>
 * <tr><td>{@code supportsBlock}</td><td>{@code boolean}</td><td>{@code true} if
 * this block can be nested inside other blocks.</td></tr>
 * <tr><td>{@code variables}</td><td>{@code object[] | absent}</td><td>Variables
 * the block exposes to child statements. Only present when the block declares
 * variables.</td></tr>
 * </table>
 *
 * <table>
 * <caption>Block variable fields</caption>
 * <tr><th>Field</th><th>Type</th><th>Description</th></tr>
 * <tr><td>{@code name}</td><td>{@code string}</td><td>The variable name
 * accessible in script child statements.</td></tr>
 * <tr><td>{@code type}</td><td>{@code string}</td><td>Human readable type
 * string (e.g. "Player", "World").</td></tr>
 * <tr><td>{@code refType}</td><td>{@code string | null}</td><td>The ref type
 * identifier (e.g. "PLAYER", "WORLD"), or {@code null} if the variable has no
 * ref type.</td></tr>
 * <tr><td>{@code nullable}</td><td>{@code boolean}</td><td>{@code true} if
 * the variable can be {@code null}.</td></tr>
 * <tr><td>{@code description}</td><td>{@code string | null}</td><td>Human readable
 * description of what the variable represents.</td></tr>
 * <tr><td>{@code metadata}</td><td>{@code object | absent}</td><td>Optional
 * metadata map. Only present when the variable has metadata entries.</td></tr>
 * </table>
 *
 * <h3>Event entry</h3>
 *
 * <pre>{@code
 * {
 *   "name": "join",
 *   "by": "Lumen",
 *   "className": "org.bukkit.event.player.PlayerJoinEvent",
 *   "description": "Fires when a player joins the server.",
 *   "examples": ["on join:"],
 *   "since": "1.0.0",
 *   "category": "Player",
 *   "deprecated": false,
 *   "variables": [
 *     { "name": "player", "javaType": "org.bukkit.entity.Player", "refTypeId": "PLAYER", "description": "The player who joined" }
 *   ]
 * }
 * }</pre>
 *
 * <table>
 * <caption>Event entry fields</caption>
 * <tr><th>Field</th><th>Type</th><th>Description</th></tr>
 * <tr><td>{@code name}</td><td>{@code string}</td><td>The script-level event
 * name used in {@code on <name>:} blocks.</td></tr>
 * <tr><td>{@code by}</td><td>{@code string | null}</td><td>The addon name that
 * registered this event.</td></tr>
 * <tr><td>{@code className}</td><td>{@code string}</td><td>Fully qualified
 * Bukkit event class.</td></tr>
 * <tr><td>{@code description}</td><td>{@code string | null}</td><td>Human-readable
 * description.</td></tr>
 * <tr><td>{@code examples}</td><td>{@code string[]}</td><td>Usage examples.</td></tr>
 * <tr><td>{@code since}</td><td>{@code string | null}</td><td>Version
 * introduced.</td></tr>
 * <tr><td>{@code category}</td><td>{@code string | null}</td><td>Documentation
 * category.</td></tr>
 * <tr><td>{@code deprecated}</td><td>{@code boolean}</td><td>{@code true} if
 * this event is deprecated.</td></tr>
 * <tr><td>{@code variables}</td><td>{@code object[]}</td><td>Variables exposed to
 * child statements, each with {@code name}, {@code javaType}, optional
 * {@code refTypeId}, optional {@code description}, {code nullable}, and {code metadata}</td></tr>
 * </table>
 *
 * <h3>Type binding entry</h3>
 *
 * <pre>{@code
 * {
 *   "id": "PLAYER",
 *   "description": "Resolves a player reference from a variable name.",
 *   "javaType": "org.bukkit.entity.Player",
 *   "examples": ["message %who:PLAYER% \"Hello!\""],
 *   "since": "1.0.0",
 *   "deprecated": false
 * }
 * }</pre>
 *
 * <table>
 * <caption>Type binding entry fields</caption>
 * <tr><th>Field</th><th>Type</th><th>Description</th></tr>
 * <tr><td>{@code id}</td><td>{@code string}</td><td>The type identifier used in
 * patterns (e.g. {@code "PLAYER"}).</td></tr>
 * <tr><td>{@code description}</td><td>{@code string | null}</td><td>What this
 * type binding represents.</td></tr>
 * <tr><td>{@code javaType}</td><td>{@code string | null}</td><td>Fully
 * qualified Java type this resolves to.</td></tr>
 * <tr><td>{@code examples}</td><td>{@code string[]}</td><td>Usage examples. May
 * be empty.</td></tr>
 * <tr><td>{@code since}</td><td>{@code string | null}</td><td>Version
 * introduced.</td></tr>
 * <tr><td>{@code deprecated}</td><td>{@code boolean}</td><td>{@code true} if
 * this type binding is deprecated.</td></tr>
 * </table>
 *
 * @see PatternRegistry
 * @see TypeRegistry
 * @see EventDefRegistry
 */
public final class DocumentationDumper {

    private DocumentationDumper() {
    }

    /**
     * Collects all registered patterns and type bindings from the given registries
     * and writes them as a single JSON file to the specified path.
     *
     * <p>
     * Patterns that share the same handler (registered via multi-pattern builders)
     * are grouped into a single documentation entry with multiple pattern strings.
     *
     * @param patternRegistry the pattern registry containing all statements,
     *                        blocks, expressions, and conditions
     */
    public static void dump(@NotNull PatternRegistry patternRegistry) {
        Path outputPath = Lumen.instance().getDataFolder().toPath().resolve("documentation");
        try {
            Files.createDirectories(outputPath);
        } catch (IOException e) {
            LumenLogger.warning("Failed to create directories for documentation output: " + e.getMessage());
            return;
        }
        TypeRegistry typeRegistry = patternRegistry.getTypeRegistry();

        CompletableFuture.runAsync(() -> {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("version", Lumen.instance().getDescription().getVersion());
            root.put("generatedAt", Instant.now().toString());
            root.put("statements", groupStatements(patternRegistry.getStatements()));
            root.put("expressions", groupExpressions(patternRegistry.getExpressions()));
            root.put("conditions", groupConditions(patternRegistry.getConditionRegistry().getConditions()));
            root.put("blocks", groupBlocks(patternRegistry.getBlocks()));
            root.put("loopSources", groupLoops(patternRegistry.getLoopRegistry().getLoops()));
            root.put("events", collectEvents());
            root.put("typeBindings", collectTypeBindings(typeRegistry));

            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .serializeNulls()
                    .disableHtmlEscaping()
                    .create();
            Gson raw = new GsonBuilder()
                    .serializeNulls()
                    .disableHtmlEscaping()
                    .create();

            try {
                try (Writer writer = Files.newBufferedWriter(outputPath.resolve("documentation.json"),
                        StandardCharsets.UTF_8)) {
                    gson.toJson(root, writer);
                    LumenLogger.debug("DocumentationDumper", "Formatted Documentation JSON written to " + outputPath.resolve("documentation.json"));
                }

                try (Writer writer = Files.newBufferedWriter(outputPath.resolve("documentation-raw.json"),
                        StandardCharsets.UTF_8)) {
                    raw.toJson(root, writer);
                    LumenLogger.debug("DocumentationDumper", "Raw Documentation JSON written to " + outputPath.resolve("documentation-raw.json"));
                }
            } catch (IOException e) {
                LumenLogger.warning("Failed to write documentation JSON: " + e.getMessage());
            }
        });
    }

    private static @NotNull List<Map<String, Object>> groupStatements(
            @NotNull List<RegisteredPattern> statements) {
        IdentityHashMap<Object, List<String>> handlerToPatterns = new IdentityHashMap<>();
        IdentityHashMap<Object, PatternMeta> handlerToMeta = new IdentityHashMap<>();
        List<Object> handlerOrder = new ArrayList<>();

        for (RegisteredPattern rp : statements) {
            Object handler = rp.handler();
            handlerToPatterns.computeIfAbsent(handler, k -> {
                handlerOrder.add(k);
                return new ArrayList<>();
            }).add(rp.pattern().raw());
            handlerToMeta.putIfAbsent(handler, rp.meta());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object handler : handlerOrder) {
            result.add(buildPatternEntry(handlerToPatterns.get(handler), handlerToMeta.get(handler)));
        }
        return result;
    }

    private static @NotNull List<Map<String, Object>> groupExpressions(
            @NotNull List<RegisteredExpression> expressions) {
        IdentityHashMap<Object, List<String>> handlerToPatterns = new IdentityHashMap<>();
        IdentityHashMap<Object, PatternMeta> handlerToMeta = new IdentityHashMap<>();
        List<Object> handlerOrder = new ArrayList<>();

        for (RegisteredExpression re : expressions) {
            Object handler = re.handler();
            handlerToPatterns.computeIfAbsent(handler, k -> {
                handlerOrder.add(k);
                return new ArrayList<>();
            }).add(re.pattern().raw());
            handlerToMeta.putIfAbsent(handler, re.meta());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object handler : handlerOrder) {
            result.add(buildPatternEntry(handlerToPatterns.get(handler), handlerToMeta.get(handler)));
        }
        return result;
    }

    private static @NotNull List<Map<String, Object>> groupConditions(
            @NotNull List<RegisteredCondition> conditions) {
        IdentityHashMap<Object, List<String>> handlerToPatterns = new IdentityHashMap<>();
        IdentityHashMap<Object, PatternMeta> handlerToMeta = new IdentityHashMap<>();
        List<Object> handlerOrder = new ArrayList<>();

        for (RegisteredCondition rc : conditions) {
            Object handler = rc.handler();
            handlerToPatterns.computeIfAbsent(handler, k -> {
                handlerOrder.add(k);
                return new ArrayList<>();
            }).add(rc.pattern().raw());
            handlerToMeta.putIfAbsent(handler, rc.meta());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object handler : handlerOrder) {
            result.add(buildPatternEntry(handlerToPatterns.get(handler), handlerToMeta.get(handler)));
        }
        return result;
    }

    private static @NotNull List<Map<String, Object>> groupBlocks(
            @NotNull List<RegisteredBlock> blocks) {
        IdentityHashMap<Object, List<String>> handlerToPatterns = new IdentityHashMap<>();
        IdentityHashMap<Object, PatternMeta> handlerToMeta = new IdentityHashMap<>();
        IdentityHashMap<Object, List<BlockVarInfo>> handlerToVars = new IdentityHashMap<>();
        IdentityHashMap<Object, Boolean> handlerToRootLevel = new IdentityHashMap<>();
        IdentityHashMap<Object, Boolean> handlerToSupportsBlock = new IdentityHashMap<>();
        List<Object> handlerOrder = new ArrayList<>();

        for (RegisteredBlock rb : blocks) {
            Object handler = rb.handler();
            handlerToPatterns.computeIfAbsent(handler, k -> {
                handlerOrder.add(k);
                return new ArrayList<>();
            }).add(rb.pattern().raw());
            handlerToMeta.putIfAbsent(handler, rb.meta());
            handlerToVars.putIfAbsent(handler, rb.variables());
            handlerToRootLevel.putIfAbsent(handler, rb.supportsRootLevel());
            handlerToSupportsBlock.putIfAbsent(handler, rb.supportsBlock());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object handler : handlerOrder) {
            Map<String, Object> entry = buildPatternEntry(
                    handlerToPatterns.get(handler), handlerToMeta.get(handler));
            entry.put("supportsRootLevel", handlerToRootLevel.get(handler));
            entry.put("supportsBlock", handlerToSupportsBlock.get(handler));
            List<BlockVarInfo> vars = handlerToVars.get(handler);
            if (vars != null && !vars.isEmpty()) {
                List<Map<String, Object>> varList = new ArrayList<>();
                for (BlockVarInfo v : vars) {
                    Map<String, Object> varObj = new LinkedHashMap<>();
                    varObj.put("name", v.name());
                    varObj.put("type", v.type());
                    varObj.put("refType", v.refType() != null ? v.refType().id() : null);
                    varObj.put("nullable", v.metadata().getOrDefault("nullable", false));
                    varObj.put("description", v.description());
                    if (!v.metadata().isEmpty()) {
                        varObj.put("metadata", v.metadata());
                    }
                    varList.add(varObj);
                }
                entry.put("variables", varList);
            }
            result.add(entry);
        }
        return result;
    }

    private static @NotNull List<Map<String, Object>> groupLoops(
            @NotNull List<RegisteredLoop> loops) {
        IdentityHashMap<Object, List<String>> handlerToPatterns = new IdentityHashMap<>();
        IdentityHashMap<Object, PatternMeta> handlerToMeta = new IdentityHashMap<>();
        List<Object> handlerOrder = new ArrayList<>();

        for (RegisteredLoop rl : loops) {
            Object handler = rl.handler();
            handlerToPatterns.computeIfAbsent(handler, k -> {
                handlerOrder.add(k);
                return new ArrayList<>();
            }).add(rl.pattern().raw());
            handlerToMeta.putIfAbsent(handler, rl.meta());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object handler : handlerOrder) {
            result.add(buildPatternEntry(handlerToPatterns.get(handler), handlerToMeta.get(handler)));
        }
        return result;
    }

    private static @NotNull Map<String, Object> buildPatternEntry(
            @NotNull List<String> patterns,
            @NotNull PatternMeta meta) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("patterns", patterns);
        entry.put("by", meta.by());
        entry.put("description", meta.description());
        entry.put("examples", meta.examples());
        entry.put("since", meta.since());
        entry.put("category", meta.category() != null ? meta.category().name() : null);
        entry.put("deprecated", meta.deprecated());
        return entry;
    }

    private static @NotNull List<Map<String, Object>> collectEvents() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (EventDefinition def : EventDefRegistry.apiDefinitions().values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", def.name());
            entry.put("by", def.by());
            entry.put("className", def.className());
            entry.put("description", def.description());
            entry.put("examples", def.examples());
            entry.put("since", def.since());
            entry.put("category", def.category());
            entry.put("deprecated", def.deprecated());
            entry.put("advanced", false);

            List<Map<String, Object>> varList = new ArrayList<>();
            for (var varEntry : def.vars().entrySet()) {
                Map<String, Object> varObj = new LinkedHashMap<>();
                EventDefinition.VarEntry var = varEntry.getValue();
                varObj.put("name", varEntry.getKey());
                varObj.put("javaType", var.javaType());
                varObj.put("refTypeId", var.refTypeId());
                varObj.put("description", var.description());
                varObj.put("nullable", var.metadata().getOrDefault("nullable", false));
                if (!var.metadata().isEmpty()) {
                    varObj.put("metadata", var.metadata());
                }
                varList.add(varObj);
            }
            entry.put("variables", varList);
            result.add(entry);
        }

        for (AdvancedEventDefinition def : EventDefRegistry.advancedDefs().values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", def.name());
            entry.put("by", def.by());
            entry.put("className", null);
            entry.put("description", def.description());
            entry.put("examples", def.examples());
            entry.put("since", def.since());
            entry.put("category", def.category());
            entry.put("deprecated", def.deprecated());
            entry.put("advanced", true);
            entry.put("interfaces", def.interfaces());
            entry.put("fields", def.fields());

            List<Map<String, Object>> varList = new ArrayList<>();
            for (var varEntry : def.vars().entrySet()) {
                Map<String, Object> varObj = new LinkedHashMap<>();
                EventDefinition.VarEntry ve = varEntry.getValue();
                varObj.put("name", varEntry.getKey());
                varObj.put("javaType", ve.javaType());
                varObj.put("refTypeId", ve.refTypeId());
                varObj.put("description", ve.description());
                varList.add(varObj);
            }
            entry.put("variables", varList);
            result.add(entry);
        }

        return result;
    }

    private static @NotNull List<Map<String, Object>> collectTypeBindings(
            @NotNull TypeRegistry typeRegistry) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<String, TypeBindingMeta> entry : typeRegistry.allMeta().entrySet()) {
            Map<String, Object> obj = new LinkedHashMap<>();
            TypeBindingMeta meta = entry.getValue();
            obj.put("id", entry.getKey());
            obj.put("description", meta.description());
            obj.put("javaType", meta.javaType());
            obj.put("examples", meta.examples());
            obj.put("since", meta.since());
            obj.put("deprecated", meta.deprecated());
            result.add(obj);
        }

        for (String id : typeRegistry.allBindings().keySet()) {
            if (!typeRegistry.allMeta().containsKey(id)) {
                Map<String, Object> obj = new LinkedHashMap<>();
                obj.put("id", id);
                obj.put("description", null);
                obj.put("javaType", null);
                obj.put("examples", List.of());
                obj.put("since", null);
                obj.put("deprecated", false);
                result.add(obj);
            }
        }

        return result;
    }
}
