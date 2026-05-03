package dev.lumenlang.lumen.headless.sim.tests.conditions;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Condition-side inputs (permission checks, comparisons, region checks). Most lock in
 * cross-pattern picks the sim makes when no clean condition pattern is found.
 */
@SimulatorTest
public final class ConditionForms {

    private ConditionForms() {
    }

    /**
     * Buggy lock-in. Permission checks should match a permission pattern, sim picks an
     * attribute pattern.
     */
    @SimCase(name = "cond: player has permission (BUG locked)")
    public static SimulatorCase hasPermission() {
        return SimulatorCase.condition("p has permission \"command.use\"")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("%e:ENTITY% has %attr:ATTRIBUTE%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.ExtraTokens.class)
                .expectConfidenceAtLeast(0.63)
                .expectSuggestionCount(2, 2);
    }

    /**
     * Buggy lock-in. Same root issue as {@link #hasPermission()}, with a typo on the literal.
     */
    @SimCase(name = "cond: 'permision' typo (BUG locked)")
    public static SimulatorCase permisionTypo() {
        return SimulatorCase.condition("p has permision \"command.use\"")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("%e:ENTITY% has %attr:ATTRIBUTE%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.ExtraTokens.class)
                .expectConfidenceAtLeast(0.63)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "cond: p's health > 10")
    public static SimulatorCase possessiveHealthCmp() {
        return SimulatorCase.condition("p's health > 10")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "cond: bare 'p health > 10' without possessive")
    public static SimulatorCase bareHealthCmp() {
        return SimulatorCase.condition("p health > 10")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("%p:PLAYER_POSSESSIVE% health %op:OP% %n:INT%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    /**
     * Buggy lock-in. {@code p is op} should match {@code OFFLINE_PLAYER is op} cleanly when p is
     * a PLAYER (PLAYER widens to OFFLINE_PLAYER). Sim reports a TypeMismatch.
     */
    @SimCase(name = "cond: p is op (BUG locked)")
    public static SimulatorCase isOp() {
        return SimulatorCase.condition("p is op")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("%op:OFFLINE_PLAYER% (is|is not) [a] op")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    /**
     * Buggy lock-in. Sim mismatches LOCATION as PLAYER on the inside-region pattern. Flip once
     * the right pattern is preferred.
     */
    @SimCase(name = "cond: location inside region (BUG locked)")
    public static SimulatorCase insideRegion() {
        return SimulatorCase.condition("loc is inside a to b")
                .env(EnvSimulator.create()
                        .withVar("loc", MinecraftTypes.LOCATION)
                        .withVar("a", MinecraftTypes.LOCATION)
                        .withVar("b", MinecraftTypes.LOCATION))
                .expectTopPattern("%who:PLAYER% (is|is not) inside %min:LOCATION% to %max:LOCATION%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "cond: 'inisde' typo for 'inside'")
    public static SimulatorCase insideTypo() {
        return SimulatorCase.condition("loc is inisde a to b")
                .env(EnvSimulator.create()
                        .withVar("loc", MinecraftTypes.LOCATION)
                        .withVar("a", MinecraftTypes.LOCATION)
                        .withVar("b", MinecraftTypes.LOCATION))
                .expectTopPattern("%loc:LOCATION% (is|is not) inside %min:LOCATION% to %max:LOCATION%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.475)
                .expectSuggestionCount(2, 2);
    }
}
