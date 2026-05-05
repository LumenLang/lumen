package dev.lumenlang.lumen.headless.sim.tests.stress;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Long inputs that stress BFS depth, combinatorial token removal, and shape-match cutoffs.
 */
@SimulatorTest
public final class LongInputs {

    private LongInputs() {
    }

    @SimCase(name = "long: full title with subtitle and timing")
    public static SimulatorCase fullTitle() {
        return SimulatorCase.statement("send title \"hi\" with subtitle \"sub\" to p with fade in 10 stay 20 fade out 10")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "long: title with extra junk between every part")
    public static SimulatorCase titleWithJunk() {
        return SimulatorCase.statement("send weird title nope \"hi\" foo to bar p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "long: garbage stream of unrelated words")
    public static SimulatorCase garbageStream() {
        return SimulatorCase.statement("lorem ipsum dolor sit amet consectetur");
    }

    @SimCase(name = "long: chained set with arithmetic words")
    public static SimulatorCase chainedSet() {
        return SimulatorCase.statement("set count to 1 plus 2 plus 3 plus 4 plus 5")
                .env(EnvSimulator.create().withVar("count", PrimitiveType.INT));
    }

    @SimCase(name = "long: deeply nested possessive chain")
    public static SimulatorCase deepPossessive() {
        return SimulatorCase.expression("get p's target's location's world")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }
}
