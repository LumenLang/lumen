package dev.lumenlang.lumen.headless.sim.tests.missing;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Inputs that omit a required pattern argument. Several lock in cross-pattern matches the sim
 * picks today; revisit when the sim recognises partial intent better.
 */
@SimulatorTest
public final class MissingArguments {

    private MissingArguments() {
    }

    /**
     * Buggy lock-in. {@code damage by 5} should rank a damage pattern, not {@code split by}.
     * Flip once partial-prefix matching prefers the right verb.
     */
    @SimCase(name = "missing: damage without target (BUG locked)")
    public static SimulatorCase damageNoTarget() {
        return SimulatorCase.statement("damage by 5")
                .expectTopPattern("%s:STRING% split by %delim:STRING%")
                .expectPrimaryIssue(SuggestionIssue.Reorder.class)
                .expectAnyIssue(SuggestionIssue.Reorder.class)
                .expectConfidenceAtLeast(0.75)
                .expectSuggestionCount(2, 2);
    }

    /**
     * Buggy lock-in. Same root issue as {@link #damageNoTarget()}.
     */
    @SimCase(name = "missing: damage target but no amount (BUG locked)")
    public static SimulatorCase damageNoAmount() {
        return SimulatorCase.statement("damage p by")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("%s:STRING% split by %delim:STRING%")
                .expectPrimaryIssue(SuggestionIssue.Reorder.class)
                .expectAnyIssue(SuggestionIssue.Reorder.class)
                .expectConfidenceAtLeast(0.75)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "missing: spawn without entity type")
    public static SimulatorCase spawnNoType() {
        return SimulatorCase.statement("spawn at l")
                .env(EnvSimulator.create().withVar("l", MinecraftTypes.LOCATION))
                .expectTopPattern("spawn %type:ENTITY_TYPE% [at] %loc:LOCATION%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "missing: give with player only")
    public static SimulatorCase givePlayerOnly() {
        return SimulatorCase.statement("give p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("give %who:PLAYER% %item:MATERIAL% %amt:INT%")
                .expectConfidenceAtLeast(0.85)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "missing: teleport with player only")
    public static SimulatorCase teleportPlayerOnly() {
        return SimulatorCase.statement("teleport p to")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("(teleport|tp) %who:PLAYER% [to] %target:PLAYER%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "missing: set time of without world")
    public static SimulatorCase setTimeNoWorld() {
        return SimulatorCase.statement("set time of to 6000")
                .expectTopPattern("set time [of] %w:WORLD% [to] %val:INT%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }
}
