package dev.lumenlang.lumen.headless.sim.tests.combo;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs that pile up many independent mistakes in a single statement: typos, swaps, extras,
 * missing tokens, and wrong types all at once.
 */
@SimulatorTest
public final class Pileup {

    private Pileup() {
    }

    @SimCase(name = "pileup: triple typo + swap + extras")
    public static SimulatorCase tripleTypoSwapExtras() {
        return SimulatorCase.statement("teleprt pls l t p extra junk")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER).withVar("l", MinecraftTypes.LOCATION));
    }

    @SimCase(name = "pileup: send title with everything broken")
    public static SimulatorCase sendTitleAllBroken() {
        return SimulatorCase.statement("sand titl with subitle hi sub p extra fade in 10")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "pileup: damage with typo, wrong type, swap, extras")
    public static SimulatorCase damageMassMistakes() {
        return SimulatorCase.statement("damge by \"five\" name extra now please")
                .env(EnvSimulator.create().withVar("name", PrimitiveType.STRING));
    }

    @SimCase(name = "pileup: set with garbage tokens between every part")
    public static SimulatorCase setGarbageBetween() {
        return SimulatorCase.statement("Set foo count bar to baz 5 qux")
                .env(EnvSimulator.create().withVar("count", PrimitiveType.INT));
    }

    @SimCase(name = "pileup: kill many wrong tokens trailing")
    public static SimulatorCase killNoise() {
        return SimulatorCase.statement("kil mob now then later please immediately")
                .env(EnvSimulator.create().withVar("mob", MinecraftTypes.LIVING_ENTITY));
    }

    @SimCase(name = "pileup: spawn with typo, swap, missing literal, extras")
    public static SimulatorCase spawnPiled() {
        return SimulatorCase.statement("spwan at l zomb extra now")
                .env(EnvSimulator.create().withVar("l", MinecraftTypes.LOCATION));
    }

    @SimCase(name = "pileup: every with typo, missing unit, extras")
    public static SimulatorCase everyPiled() {
        return SimulatorCase.block("ever 5 maybe please now");
    }

    @SimCase(name = "pileup: condition with typo, possessive missing, wrong op, extras")
    public static SimulatorCase condPiled() {
        return SimulatorCase.condition("p halth >> 10 maybe now")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "pileup: long sentence almost matching nothing")
    public static SimulatorCase nearGarbage() {
        return SimulatorCase.statement("the quick brown fox jumps over the lazy dog");
    }
}
