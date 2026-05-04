package dev.lumenlang.lumen.headless.sim.cases;

import dev.lumenlang.lumen.api.codegen.source.SourceMap;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.codegen.BlockContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.codegen.source.SourceMapImpl;
import dev.lumenlang.lumen.pipeline.data.DataSchema;
import dev.lumenlang.lumen.pipeline.language.nodes.BlockNode;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder that produces a {@link TypeEnvImpl} preconfigured for pattern-simulator tests.
 */
public final class EnvSimulator {

    private final List<Var> vars = new ArrayList<>();
    private final List<Var> globals = new ArrayList<>();
    private final Map<String, Map<String, LumenType>> dataSchemas = new LinkedHashMap<>();
    private @NotNull String fakeSource = "<test>";

    private EnvSimulator() {
    }

    /**
     * Empty environment with no declared variables.
     */
    public static @NotNull EnvSimulator empty() {
        return new EnvSimulator();
    }

    /**
     * Empty environment ready to be populated.
     */
    public static @NotNull EnvSimulator create() {
        return new EnvSimulator();
    }

    /**
     * Declares a local variable visible to the simulator.
     */
    public @NotNull EnvSimulator withVar(@NotNull String name, @NotNull LumenType type) {
        vars.add(new Var(name, type));
        return this;
    }

    /**
     * Declares a script-wide global visible to the simulator.
     */
    public @NotNull EnvSimulator withGlobal(@NotNull String name, @NotNull LumenType type) {
        globals.add(new Var(name, type));
        return this;
    }

    /**
     * Registers a data schema with the given field types.
     *
     * @param typeName lowercase data type name as it appears in the script
     * @param fields   field name to type mapping in declaration order
     */
    public @NotNull EnvSimulator withDataSchema(@NotNull String typeName, @NotNull Map<String, LumenType> fields) {
        dataSchemas.put(typeName, new LinkedHashMap<>(fields));
        return this;
    }

    /**
     * Sets the fake source string the env's {@link SourceMap} reports.
     */
    public @NotNull EnvSimulator withSource(@NotNull String source) {
        this.fakeSource = source;
        return this;
    }

    /**
     * Fresh {@link TypeEnvImpl} with the configured state applied.
     */
    public @NotNull TypeEnvImpl build() {
        TypeEnvImpl env = new TypeEnvImpl();
        env.setSourceMap(new SourceMapImpl(fakeSource));
        BlockNode rootHead = new BlockNode(0, 1, "<test>", List.of());
        env.enterBlock(new BlockContextImpl(rootHead, null, List.of(rootHead), 0));
        for (Var v : vars) {
            env.defineVar(v.name(), v.type(), v.name());
        }
        for (Var g : globals) {
            env.registerGlobal(new VarRef(g.name(), g.type(), g.name()));
        }
        for (Map.Entry<String, Map<String, LumenType>> e : dataSchemas.entrySet()) {
            env.registerDataSchema(new DataSchema(e.getKey(), Map.copyOf(e.getValue())));
        }
        return env;
    }

    private record Var(@NotNull String name, @NotNull LumenType type) {
    }
}
