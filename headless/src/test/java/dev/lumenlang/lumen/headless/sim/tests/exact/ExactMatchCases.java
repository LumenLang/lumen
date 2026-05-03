package dev.lumenlang.lumen.headless.sim.tests.exact;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Cases where the input is a perfect match and the simulator should produce no suggestions.
 */
@SimulatorTest
public final class ExactMatchCases {

    private ExactMatchCases() {
    }

    @SimCase(name = "exact teleport pattern produces no suggestions")
    public static SimulatorCase exactTeleport() {
        return SimulatorCase.statement("teleport p to l")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER).withVar("l", MinecraftTypes.LOCATION))
                .expectNoSuggestions();
    }

    @SimCase(name = "exact give pattern produces no suggestions")
    public static SimulatorCase exactGive() {
        return SimulatorCase.statement("give p diamond")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectNoSuggestions();
    }

    @SimCase(name = "exact message pattern produces no suggestions")
    public static SimulatorCase exactMessage() {
        return SimulatorCase.statement("message p \"hello\"")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectNoSuggestions();
    }

    @SimCase(name = "reassignment to existing INT var produces no suggestions")
    public static SimulatorCase reassignInt() {
        return SimulatorCase.statement("set count to 5")
                .env(EnvSimulator.create().withVar("count", PrimitiveType.INT))
                .expectNoSuggestions();
    }

    @SimCase(name = "empty input produces no suggestions")
    public static SimulatorCase emptyInput() {
        return SimulatorCase.statement("")
                .expectNoSuggestions();
    }
}
