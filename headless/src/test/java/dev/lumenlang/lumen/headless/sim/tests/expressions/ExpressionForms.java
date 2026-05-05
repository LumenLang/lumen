package dev.lumenlang.lumen.headless.sim.tests.expressions;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Expression-side inputs (getters, possessives, math/min/max, world lookup, clamp).
 */
@SimulatorTest
public final class ExpressionForms {

    private ExpressionForms() {
    }

    @SimCase(name = "expr: get p's health")
    public static SimulatorCase possessiveHealth() {
        return SimulatorCase.expression("get p's health")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "expr: 'get p health' without possessive")
    public static SimulatorCase nonPossessiveHealth() {
        return SimulatorCase.expression("get p health")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "expr: 'gat' typo for 'get'")
    public static SimulatorCase gatForGet() {
        return SimulatorCase.expression("gat p's health")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "expr: combined string of two strings")
    public static SimulatorCase combinedString() {
        return SimulatorCase.expression("combined string of \"a\" and \"b\"");
    }

    @SimCase(name = "expr: max of two numbers")
    public static SimulatorCase maxOfTwo() {
        return SimulatorCase.expression("max of 5 and 10");
    }

    /**
     * Buggy lock-in. Sim returns no suggestions because {@code maks -> max} is a 2-edit typo and
     * any threshold loose enough to accept it also accepts unrelated keyword pairs like
     * {@code send -> set}. Flip once the typo path can distinguish phonetic typos from
     * neighbouring real keywords.
     */
    @SimCase(name = "expr: 'maks' typo for 'max' (BUG locked)")
    public static SimulatorCase maksForMax() {
        return SimulatorCase.expression("maks of 5 and 10");
    }

    @SimCase(name = "expr: get world of location")
    public static SimulatorCase getWorld() {
        return SimulatorCase.expression("get loc world")
                .env(EnvSimulator.create().withVar("loc", MinecraftTypes.LOCATION));
    }

    @SimCase(name = "expr: clamp number")
    public static SimulatorCase clampNumber() {
        return SimulatorCase.expression("clamp x between 0 and 10")
                .env(EnvSimulator.create().withVar("x", PrimitiveType.INT));
    }
}
