package dev.lumenlang.lumen.headless.sim.tests.typo;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Discovery-mode cases for single-literal typos in statement input.
 */
@SimulatorTest
public final class TypoStatementCases {

    private TypoStatementCases() {
    }

    @SimCase(name = "typo: 'st' for 'set'")
    public static SimulatorCase stForSet() {
        return SimulatorCase.statement("st x to 5");
    }

    @SimCase(name = "typo: 't' for 'to'")
    public static SimulatorCase tForTo() {
        return SimulatorCase.statement("set x t 5");
    }

    @SimCase(name = "typo: 'teleprt' for 'teleport'")
    public static SimulatorCase teleprtForTeleport() {
        return SimulatorCase.statement("teleprt p to l")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER).withVar("l", MinecraftTypes.LOCATION));
    }

    @SimCase(name = "typo: 'giv' for 'give'")
    public static SimulatorCase givForGive() {
        return SimulatorCase.statement("giv p diamond")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "typo: 'brodcast' for 'broadcast'")
    public static SimulatorCase brodcastForBroadcast() {
        return SimulatorCase.statement("brodcast \"hi\"");
    }

    @SimCase(name = "typo: 'kil' for 'kill'")
    public static SimulatorCase kilForKill() {
        return SimulatorCase.statement("kil p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }
}
