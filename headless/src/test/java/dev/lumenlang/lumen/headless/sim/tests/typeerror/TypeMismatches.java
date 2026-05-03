package dev.lumenlang.lumen.headless.sim.tests.typeerror;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Inputs whose argument types fail the binding the matching pattern asks for.
 */
@SimulatorTest
public final class TypeMismatches {

    private TypeMismatches() {
    }

    @SimCase(name = "type: assign string to INT var")
    public static SimulatorCase intGotString() {
        return SimulatorCase.statement("set count to \"hi\"")
                .env(EnvSimulator.create().withVar("count", PrimitiveType.INT).withVar("p", MinecraftTypes.PLAYER))
                .expectAnySuggestions();
    }

    @SimCase(name = "type: damage by string amount")
    public static SimulatorCase damageStringAmount() {
        return SimulatorCase.statement("damage p by \"five\"")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("damage %e:ENTITY% [by] %val:INT%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "type: teleport player to int")
    public static SimulatorCase teleportToInt() {
        return SimulatorCase.statement("teleport p to 5")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("(teleport|tp) %who:PLAYER% [to] %target:PLAYER%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "type: give to non-player target")
    public static SimulatorCase giveToString() {
        return SimulatorCase.statement("give \"alice\" diamond")
                .expectTopPattern("give %who:PLAYER% %item:ITEM%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "type: kill a string literal")
    public static SimulatorCase killString() {
        return SimulatorCase.statement("kill \"alice\"")
                .expectTopPattern("(kill|slay) %who:PLAYER%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "type: spawn unknown entity type ident")
    public static SimulatorCase spawnUnknownType() {
        return SimulatorCase.statement("spawn glowsquidd at l")
                .env(EnvSimulator.create().withVar("l", MinecraftTypes.LOCATION))
                .expectTopPattern("spawn %type:ENTITY_TYPE% [at] %loc:LOCATION%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "type: set STRING var with int")
    public static SimulatorCase stringGotInt() {
        return SimulatorCase.statement("set name to 5")
                .env(EnvSimulator.create().withVar("name", PrimitiveType.STRING))
                .expectTopPattern("set %name:IDENT% to %val:EXPR%")
                .expectPrimaryIssue(SuggestionIssue.HandlerDiagnostic.class)
                .expectAnyIssue(SuggestionIssue.HandlerDiagnostic.class)
                .expectConfidenceAtLeast(0.75)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "type: set INT var with double")
    public static SimulatorCase intGotDouble() {
        return SimulatorCase.statement("set count to 1.5")
                .env(EnvSimulator.create().withVar("count", PrimitiveType.INT))
                .expectTopPattern("set %name:IDENT% to %val:EXPR%")
                .expectPrimaryIssue(SuggestionIssue.HandlerDiagnostic.class)
                .expectAnyIssue(SuggestionIssue.HandlerDiagnostic.class)
                .expectConfidenceAtLeast(0.75)
                .expectSuggestionCount(2, 2);
    }
}
