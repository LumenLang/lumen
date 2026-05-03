package dev.lumenlang.lumen.headless.sim.tests.missing;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Cases where the input is missing a required literal keyword from the matching pattern.
 */
@SimulatorTest
public final class MissingLiteralCases {

    private MissingLiteralCases() {
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

    @SimCase(name = "missing argument: 'heal' alone")
    public static SimulatorCase healAlone() {
        return SimulatorCase.statement("heal")
                .expectContainsPattern("(heal|restore) [the] %who:PLAYER%");
    }
}
