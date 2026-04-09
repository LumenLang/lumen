package dev.lumenlang.lumen.plugin.documentation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumenlang.lumen.api.LumenAddon;
import dev.lumenlang.lumen.api.event.AdvancedEventDefinition;
import dev.lumenlang.lumen.api.event.EventDefinition;
import dev.lumenlang.lumen.api.pattern.BlockVarInfo;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.api.type.TypeBindingMeta;
import dev.lumenlang.lumen.pipeline.addon.AddonManager;
import dev.lumenlang.lumen.pipeline.conditions.registry.RegisteredCondition;
import dev.lumenlang.lumen.pipeline.documentation.LumenDoc;
import dev.lumenlang.lumen.pipeline.events.EventDefRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredBlock;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpression;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPattern;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.pipeline.loop.RegisteredLoop;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import dev.lumenlang.lumen.plugin.Lumen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Generates per-addon documentation files ({@code .ldoc}).
 *
 * <p>Enabled via {@code extra.enable-documentation-tool}. Writes one file per
 * addon (and one for Lumen itself) into {@code plugins/Lumen/documentation/}.
 * Each file is named {@code {AddonName}-documentation.ldoc}. The file name
 * encodes addon identity, so individual entries never carry an owner field.
 *
 * <h2>Root fields</h2>
 * <ul>
 *   <li>{@code version} - the addon or plugin version string.</li>
 *   <li>{@code generatedAt} - ISO-8601 timestamp of when the file was written.</li>
 *   <li>{@code statements} - registered statement patterns.</li>
 *   <li>{@code expressions} - registered expression patterns.</li>
 *   <li>{@code conditions} - registered condition patterns.</li>
 *   <li>{@code blocks} - registered block patterns.</li>
 *   <li>{@code loopSources} - registered loop-source patterns.</li>
 *   <li>{@code events} - registered event definitions.</li>
 *   <li>{@code empty} - {@code true} if this addon registered nothing; tooling should mark it as unused and treat the version as potentially outdated.</li>
 *   <li>{@code typeBindings} - registered type bindings; only present on the Lumen file (subject to change in the future).</li>
 * </ul>
 *
 * <h2>Pattern entry fields (statements, expressions, conditions, blocks, loopSources)</h2>
 * <ul>
 *   <li>{@code patterns} - list of raw pattern strings that match this entry.</li>
 *   <li>{@code description} - human-readable explanation of what the pattern does.</li>
 *   <li>{@code examples} - list of example usage strings.</li>
 *   <li>{@code since} - version string when this pattern was first available, or {@code null}.</li>
 *   <li>{@code category} - category name from {@code PatternMeta}, or {@code null}.</li>
 *   <li>{@code deprecated} - {@code true} if the pattern is deprecated.</li>
 * </ul>
 *
 * <h2>Extra fields for expressions</h2>
 * <ul>
 *   <li>{@code returnTypeId} - the type ID of the return value (e.g. {@code "PLAYER"} or {@code "int"}), or {@code null}.</li>
 * </ul>
 *
 * <h2>Extra fields for blocks</h2>
 * <ul>
 *   <li>{@code supportsRootLevel} - {@code true} if the block is allowed at script root level.</li>
 *   <li>{@code supportsBlock} - {@code true} if the block is allowed inside another block.</li>
 *   <li>{@code variables} - list of variables injected into the block scope, each with:
 *     {@code name}, {@code type}, {@code objectType} (type ID or {@code null}),
 *     {@code nullable}, {@code description}, and optionally {@code metadata}.</li>
 * </ul>
 *
 * <h2>Event entry fields</h2>
 * <ul>
 *   <li>{@code name} - the event identifier used in scripts.</li>
 *   <li>{@code className} - the backing Java class name, or {@code null} for advanced events.</li>
 *   <li>{@code description} - human-readable explanation.</li>
 *   <li>{@code examples} - list of example usage strings.</li>
 *   <li>{@code since} - version string, or {@code null}.</li>
 *   <li>{@code category} - category string, or {@code null}.</li>
 *   <li>{@code deprecated} - {@code true} if the event is deprecated.</li>
 *   <li>{@code advanced} - {@code true} for {@link AdvancedEventDefinition} entries.</li>
 *   <li>{@code interfaces} - list of implemented interface names (advanced events only).</li>
 *   <li>{@code fields} - map of field name to value (advanced events only).</li>
 *   <li>{@code variables} - list of event variables, each with:
 *     {@code name}, {@code javaType}, {@code typeId}, {@code description},
 *     {@code nullable}, and optionally {@code metadata}.</li>
 * </ul>
 *
 * <h2>Type binding entry fields</h2>
 * <ul>
 *   <li>{@code id} - the unique type identifier (e.g. {@code "PLAYER"}).</li>
 *   <li>{@code description} - human-readable explanation, or {@code null}.</li>
 *   <li>{@code javaType} - the fully qualified Java type this ID maps to, or {@code null}.</li>
 *   <li>{@code examples} - list of example usage strings.</li>
 *   <li>{@code since} - version string, or {@code null}.</li>
 *   <li>{@code deprecated} - {@code true} if the type is deprecated.</li>
 * </ul>
 *
 * @see PatternRegistry
 * @see TypeRegistry
 * @see EventDefRegistry
 */
public final class DocumentationDumper {

    private static final String LUMEN = "Lumen";

    private DocumentationDumper() {
    }

    /**
     * Dumps all registrations into per-addon {@code .ldoc} files.
     *
     * @param patternRegistry the pattern registry
     * @param addonManager    the addon manager
     */
    public static void dump(@NotNull PatternRegistry patternRegistry, @NotNull AddonManager addonManager) {
        Path outputPath = Lumen.instance().getDataFolder().toPath().resolve("documentation");
        try {
            Files.createDirectories(outputPath);
        } catch (IOException e) {
            LumenLogger.warning("Failed to create directories for documentation output: " + e.getMessage());
            return;
        }
        TypeRegistry typeRegistry = patternRegistry.getTypeRegistry();

        CompletableFuture.runAsync(() -> {
            Map<String, List<Map<String, Object>>> statements = groupStatements(patternRegistry.getStatements());
            Map<String, List<Map<String, Object>>> expressions = groupExpressions(patternRegistry.getExpressions());
            Map<String, List<Map<String, Object>>> conditions = groupConditions(patternRegistry.getConditionRegistry().getConditions());
            Map<String, List<Map<String, Object>>> blocks = groupBlocks(patternRegistry.getBlocks());
            Map<String, List<Map<String, Object>>> loops = groupLoops(patternRegistry.getLoopRegistry().getLoops());
            Map<String, List<Map<String, Object>>> events = collectEvents();

            Set<String> addonNames = new LinkedHashSet<>();
            addonNames.add(LUMEN);
            addonNames.addAll(statements.keySet());
            addonNames.addAll(expressions.keySet());
            addonNames.addAll(conditions.keySet());
            addonNames.addAll(blocks.keySet());
            addonNames.addAll(loops.keySet());
            addonNames.addAll(events.keySet());
            for (LumenAddon addon : addonManager.addons()) {
                addonNames.add(addon.name());
            }

            Gson gson = new GsonBuilder()
                    .serializeNulls()
                    .disableHtmlEscaping()
                    .create();

            for (String name : addonNames) {
                Map<String, Object> root = new LinkedHashMap<>();
                if (LUMEN.equals(name)) {
                    root.put("version", Lumen.instance().getDescription().getVersion());
                } else {
                    LumenAddon addon = addonManager.get(name);
                    root.put("version", addon != null ? addon.version() : null);
                }
                List<Map<String, Object>> stmts = statements.getOrDefault(name, List.of());
                List<Map<String, Object>> exprs = expressions.getOrDefault(name, List.of());
                List<Map<String, Object>> conds = conditions.getOrDefault(name, List.of());
                List<Map<String, Object>> blks = blocks.getOrDefault(name, List.of());
                List<Map<String, Object>> lps = loops.getOrDefault(name, List.of());
                List<Map<String, Object>> evts = events.getOrDefault(name, List.of());
                List<Map<String, Object>> types = LUMEN.equals(name) ? collectTypeBindings(typeRegistry) : List.of();

                boolean empty = stmts.isEmpty() && exprs.isEmpty() && conds.isEmpty() && blks.isEmpty() && lps.isEmpty() && evts.isEmpty() && types.isEmpty();

                root.put("empty", empty);
                root.put("generatedAt", Instant.now().toString());
                root.put("statements", stmts);
                root.put("expressions", exprs);
                root.put("conditions", conds);
                root.put("blocks", blks);
                root.put("loopSources", lps);
                root.put("events", evts);
                root.put("typeBindings", types);

                try {
                    LumenDoc.write(outputPath.resolve(LumenDoc.resourceName(name)), gson.toJson(root));
                    LumenLogger.debug("DocumentationDumper", "Written " + LumenDoc.resourceName(name));
                } catch (IOException e) {
                    LumenLogger.warning("Failed to write documentation for " + name + ": " + e.getMessage());
                }
            }

            LumenLogger.info("Documentation dumped for " + addonNames.size() + " source(s): " + String.join(", ", addonNames));
        });
    }

    private static @NotNull String ownerOf(@Nullable String by) {
        return by != null ? by : LUMEN;
    }

    private static @NotNull Map<String, List<Map<String, Object>>> groupStatements(@NotNull List<RegisteredPattern> statements) {
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

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (Object handler : handlerOrder) {
            PatternMeta meta = handlerToMeta.get(handler);
            result.computeIfAbsent(ownerOf(meta.by()), k -> new ArrayList<>())
                    .add(buildPatternEntry(handlerToPatterns.get(handler), meta));
        }
        return result;
    }

    private static @NotNull Map<String, List<Map<String, Object>>> groupExpressions(@NotNull List<RegisteredExpression> expressions) {
        IdentityHashMap<Object, List<String>> handlerToPatterns = new IdentityHashMap<>();
        IdentityHashMap<Object, PatternMeta> handlerToMeta = new IdentityHashMap<>();
        IdentityHashMap<Object, String> handlerToReturnType = new IdentityHashMap<>();
        List<Object> handlerOrder = new ArrayList<>();

        for (RegisteredExpression re : expressions) {
            Object handler = re.handler();
            handlerToPatterns.computeIfAbsent(handler, k -> {
                handlerOrder.add(k);
                return new ArrayList<>();
            }).add(re.pattern().raw());
            handlerToMeta.putIfAbsent(handler, re.meta());
            if (re.returnTypeId() != null) {
                handlerToReturnType.putIfAbsent(handler, re.returnTypeId());
            }
        }

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (Object handler : handlerOrder) {
            PatternMeta meta = handlerToMeta.get(handler);
            Map<String, Object> entry = buildPatternEntry(handlerToPatterns.get(handler), meta);
            entry.put("returnTypeId", handlerToReturnType.get(handler));
            result.computeIfAbsent(ownerOf(meta.by()), k -> new ArrayList<>()).add(entry);
        }
        return result;
    }

    private static @NotNull Map<String, List<Map<String, Object>>> groupConditions(@NotNull List<RegisteredCondition> conditions) {
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

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (Object handler : handlerOrder) {
            PatternMeta meta = handlerToMeta.get(handler);
            result.computeIfAbsent(ownerOf(meta.by()), k -> new ArrayList<>())
                    .add(buildPatternEntry(handlerToPatterns.get(handler), meta));
        }
        return result;
    }

    private static @NotNull Map<String, List<Map<String, Object>>> groupBlocks(@NotNull List<RegisteredBlock> blocks) {
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

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (Object handler : handlerOrder) {
            PatternMeta meta = handlerToMeta.get(handler);
            Map<String, Object> entry = buildPatternEntry(handlerToPatterns.get(handler), meta);
            entry.put("supportsRootLevel", handlerToRootLevel.get(handler));
            entry.put("supportsBlock", handlerToSupportsBlock.get(handler));
            List<BlockVarInfo> vars = handlerToVars.get(handler);
            if (vars != null && !vars.isEmpty()) {
                List<Map<String, Object>> varList = new ArrayList<>();
                for (BlockVarInfo v : vars) {
                    Map<String, Object> varObj = new LinkedHashMap<>();
                    varObj.put("name", v.name());
                    varObj.put("type", v.type());
                    varObj.put("objectType", v.objectType() != null ? v.objectType().id() : null);
                    varObj.put("nullable", v.metadata().getOrDefault("nullable", false));
                    varObj.put("description", v.description());
                    if (!v.metadata().isEmpty()) {
                        varObj.put("metadata", v.metadata());
                    }
                    varList.add(varObj);
                }
                entry.put("variables", varList);
            }
            result.computeIfAbsent(ownerOf(meta.by()), k -> new ArrayList<>()).add(entry);
        }
        return result;
    }

    private static @NotNull Map<String, List<Map<String, Object>>> groupLoops(@NotNull List<RegisteredLoop> loops) {
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

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (Object handler : handlerOrder) {
            PatternMeta meta = handlerToMeta.get(handler);
            result.computeIfAbsent(ownerOf(meta.by()), k -> new ArrayList<>())
                    .add(buildPatternEntry(handlerToPatterns.get(handler), meta));
        }
        return result;
    }

    private static @NotNull Map<String, Object> buildPatternEntry(@NotNull List<String> patterns, @NotNull PatternMeta meta) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("patterns", patterns);
        entry.put("description", meta.description());
        entry.put("examples", meta.examples());
        entry.put("since", meta.since());
        entry.put("category", meta.category() != null ? meta.category().name() : null);
        entry.put("deprecated", meta.deprecated());
        return entry;
    }

    private static @NotNull Map<String, List<Map<String, Object>>> collectEvents() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        for (EventDefinition def : EventDefRegistry.apiDefinitions().values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", def.name());
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
                varObj.put("typeId", var.typeId());
                varObj.put("description", var.description());
                varObj.put("nullable", var.metadata().getOrDefault("nullable", false));
                if (!var.metadata().isEmpty()) {
                    varObj.put("metadata", var.metadata());
                }
                varList.add(varObj);
            }
            entry.put("variables", varList);
            result.computeIfAbsent(ownerOf(def.by()), k -> new ArrayList<>()).add(entry);
        }

        for (AdvancedEventDefinition def : EventDefRegistry.advancedDefs().values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", def.name());
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
                varObj.put("typeId", ve.typeId());
                varObj.put("description", ve.description());
                varObj.put("nullable", ve.metadata().getOrDefault("nullable", false));
                if (!ve.metadata().isEmpty()) {
                    varObj.put("metadata", ve.metadata());
                }
                varList.add(varObj);
            }
            entry.put("variables", varList);
            result.computeIfAbsent(ownerOf(def.by()), k -> new ArrayList<>()).add(entry);
        }

        return result;
    }

    private static @NotNull List<Map<String, Object>> collectTypeBindings(@NotNull TypeRegistry typeRegistry) {
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
