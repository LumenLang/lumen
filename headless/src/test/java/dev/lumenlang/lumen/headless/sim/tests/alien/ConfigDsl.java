package dev.lumenlang.lumen.headless.sim.tests.alien;

import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs from config DSLs and lightweight markup: HOCON, INI, .env, Caddyfile, Dockerfile,
 * Makefile, Markdown.
 */
@SimulatorTest
public final class ConfigDsl {

    private ConfigDsl() {
    }

    @SimCase(name = "ini: section")
    public static SimulatorCase iniSection() {
        return SimulatorCase.statement("[player.config]");
    }

    @SimCase(name = "ini: key value")
    public static SimulatorCase iniPair() {
        return SimulatorCase.statement("health=20");
    }

    @SimCase(name = "env: shell-style")
    public static SimulatorCase envShell() {
        return SimulatorCase.statement("LUMEN_DEBUG=true");
    }

    @SimCase(name = "hocon: nested key")
    public static SimulatorCase hocon() {
        return SimulatorCase.statement("player { health = 20, name = \"alice\" }");
    }

    @SimCase(name = "caddyfile: route")
    public static SimulatorCase caddyfile() {
        return SimulatorCase.statement("respond /heal 200");
    }

    @SimCase(name = "dockerfile: from")
    public static SimulatorCase dockerFrom() {
        return SimulatorCase.statement("FROM openjdk:17-jdk");
    }

    @SimCase(name = "dockerfile: run")
    public static SimulatorCase dockerRun() {
        return SimulatorCase.statement("RUN apt-get update && apt-get install -y curl");
    }

    @SimCase(name = "makefile: rule")
    public static SimulatorCase makefile() {
        return SimulatorCase.statement("build: src/main.luma");
    }

    @SimCase(name = "markdown: heading")
    public static SimulatorCase mdHeading() {
        return SimulatorCase.statement("# Player heal script");
    }

    @SimCase(name = "markdown: list bullet")
    public static SimulatorCase mdBullet() {
        return SimulatorCase.statement("* heal the player");
    }
}
