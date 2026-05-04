package dev.lumenlang.lumen.headless.sim.tests.typo;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;

/**
 * One mistyped keyword in an otherwise well-formed statement.
 */
@SimulatorTest
public final class SingleTokenTypos {

    private SingleTokenTypos() {
    }

    @SimCase(name = "typo: 'st' for 'set'")
    public static SimulatorCase stForSet() {
        return SimulatorCase.statement("st x to 5")
                .expectTopPattern("set %name:IDENT% to %val:EXPR%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }

    /**
     * Buggy lock-in. {@code t} → {@code to} should keep us on the {@code set} pattern; sim drifts
     * to a location coord getter.
     */
    @SimCase(name = "typo: 't' for 'to' (BUG locked)")
    public static SimulatorCase tForTo() {
        return SimulatorCase.statement("set x t 5")
                .expectTopPattern("get %loc:LOCATION% (x|y|z|yaw|pitch)")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.TypeMismatch.class)
                .expectConfidenceAtLeast(0.63)
                .expectSuggestionCount(2, 2);
    }

    /**
     * Buggy lock-in. {@code teleprt} should keep the LOCATION shape, but sim picks the PLAYER one.
     */
    @SimCase(name = "typo: 'teleprt' for 'teleport' (BUG locked)")
    public static SimulatorCase teleprtForTeleport() {
        return SimulatorCase.statement("teleprt p to l")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER).withVar("l", MinecraftTypes.LOCATION))
                .expectTopPattern("(teleport|tp) %who:PLAYER% [to] %loc:LOCATION%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "typo: 'giv' for 'give'")
    public static SimulatorCase givForGive() {
        return SimulatorCase.statement("giv p diamond")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("give %who:PLAYER% %item:ITEM%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "typo: 'brodcast' for 'broadcast'")
    public static SimulatorCase brodcastForBroadcast() {
        return SimulatorCase.statement("brodcast \"hi\"")
                .expectTopPattern("(broadcast|announce) %text:STRING%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(1, 1);
    }

    @SimCase(name = "typo: 'kil' for 'kill'")
    public static SimulatorCase kilForKill() {
        return SimulatorCase.statement("kil p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("(kill|slay) %who:PLAYER%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "typo: 'sand' for 'send' before title")
    public static SimulatorCase sandForSend() {
        return SimulatorCase.statement("sand title \"hi\" to p")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("send title %title:STRING% to %who:PLAYER%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "typo: 'massage' for 'message'")
    public static SimulatorCase massageForMessage() {
        return SimulatorCase.statement("massage p \"hello\"")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("message %who:PLAYER% %text:STRING%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(1, 1);
    }

    @SimCase(name = "typo: 'spwan' for 'spawn'")
    public static SimulatorCase spwanForSpawn() {
        return SimulatorCase.statement("spwan zombie at l")
                .env(EnvSimulator.create().withVar("l", MinecraftTypes.LOCATION))
                .expectTopPattern("spawn %type:ENTITY_TYPE% [at] %loc:LOCATION%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }

    @SimCase(name = "typo: 'damge' for 'damage'")
    public static SimulatorCase damgeForDamage() {
        return SimulatorCase.statement("damge p by 5")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER))
                .expectTopPattern("damage %e:LIVING_ENTITY% [by] %val:INT%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(1, 1);
    }

    @SimCase(name = "typo: 'remov' for 'remove'")
    public static SimulatorCase removForRemove() {
        return SimulatorCase.statement("remov mob")
                .env(EnvSimulator.create().withVar("mob", MinecraftTypes.LIVING_ENTITY))
                .expectTopPattern("remove %e:ENTITY%")
                .expectPrimaryIssue(SuggestionIssue.Typo.class)
                .expectAnyIssue(SuggestionIssue.Typo.class)
                .expectConfidenceAtLeast(0.95)
                .expectSuggestionCount(2, 2);
    }
}
