package dev.lumenlang.lumen.headless;

import dev.lumenlang.lumen.api.codegen.source.SourceMap;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.codegen.BlockContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.codegen.source.SourceMapImpl;
import dev.lumenlang.lumen.pipeline.language.nodes.BlockNode;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.suggestor.PatternSuggestor;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.CompletionItem;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.GhostText;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.PlanEntry;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.SignatureItem;
import dev.lumenlang.lumen.pipeline.language.suggestor.result.SuggestorPlan;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks one input line keystroke by keystroke and prints the suggestor's view at every cursor
 * position so real typing patterns can be inspected for bad rankings, missing hints, or wrong
 * signature shape.
 */
public final class SuggestorTypingSim {

    private SuggestorTypingSim() {
    }

    public static void main(@NotNull String[] args) {
        new HeadlessLumen();
        Map<String, LumenType> commandEnv = new LinkedHashMap<>();
        commandEnv.put("player", MinecraftTypes.PLAYER);
        commandEnv.put("sender", MinecraftTypes.ENTITY);
        commandEnv.put("world", MinecraftTypes.WORLD);
        runOne("set x to ", PatternSimulator.Scope.STATEMENT, commandEnv);
        runOne("set ", PatternSimulator.Scope.STATEMENT, commandEnv);
        runOne("send a", PatternSimulator.Scope.STATEMENT, commandEnv);
        runOne("send actionbar \"hi\" to ", PatternSimulator.Scope.STATEMENT, commandEnv);
        runOne("give player diamond", PatternSimulator.Scope.STATEMENT, commandEnv);
        runOne("if ", PatternSimulator.Scope.CONDITION, commandEnv);
        runOne("chance ", PatternSimulator.Scope.STATEMENT, commandEnv);
        runOne("teleport player to ", PatternSimulator.Scope.STATEMENT, commandEnv);
    }

    private static void runOne(@NotNull String line, @NotNull PatternSimulator.Scope scope, @NotNull Map<String, LumenType> vars) {
        System.out.println();
        System.out.println("=========================================================================");
        System.out.println("input: '" + line + "'   scope=" + scope + "   env=" + envString(vars));
        System.out.println("=========================================================================");
        PatternRegistry reg = PatternRegistry.instance();
        for (int col = 0; col <= line.length(); col++) {
            String typed = line.substring(0, col);
            TypeEnvImpl env = buildEnv(vars);
            SuggestorPlan plan = PatternSuggestor.suggest(line, col, reg, env, scope);
            printStep(typed, col, plan);
        }
    }

    private static void printStep(@NotNull String typed, int col, @NotNull SuggestorPlan plan) {
        System.out.println();
        System.out.println("[col " + col + "]  '" + typed + "|'");
        System.out.println("  active: " + plan.active().policy() + " '" + plan.active().text() + "'");
        SignatureItem sig = plan.signature();
        if (sig != null) {
            System.out.println("  sig:    " + sig.renderedShape() + "   (active segment=" + sig.activeSegmentIndex() + ")");
        } else {
            System.out.println("  sig:    <none>");
        }
        GhostText ghost = plan.ghostText();
        if (ghost != null) System.out.println("  ghost @" + ghost.insertAt() + ": '" + ghost.text() + "'");
        List<CompletionItem> completions = plan.completions();
        int show = Math.min(completions.size(), 5);
        System.out.println("  completions: " + completions.size() + (show < completions.size() ? "  (top " + show + ")" : ""));
        for (int i = 0; i < show; i++) {
            CompletionItem c = completions.get(i);
            System.out.println("    " + String.format("%.3f", c.score()) + "  " + c.displayLabel() + "  [" + c.detail() + "]");
        }
        if (!plan.entries().isEmpty()) {
            PlanEntry top = plan.entries().get(0);
            System.out.println("  top entry: " + top.pattern().raw() + "   score=" + String.format("%.3f", top.score()) + "   atBinding=" + top.position().atBindingId() + "   consumed=" + top.position().consumedTokens());
        }
    }

    private static @NotNull String envString(@NotNull Map<String, LumenType> vars) {
        if (vars.isEmpty()) return "<empty>";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, LumenType> e : vars.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(e.getKey()).append(":").append(e.getValue().displayName());
        }
        return sb.toString();
    }

    private static @NotNull TypeEnvImpl buildEnv(@NotNull Map<String, LumenType> vars) {
        TypeEnvImpl env = new TypeEnvImpl();
        SourceMap source = new SourceMapImpl("<sim>");
        env.setSourceMap(source);
        BlockNode rootHead = new BlockNode(0, 1, "<sim>", List.of());
        env.enterBlock(new BlockContextImpl(rootHead, null, List.of(rootHead), 0));
        for (Map.Entry<String, LumenType> entry : new LinkedHashMap<>(vars).entrySet()) {
            env.defineVar(entry.getKey(), entry.getValue(), entry.getKey());
        }
        return env;
    }
}
