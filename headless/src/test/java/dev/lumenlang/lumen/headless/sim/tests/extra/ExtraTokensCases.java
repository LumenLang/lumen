package dev.lumenlang.lumen.headless.sim.tests.extra;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Cases where the input has additional tokens beyond what the matching pattern needs.
 */
@SimulatorTest
public final class ExtraTokensCases {

    private ExtraTokensCases() {
    }

    @SimCase(name = "extra tokens trailing a complete title pattern")
    public static SimulatorCase trailingJunk() {
        return SimulatorCase.statement("send title \"hi\" to p extra junk")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("send title %title:STRING% to %who:PLAYER%")
                .expectAnyIssue(SuggestionIssue.ExtraTokens.class);
    }
}
