package dev.lumenlang.lumen.headless.sim.tests.typo;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Typos mixed with whitespace, casing, digit-substitution, and arg-keyword variants. Stress
 * cases for the typo scorer.
 */
@SimulatorTest
public final class CornerCaseTypos {

    private CornerCaseTypos() {
    }

    @SimCase(name = "block typo: 'reapt' for 'repeat'")
    public static SimulatorCase reaptForRepeat() {
        return SimulatorCase.block("reapt 5 times");
    }

    /**
     * Buggy lock-in. Triple whitespace shouldn't change matching, but sim drifts to the location
     * coord getter (same root cause as {@code tForTo}).
     */
    @SimCase(name = "typo: triple whitespace 'set    x   t   5' (BUG locked)")
    public static SimulatorCase tripleSpace() {
        return SimulatorCase.statement("set    x   t   5");
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
