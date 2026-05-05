package dev.lumenlang.lumen.headless.sim.tests.alien;

import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Skript dialect: curly-brace vars, loop-all syntax, made-up verbs, named-item builders. Lumen
 * does not implement any of these and the simulator should not pretend they are near-matches.
 */
@SimulatorTest
public final class SkriptDialect {

    private SkriptDialect() {
    }

    @SimCase(name = "skript: curly local var assignment")
    public static SimulatorCase curlyLocal() {
        return SimulatorCase.statement("set {_score} to 0");
    }

    @SimCase(name = "skript: curly global var assignment")
    public static SimulatorCase curlyGlobal() {
        return SimulatorCase.statement("set {score::%player%} to 0");
    }

    @SimCase(name = "skript: loop all players")
    public static SimulatorCase loopPlayers() {
        return SimulatorCase.block("loop all players:");
    }

    @SimCase(name = "skript: loop all entities")
    public static SimulatorCase loopEntities() {
        return SimulatorCase.block("loop all entities in world \"world\":");
    }

    @SimCase(name = "skript: send to all players")
    public static SimulatorCase sendToAll() {
        return SimulatorCase.statement("send \"hi\" to all players");
    }

    @SimCase(name = "skript: wait then")
    public static SimulatorCase waitThen() {
        return SimulatorCase.statement("wait 5 ticks then teleport player to spawn");
    }

    @SimCase(name = "skript: make player jump")
    public static SimulatorCase makeJump() {
        return SimulatorCase.statement("make player jump");
    }

    @SimCase(name = "skript: make player invisible")
    public static SimulatorCase makeInvisible() {
        return SimulatorCase.statement("make player invisible to other players");
    }

    @SimCase(name = "skript: give named item")
    public static SimulatorCase giveNamed() {
        return SimulatorCase.statement("give player a diamond sword named \"sharp\"");
    }

    @SimCase(name = "skript: on event header")
    public static SimulatorCase onEvent() {
        return SimulatorCase.block("on rightclick on a sign:");
    }

    @SimCase(name = "skript: every percent placeholder")
    public static SimulatorCase percentPlaceholder() {
        return SimulatorCase.statement("send \"%player% joined\" to console");
    }

    @SimCase(name = "skript: list expression")
    public static SimulatorCase listExpr() {
        return SimulatorCase.statement("add player to {team::red::*}");
    }

    @SimCase(name = "skript: function call colon")
    public static SimulatorCase functionCall() {
        return SimulatorCase.statement("function greet(p: player):");
    }

    @SimCase(name = "skript: stop trigger")
    public static SimulatorCase stopTrigger() {
        return SimulatorCase.statement("stop the trigger");
    }
}
