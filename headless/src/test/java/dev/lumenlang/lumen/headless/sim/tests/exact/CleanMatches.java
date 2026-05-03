package dev.lumenlang.lumen.headless.sim.tests.exact;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Inputs that the user clearly wrote correctly. Some still produce suggestions today because the
 * simulator over-reaches. Those locked-in expectations are flagged inline so a future fix flips
 * them to {@code expectNoSuggestions()}.
 */
@SimulatorTest
public final class CleanMatches {

    private CleanMatches() {
    }

    @SimCase(name = "clean: teleport p to l")
    public static SimulatorCase teleport() {
        return SimulatorCase.statement("teleport p to l")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER).withVar("l", MinecraftTypes.LOCATION))
                .expectNoSuggestions();
    }

    @SimCase(name = "clean: give p diamond")
    public static SimulatorCase give() {
        return SimulatorCase.statement("give p diamond")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectNoSuggestions();
    }

    @SimCase(name = "clean: message p \"hello\"")
    public static SimulatorCase message() {
        return SimulatorCase.statement("message p \"hello\"")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectNoSuggestions();
    }

    /**
     * Buggy lock-in. {@code set count to 5} (count is INT) should reassign cleanly. Sim picks
     * an item-amount and a block-data pattern. Flip once reassignment beats stretch matches.
     */
    @SimCase(name = "clean: reassign existing INT var (BUG locked)")
    public static SimulatorCase reassignInt() {
        return SimulatorCase.statement("set count to 5")
                .env(EnvSimulator.create().withVar("count", PrimitiveType.INT))
                .expectTopPattern("set %i:ITEMSTACK_POSSESSIVE% amount [to] %amt:INT%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.63)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "clean: empty input")
    public static SimulatorCase emptyInput() {
        return SimulatorCase.statement("")
                .expectNoSuggestions();
    }

    @SimCase(name = "clean: broadcast string literal")
    public static SimulatorCase broadcast() {
        return SimulatorCase.statement("broadcast \"hello\"");
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
                .env(EnvSimulator.create().withVar("mob", MinecraftTypes.ENTITY));
    }

    @SimCase(name = "clean: heal entity")
    public static SimulatorCase healEntity() {
        return SimulatorCase.statement("heal mob")
                .env(EnvSimulator.create().withVar("mob", MinecraftTypes.ENTITY))
                .expectNoSuggestions();
    }

    @SimCase(name = "clean: spawn entity at location")
    public static SimulatorCase spawnAtLocation() {
        return SimulatorCase.statement("spawn zombie at l")
                .env(EnvSimulator.create().withVar("l", MinecraftTypes.LOCATION))
                .expectNoSuggestions();
    }

    /**
     * Buggy lock-in. {@code set greeting to "hi"} should declare a new STRING var, not match a
     * BLOCK data pattern. Flip once new-var declaration is recognised by the sim.
     */
    @SimCase(name = "clean: declare new STRING var (BUG locked)")
    public static SimulatorCase declareNewVar() {
        return SimulatorCase.statement("set greeting to \"hi\"")
                .expectTopPattern("set %b:BLOCK% data [to] %data:STRING%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.63)
                .expectSuggestionCount(2, 2);
    }

    /**
     * Buggy lock-in. Reassigning a STRING var with a STRING value should match cleanly. Sim picks
     * the BLOCK pattern instead. Flip once reassignment is preferred over BLOCK.
     */
    @SimCase(name = "clean: reassign STRING var with STRING value (BUG locked)")
    public static SimulatorCase reassignString() {
        return SimulatorCase.statement("set greeting to \"hello\"")
                .env(EnvSimulator.create().withVar("greeting", PrimitiveType.STRING))
                .expectTopPattern("set %b:BLOCK% data [to] %data:STRING%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.63)
                .expectSuggestionCount(2, 2);
    }
}
