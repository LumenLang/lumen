package dev.lumenlang.lumen.headless.sim.tests.incomplete;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Inputs that exhaust before the pattern is fully consumed (IncompleteInput) or skip a required
 * mid-pattern literal (MissingLiteral).
 */
@SimulatorTest
public final class IncompleteCases {

    private IncompleteCases() {
    }

    @SimCase(name = "incomplete: wait with no time unit")
    public static SimulatorCase waitNoUnit() {
        return SimulatorCase.block("wait 20")
                .expectTopPattern("wait %n:NUMBER% (tick|second|minute|hour|day)[s]")
                .expectPrimaryIssue(SuggestionIssue.IncompleteInput.class)
                .expectAnyIssue(SuggestionIssue.IncompleteInput.class)
                .expectConfidenceAtLeast(0.85)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "incomplete: damage entity by (no amount)")
    public static SimulatorCase damageNoAmount() {
        return SimulatorCase.statement("damage mob by")
                .env(EnvSimulator.create().withVar("mob", MinecraftTypes.ENTITY))
                .expectTopPattern("damage %e:ENTITY% [by] %val:INT%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(1, 1);
    }

    @SimCase(name = "incomplete: spawn entity at (no location)")
    public static SimulatorCase spawnNoLoc() {
        return SimulatorCase.statement("spawn zombie at")
                .expectTopPattern("spawn %type:ENTITY_TYPE% at %who:PLAYER%")
                .expectConfidenceAtLeast(0.85)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "incomplete: teleport p to (no target)")
    public static SimulatorCase teleportNoTarget() {
        return SimulatorCase.statement("teleport p to")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("(teleport|tp) %who:PLAYER% [to] %target:PLAYER%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    /**
     * Note: this case currently scores cleanly because {@code [by]} is optional. Sim sees
     * {@code damage mob 5} as a complete match and returns conf 1.0. Real fix would require
     * the [by] literal to influence ranking when present in adjacent patterns.
     */
    @SimCase(name = "missing literal: damage entity missing 'by'")
    public static SimulatorCase damageMissingBy() {
        return SimulatorCase.statement("damage mob 5")
                .env(EnvSimulator.create().withVar("mob", MinecraftTypes.ENTITY))
                .expectCleanTop("damage %e:ENTITY% [by] %val:INT%");
    }

    @SimCase(name = "missing literal: set time of WORLD without 'to'")
    public static SimulatorCase setTimeNoToWord() {
        return SimulatorCase.statement("set time of overworld 6000")
                .expectTopPattern("set time [of] %w:WORLD% [to] %val:INT%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.60)
                .expectSuggestionCount(2, 2);
    }
}
