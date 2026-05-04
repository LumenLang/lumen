package dev.lumenlang.lumen.headless.sim.tests.missing;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Inputs that omit a required literal keyword (e.g. {@code title}, {@code to}).
 */
@SimulatorTest
public final class MissingLiterals {

    private MissingLiterals() {
    }

    @SimCase(name = "missing literal: send needs 'title' or 'actionbar'")
    public static SimulatorCase sendMissingTitle() {
        return SimulatorCase.statement("send \"hi\" to p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectAnySuggestions()
                .expect("top suggestion is a 'send' pattern", suggestions -> {
                    if (suggestions.isEmpty()) return "no suggestions";
                    String top = suggestions.get(0).pattern().raw();
                    return top.startsWith("send ") ? null : "top pattern is '" + top + "'";
                });
    }

    @SimCase(name = "missing literal: heal alone")
    public static SimulatorCase healAlone() {
        return SimulatorCase.statement("heal")
                .expectContainsPattern("(heal|restore) %e:ENTITY%");
    }

    @SimCase(name = "missing literal: send title without 'to' before recipient")
    public static SimulatorCase sendNoTo() {
        return SimulatorCase.statement("send title \"hi\" p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("send title %title:STRING% to %who:PLAYER%")
                .expectPrimaryIssue(SuggestionIssue.MissingLiteral.class)
                .expectAnyIssue(SuggestionIssue.MissingLiteral.class)
                .expectConfidenceAtLeast(0.40)
                .expectSuggestionCount(2, 2);
    }
}
