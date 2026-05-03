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

    @SimCase(name = "missing: damage without target")
    public static SimulatorCase damageNoTarget() {
        return SimulatorCase.statement("damage by 5")
                .expectTopPattern("damage %e:ENTITY% [by] %val:INT%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(1, 1);
    }

    @SimCase(name = "missing: damage target but no amount")
    public static SimulatorCase damageNoAmount() {
        return SimulatorCase.statement("damage p by")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("damage %e:ENTITY% [by] %val:INT%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(1, 1);
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

    /**
     * Buggy lock-in. After the sandbox fix, the catch-all {@code set %name to %val} pattern
     * accepts the BFS-1 reduction (drop {@code of}) cleanly and outranks the semantically
     * correct time pattern's TypeMismatch fallback. Flip once catch-all patterns are
     * deboosted relative to literal-rich competitors.
     */
    @SimCase(name = "missing: set time of without world (BUG locked)")
    public static SimulatorCase setTimeNoWorld() {
        return SimulatorCase.statement("set time of to 6000")
                .expectTopPattern("set %name:IDENT% to %val:EXPR%")
                .expectPrimaryIssue(SuggestionIssue.ExtraTokens.class)
                .expectAnyIssue(SuggestionIssue.ExtraTokens.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }
}
