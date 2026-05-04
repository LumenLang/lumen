package dev.lumenlang.lumen.headless.sim.tests.exact;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs that the user clearly wrote correctly: the simulator's top suggestion must equal the
 * matching pattern, with zero issues attached and confidence 1.0.
 */
@SimulatorTest
public final class CleanMatches {

    private CleanMatches() {
    }

    @SimCase(name = "clean: teleport p to l")
    public static SimulatorCase teleport() {
        return SimulatorCase.statement("teleport p to l")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER).withVar("l", MinecraftTypes.LOCATION));
    }

    @SimCase(name = "clean: give p diamond")
    public static SimulatorCase give() {
        return SimulatorCase.statement("give p diamond")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "clean: message p \"hello\"")
    public static SimulatorCase message() {
        return SimulatorCase.statement("message p \"hello\"")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "clean: reassign existing INT var")
    public static SimulatorCase reassignInt() {
        return SimulatorCase.statement("set count to 5")
                .env(EnvSimulator.create().withVar("count", PrimitiveType.INT));
    }

    @SimCase(name = "clean: kill player variable")
    public static SimulatorCase kill() {
        return SimulatorCase.statement("kill p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "clean: send title with all parts")
    public static SimulatorCase sendTitle() {
        return SimulatorCase.statement("send title \"hi\" to p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "clean: damage entity by amount")
    public static SimulatorCase damage() {
        return SimulatorCase.statement("damage mob by 5")
                .env(EnvSimulator.create().withVar("mob", MinecraftTypes.LIVING_ENTITY));
    }

    @SimCase(name = "clean: heal entity")
    public static SimulatorCase healEntity() {
        return SimulatorCase.statement("heal mob")
                .env(EnvSimulator.create().withVar("mob", MinecraftTypes.LIVING_ENTITY));
    }

    @SimCase(name = "clean: spawn entity at location")
    public static SimulatorCase spawnAtLocation() {
        return SimulatorCase.statement("spawn zombie at l")
                .env(EnvSimulator.create().withVar("l", MinecraftTypes.LOCATION));
    }

    @SimCase(name = "clean: declare new STRING var")
    public static SimulatorCase declareNewVar() {
        return SimulatorCase.statement("set greeting to \"hi\"");
    }

    @SimCase(name = "clean: reassign STRING var with STRING value")
    public static SimulatorCase reassignString() {
        return SimulatorCase.statement("set greeting to \"hello\"")
                .env(EnvSimulator.create().withVar("greeting", PrimitiveType.STRING));
    }
}
