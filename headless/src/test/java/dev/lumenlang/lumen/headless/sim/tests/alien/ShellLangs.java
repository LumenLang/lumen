package dev.lumenlang.lumen.headless.sim.tests.alien;

import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Shell-style inputs: bash, zsh, PowerShell, Minecraft commands. Easy to confuse for Lumen
 * because verb-first syntax overlaps.
 */
@SimulatorTest
public final class ShellLangs {

    private ShellLangs() {
    }

    @SimCase(name = "bash: shebang")
    public static SimulatorCase shebang() {
        return SimulatorCase.statement("#!/bin/sh");
    }

    @SimCase(name = "bash: echo")
    public static SimulatorCase echo() {
        return SimulatorCase.statement("echo hello");
    }

    @SimCase(name = "bash: var assignment")
    public static SimulatorCase bashAssign() {
        return SimulatorCase.statement("count=5");
    }

    @SimCase(name = "bash: subshell")
    public static SimulatorCase bashSubshell() {
        return SimulatorCase.statement("name=$(whoami)");
    }

    @SimCase(name = "bash: pipe chain")
    public static SimulatorCase bashPipe() {
        return SimulatorCase.statement("ps aux | grep java | wc -l");
    }

    @SimCase(name = "bash: heredoc")
    public static SimulatorCase bashHeredoc() {
        return SimulatorCase.statement("cat <<EOF");
    }

    @SimCase(name = "powershell: variable")
    public static SimulatorCase powershellVar() {
        return SimulatorCase.statement("$count = 5");
    }

    @SimCase(name = "powershell: pipe to where")
    public static SimulatorCase powershellPipe() {
        return SimulatorCase.statement("Get-Player | Where-Object { $_.Health -gt 0 }");
    }

    @SimCase(name = "minecraft command: kill")
    public static SimulatorCase mcKill() {
        return SimulatorCase.statement("/kill @a");
    }

    @SimCase(name = "minecraft command: tp")
    public static SimulatorCase mcTp() {
        return SimulatorCase.statement("/tp @p ~ ~10 ~");
    }

    @SimCase(name = "minecraft command: give")
    public static SimulatorCase mcGive() {
        return SimulatorCase.statement("/give @p diamond 64");
    }

    @SimCase(name = "minecraft command: execute")
    public static SimulatorCase mcExecute() {
        return SimulatorCase.statement("/execute as @a at @s run kill");
    }

    @SimCase(name = "minecraft command: scoreboard")
    public static SimulatorCase mcScoreboard() {
        return SimulatorCase.statement("/scoreboard players add @p kills 1");
    }
}
