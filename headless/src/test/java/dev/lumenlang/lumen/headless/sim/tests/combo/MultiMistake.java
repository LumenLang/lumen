package dev.lumenlang.lumen.headless.sim.tests.combo;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs that combine two or more independent mistakes in the same statement.
 */
@SimulatorTest
public final class MultiMistake {

    private MultiMistake() {
    }

    @SimCase(name = "combo: two typos in one statement")
    public static SimulatorCase doubleTypo() {
        return SimulatorCase.statement("kil zomb")
                .env(EnvSimulator.create().withVar("zomb", MinecraftTypes.LIVING_ENTITY));
    }

    @SimCase(name = "combo: typo + wrong type")
    public static SimulatorCase typoPlusWrongType() {
        return SimulatorCase.statement("damge \"alice\" by 5");
    }

    @SimCase(name = "combo: capitalised verb + missing arg")
    public static SimulatorCase casePlusMissing() {
        return SimulatorCase.statement("Kill")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }
}
