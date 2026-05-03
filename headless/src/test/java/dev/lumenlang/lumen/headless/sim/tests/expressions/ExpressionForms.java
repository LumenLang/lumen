package dev.lumenlang.lumen.headless.sim.tests.expressions;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Expression-side inputs (getters, possessives, math/min/max, world lookup, clamp).
 */
@SimulatorTest
public final class ExpressionForms {

    private ExpressionForms() {
    }

    /**
     * Buggy lock-in. {@code get p's health} should match {@code get %ENTITY_POSSESSIVE% health}
     * cleanly. Sim picks the {@code max health} variant with a Reorder.
     */
    @SimCase(name = "expr: get p's health (BUG locked)")
    public static SimulatorCase possessiveHealth() {
        return SimulatorCase.expression("get p's health")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("get %e:ENTITY_POSSESSIVE% max health")
                .expectPrimaryIssue(SuggestionIssue.Reorder.class)
                .expectAnyIssue(SuggestionIssue.Reorder.class)
                .expectConfidenceAtLeast(1.0)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "expr: 'get p health' without possessive")
    public static SimulatorCase nonPossessiveHealth() {
        return SimulatorCase.expression("get p health")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("get %e:ENTITY_POSSESSIVE% health")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "expr: 'gat' typo for 'get'")
    public static SimulatorCase gatForGet() {
        return SimulatorCase.expression("gat p's health")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("get %e:ENTITY_POSSESSIVE% health")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }

    /**
     * Buggy lock-in. No {@code combined string of} pattern exists today; sim picks the LIST
     * indexer.
     */
    @SimCase(name = "expr: combined string of two strings (BUG locked)")
    public static SimulatorCase combinedString() {
        return SimulatorCase.expression("combined string of \"a\" and \"b\"")
                .expectTopPattern("%list:LIST% index of %val:EXPR%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.213)
                .expectSuggestionCount(1, 1);
    }

    /**
     * Buggy lock-in. {@code max of x and y} should match a math reducer; sim falls back to LIST
     * index of.
     */
    @SimCase(name = "expr: max of two numbers (BUG locked)")
    public static SimulatorCase maxOfTwo() {
        return SimulatorCase.expression("max of 5 and 10")
                .expectTopPattern("%list:LIST% index of %val:EXPR%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.28)
                .expectSuggestionCount(1, 1);
    }

    /**
     * Buggy lock-in. Same root cause as {@link #maxOfTwo()} with a typo on the verb.
     */
    @SimCase(name = "expr: 'maks' typo for 'max' (BUG locked)")
    public static SimulatorCase maksForMax() {
        return SimulatorCase.expression("maks of 5 and 10")
                .expectTopPattern("%list:LIST% index of %val:EXPR%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.28)
                .expectSuggestionCount(1, 1);
    }

    /**
     * Buggy lock-in. {@code get loc world} should match a possessive world getter, sim picks the
     * server-wide world-by-name lookup.
     */
    @SimCase(name = "expr: get world of location (BUG locked)")
    public static SimulatorCase getWorld() {
        return SimulatorCase.expression("get loc world")
                .env(EnvSimulator.create().withVar("loc", MinecraftTypes.LOCATION))
                .expectTopPattern("get world %name:STRING%")
                .expectPrimaryIssue(SuggestionIssue.Reorder.class)
                .expectAnyIssue(SuggestionIssue.Reorder.class)
                .expectConfidenceAtLeast(1.0)
                .expectSuggestionCount(2, 2);
    }

    /**
     * Buggy lock-in. {@code clamp x between 0 and 10} has no clamp pattern today; sim falls back
     * to a coordinate getter.
     */
    @SimCase(name = "expr: clamp number (BUG locked)")
    public static SimulatorCase clampNumber() {
        return SimulatorCase.expression("clamp x between 0 and 10")
                .env(EnvSimulator.create().withVar("x", PrimitiveType.INT))
                .expectTopPattern("[get] %b:BLOCK% (x|y|z)")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.ExtraTokens.class)
                .expectConfidenceAtLeast(0.38)
                .expectSuggestionCount(1, 1);
    }
}
