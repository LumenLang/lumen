package dev.lumenlang.lumen.headless.sim.tests.typo;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Typos mixed with casing, digit-substitution, and arg-keyword variants.
 */
@SimulatorTest
public final class CornerCaseTypos {

    private CornerCaseTypos() {
    }

    @SimCase(name = "block typo: 'reapt' for 'repeat'")
    public static SimulatorCase reaptForRepeat() {
        return SimulatorCase.block("reapt 5 times");
    }

    @SimCase(name = "typo: capitalised 'Set x to 5'")
    public static SimulatorCase capitalised() {
        return SimulatorCase.statement("Set x to 5");
    }

    @SimCase(name = "typo: digit substitution 'set x t0 5'")
    public static SimulatorCase digitInTo() {
        return SimulatorCase.statement("set x t0 5");
    }

    @SimCase(name = "typo: arg keyword 'send actionber \"hi\" to p'")
    public static SimulatorCase actionberForActionbar() {
        return SimulatorCase.statement("send actionber \"hi\" to p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }
}
