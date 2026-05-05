package dev.lumenlang.lumen.headless.sim.tests.missing;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

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
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "missing literal: heal alone")
    public static SimulatorCase healAlone() {
        return SimulatorCase.statement("heal");
    }

    @SimCase(name = "missing literal: send title without 'to' before recipient")
    public static SimulatorCase sendNoTo() {
        return SimulatorCase.statement("send title \"hi\" p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }
}
