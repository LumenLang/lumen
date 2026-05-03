package dev.lumenlang.lumen.headless.sim.tests.typeerror;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Cases where the input's argument types do not match what the matching pattern requires.
 */
@SimulatorTest
public final class TypeMismatchCases {

    private TypeMismatchCases() {
    }

    @SimCase(name = "type-mismatched assignment to INT var")
    public static SimulatorCase intGotString() {
        return SimulatorCase.statement("set count to \"hi\"")
                .env(EnvSimulator.create().withVar("count", PrimitiveType.INT).withVar("p", MinecraftTypes.PLAYER))
                .expectAnySuggestions();
    }
}
