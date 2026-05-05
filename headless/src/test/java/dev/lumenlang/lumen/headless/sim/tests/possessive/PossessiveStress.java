package dev.lumenlang.lumen.headless.sim.tests.possessive;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs that exercise the possessive form: bare names where possessive expected, possessive
 * where non-possessive expected, broken apostrophes, and casing.
 */
@SimulatorTest
public final class PossessiveStress {

    private PossessiveStress() {
    }

    @SimCase(name = "possessive: missing apostrophe 'p s health'")
    public static SimulatorCase missingApostrophe() {
        return SimulatorCase.expression("get p s health")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "possessive: detached apostrophe 'p ' s'")
    public static SimulatorCase detachedApostrophe() {
        return SimulatorCase.expression("get p 's health")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "possessive: capital possessive 'P's'")
    public static SimulatorCase capitalPossessive() {
        return SimulatorCase.expression("get P's health")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "possessive: possessive on non-entity STRING var")
    public static SimulatorCase wrongTypePossessive() {
        return SimulatorCase.expression("get name's health")
                .env(EnvSimulator.create().withVar("name", PrimitiveType.STRING));
    }
}
