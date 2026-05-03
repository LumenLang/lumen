package dev.lumenlang.lumen.headless.sim.tests.typo;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Typos mixed with whitespace, casing, digit-substitution, and arg-keyword variants. Stress
 * cases for the typo scorer.
 */
@SimulatorTest
public final class CornerCaseTypos {

    private CornerCaseTypos() {
    }

    @SimCase(name = "block typo: 'reapt' for 'repeat'")
    public static SimulatorCase reaptForRepeat() {
        return SimulatorCase.block("reapt 5 times")
                .expectTopPattern("repeat %n:INT% [time|times]")
                .expectPrimaryIssue(SuggestionIssue.ExtraTokens.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.ExtraTokens.class)
                .expectConfidenceAtLeast(0.87)
                .expectSuggestionCount(1, 1);
    }

    /**
     * Buggy lock-in. Triple whitespace shouldn't change matching, but sim drifts to the location
     * coord getter (same root cause as {@code tForTo}).
     */
    @SimCase(name = "typo: triple whitespace 'set    x   t   5' (BUG locked)")
    public static SimulatorCase tripleSpace() {
        return SimulatorCase.statement("set    x   t   5")
                .expectTopPattern("get %loc:LOCATION% (x|y|z|yaw|pitch)")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.63)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "typo: capitalised 'Set x to 5'")
    public static SimulatorCase capitalised() {
        return SimulatorCase.statement("Set x to 5")
                .expectCleanTop("set %name:IDENT% to %val:EXPR%");
    }

    @SimCase(name = "typo: digit substitution 'set x t0 5'")
    public static SimulatorCase digitInTo() {
        return SimulatorCase.statement("set x t0 5")
                .expectTopPattern("set %name:IDENT% to %val:EXPR%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "typo: arg keyword 'send actionber \"hi\" to p'")
    public static SimulatorCase actionberForActionbar() {
        return SimulatorCase.statement("send actionber \"hi\" to p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }
}
