package dev.lumenlang.lumen.headless.sim.tests.extra;

import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.EnvSimulator;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs that contain a complete pattern plus extra tokens (leading, trailing, or numeric).
 */
@SimulatorTest
public final class ExtraTokens {

    private ExtraTokens() {
    }

    @SimCase(name = "extra: trailing junk after send title")
    public static SimulatorCase trailingJunk() {
        return SimulatorCase.statement("send title \"hi\" to p extra junk")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "extra: extra word after kill")
    public static SimulatorCase afterKill() {
        return SimulatorCase.statement("kill p now")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }

    @SimCase(name = "extra: many trailing words after broadcast")
    public static SimulatorCase manyTrailing() {
        return SimulatorCase.statement("broadcast \"hi\" everyone now please")
                .env(EnvSimulator.empty());
    }

    @SimCase(name = "extra: leading word before broadcast")
    public static SimulatorCase leadingWord() {
        return SimulatorCase.statement("please broadcast \"hi\"");
    }

    @SimCase(name = "extra: numeric trailing token after message")
    public static SimulatorCase trailingNumber() {
        return SimulatorCase.statement("message p \"hello\" 42")
                .env(EnvSimulator.create().withVar("p", MinecraftTypes.PLAYER));
    }
}
