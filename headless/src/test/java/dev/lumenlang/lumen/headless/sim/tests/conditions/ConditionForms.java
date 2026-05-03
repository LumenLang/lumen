package dev.lumenlang.lumen.headless.sim.tests.conditions;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Condition-side inputs (permission checks, comparisons, region checks).
 */
@SimulatorTest
public final class ConditionForms {

    private ConditionForms() {
    }

    @SimCase(name = "cond: player has permission")
    public static SimulatorCase hasPermission() {
        return SimulatorCase.condition("p has permission \"command.use\"")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectCleanTop("%p:PLAYER% has permission %perm:STRING%");
    }

    @SimCase(name = "cond: 'permision' typo")
    public static SimulatorCase permisionTypo() {
        return SimulatorCase.condition("p has permision \"command.use\"")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("%p:PLAYER% has permission %perm:STRING%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "cond: p's health > 10")
    public static SimulatorCase possessiveHealthCmp() {
        return SimulatorCase.condition("p's health > 10")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectCleanTop("%p:PLAYER_POSSESSIVE% health %op:OP% %n:INT%");
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

    @SimCase(name = "cond: p is op")
    public static SimulatorCase isOp() {
        return SimulatorCase.condition("p is op")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectCleanTop("%p:PLAYER% (is|is not) [a] op");
    }

    @SimCase(name = "cond: location inside region")
    public static SimulatorCase insideRegion() {
        return SimulatorCase.condition("loc is inside a to b")
                .env(EnvSimulator.create()
                        .withVar("loc", MinecraftTypes.LOCATION)
                        .withVar("a", MinecraftTypes.LOCATION)
                        .withVar("b", MinecraftTypes.LOCATION))
                .expectCleanTop("%loc:LOCATION% (is|is not) inside %min:LOCATION% to %max:LOCATION%");
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
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }
}
