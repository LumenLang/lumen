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

    @SimCase(name = "combo: typo + extra trailing word")
    public static SimulatorCase typoPlusExtra() {
        return SimulatorCase.statement("kil mob extra")
                .env(EnvSimulator.create().withVar("mob", MinecraftTypes.LIVING_ENTITY));
    }

    @SimCase(name = "combo: typo on verb + reordered args")
    public static SimulatorCase typoPlusReorder() {
        return SimulatorCase.statement("tp l to p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER).withVar("l", MinecraftTypes.LOCATION));
    }

    @SimCase(name = "combo: swap + extra trailing word")
    public static SimulatorCase swapPlusExtra() {
        return SimulatorCase.statement("damage by 5 mob now")
                .env(EnvSimulator.create().withVar("mob", MinecraftTypes.LIVING_ENTITY));
    }

    @SimCase(name = "combo: typo on verb + missing literal 'to'")
    public static SimulatorCase typoPlusMissingLiteral() {
        return SimulatorCase.statement("sand title \"hi\" p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
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

    @SimCase(name = "combo: extra leading + trailing words")
    public static SimulatorCase doubleExtra() {
        return SimulatorCase.statement("please kill p now")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "combo: capitalised verb + missing arg")
    public static SimulatorCase casePlusMissing() {
        return SimulatorCase.statement("Kill")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }
}
