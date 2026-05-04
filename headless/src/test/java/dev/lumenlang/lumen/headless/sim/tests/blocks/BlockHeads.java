package dev.lumenlang.lumen.headless.sim.tests.blocks;

import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Block-head inputs: {@code repeat}, {@code every}, {@code wait}, {@code load}, {@code preload}.
 */
@SimulatorTest
public final class BlockHeads {

    private BlockHeads() {
    }

    @SimCase(name = "block: repeat N times")
    public static SimulatorCase repeat() {
        return SimulatorCase.block("repeat 5 times")
                .expectCleanTop("repeat %n:INT% [time|times]");
    }

    @SimCase(name = "block: every N seconds")
    public static SimulatorCase every() {
        return SimulatorCase.block("every 5 seconds")
                .expectCleanTop("every %n:NUMBER% (tick|second|minute|hour|day)[s]");
    }

    @SimCase(name = "block: wait N ticks")
    public static SimulatorCase waitTicks() {
        return SimulatorCase.block("wait 20 ticks")
                .expectCleanTop("wait %n:NUMBER% (tick|second|minute|hour|day)[s]");
    }

    @SimCase(name = "block: every with no unit")
    public static SimulatorCase everyNoUnit() {
        return SimulatorCase.block("every 5")
                .expectTopPattern("every %n:NUMBER% (tick|second|minute|hour|day)[s]")
                .expectPrimaryIssue(SuggestionIssue.IncompleteInput.class)
                .expectAnyIssue(SuggestionIssue.IncompleteInput.class)
                .expectConfidenceAtLeast(0.85)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "block: 'ever' typo for 'every'")
    public static SimulatorCase everForEvery() {
        return SimulatorCase.block("ever 5 seconds")
                .expectTopPattern("every %n:NUMBER% (tick|second|minute|hour|day)[s]")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "block: 'wat' typo for 'wait'")
    public static SimulatorCase watForWait() {
        return SimulatorCase.block("wat 20 ticks")
                .expectTopPattern("wait %n:NUMBER% (tick|second|minute|hour|day)[s]")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "block: load")
    public static SimulatorCase load() {
        return SimulatorCase.block("load")
                .expectCleanTop("load");
    }

    @SimCase(name = "block: preload")
    public static SimulatorCase preload() {
        return SimulatorCase.block("preload")
                .expectCleanTop("preload");
    }
}
