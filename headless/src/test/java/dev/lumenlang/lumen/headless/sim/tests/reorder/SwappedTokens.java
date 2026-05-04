package dev.lumenlang.lumen.headless.sim.tests.reorder;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * Inputs whose tokens are all present but in the wrong order.
 */
@SimulatorTest
public final class SwappedTokens {

    private SwappedTokens() {
    }

    @SimCase(name = "swap: message arguments swapped")
    public static SimulatorCase messageArgs() {
        return SimulatorCase.statement("message \"hello\" p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectAnySuggestions();
    }

    /**
     * Buggy lock-in. {@code send title to p "hi"} has all tokens in the wrong order; sim should
     * surface a Reorder issue. Currently sim emits MissingLiteral because the reorder fallback
     * does not fire here. Flip once reorder detection covers this shape.
     */
    @SimCase(name = "swap: send title recipient before string (BUG locked)")
    public static SimulatorCase sendToBeforeString() {
        return SimulatorCase.statement("send title to p \"hi\"")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("send title %title:STRING% to %who:PLAYER%")
                .expectPrimaryIssue(SuggestionIssue.MissingLiteral.class)
                .expectAnyIssue(SuggestionIssue.MissingLiteral.class)
                .expectConfidenceAtLeast(0.40)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "swap: damage 'by amount' before target")
    public static SimulatorCase damageAmountFirst() {
        return SimulatorCase.statement("damage by 5 mob")
                .env(EnvSimulator.create().withVar("mob", MinecraftTypes.ENTITY))
                .expectTopPattern("damage %e:ENTITY% [by] %val:INT%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.ExtraTokens.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(1, 1);
    }

    @SimCase(name = "swap: teleport target before player")
    public static SimulatorCase teleportTargetFirst() {
        return SimulatorCase.statement("teleport l to p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER).withVar("l", MinecraftTypes.LOCATION))
                .expectTopPattern("(teleport|tp) %who:PLAYER% [to] %target:PLAYER%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "swap: spawn type after location")
    public static SimulatorCase spawnTypeLast() {
        return SimulatorCase.statement("spawn at l zombie")
                .env(EnvSimulator.create().withVar("l", MinecraftTypes.LOCATION))
                .expectTopPattern("spawn %type:ENTITY_TYPE% [at] %loc:LOCATION%")
                .expectPrimaryIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectAnyIssue(SuggestionIssue.ExtraTokens.class)
                .expectConfidenceAtLeast(0.73)
                .expectSuggestionCount(2, 2);
    }
}
