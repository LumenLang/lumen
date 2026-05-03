package dev.lumenlang.lumen.headless.sim.tests.reorder;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Cases where the input has the right tokens in the wrong order.
 */
@SimulatorTest
public final class ReorderCases {

    private ReorderCases() {
    }

    @SimCase(name = "reorder: message arguments swapped")
    public static SimulatorCase swappedMessageArgs() {
        return SimulatorCase.statement("message \"hello\" p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectAnySuggestions();
    }
}
