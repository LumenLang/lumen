package dev.lumenlang.lumen.headless.sim.tests.missing;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs that omit a required pattern argument. Several lock in cross-pattern matches the sim
 * picks today; revisit when the sim recognises partial intent better.
 */
@SimulatorTest
public final class MissingArguments {

    private MissingArguments() {
    }

    @SimCase(name = "missing: damage without target")
    public static SimulatorCase damageNoTarget() {
        return SimulatorCase.statement("damage by 5");
    }

    @SimCase(name = "missing: damage target but no amount")
    public static SimulatorCase damageNoAmount() {
        return SimulatorCase.statement("damage p by")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "missing: spawn without entity type")
    public static SimulatorCase spawnNoType() {
        return SimulatorCase.statement("spawn at l")
                .env(EnvSimulator.create().withVar("l", MinecraftTypes.LOCATION));
    }

    @SimCase(name = "missing: give with player only")
    public static SimulatorCase givePlayerOnly() {
        return SimulatorCase.statement("give p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "missing: teleport with player only")
    public static SimulatorCase teleportPlayerOnly() {
        return SimulatorCase.statement("teleport p to")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "missing: set time of without world")
    public static SimulatorCase setTimeNoWorld() {
        return SimulatorCase.statement("set time of to 6000");
    }
}
