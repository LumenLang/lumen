package dev.lumenlang.lumen.headless.sim.tests.alien;

import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs in Java, Kotlin, Rust, C, C++, Go, Swift. None of these are valid Lumen.
 */
@SimulatorTest
public final class SystemsLangs {

    private SystemsLangs() {
    }

    @SimCase(name = "java: typed declaration")
    public static SimulatorCase javaDecl() {
        return SimulatorCase.statement("Player p = bukkit.getPlayer(\"alice\");");
    }

    @SimCase(name = "java: instanceof check")
    public static SimulatorCase javaInstanceof() {
        return SimulatorCase.condition("entity instanceof LivingEntity");
    }

    @SimCase(name = "java: try with resources")
    public static SimulatorCase javaTryWith() {
        return SimulatorCase.block("try (var p = bukkit.getPlayer()) {");
    }

    @SimCase(name = "java: for-each loop")
    public static SimulatorCase javaForEach() {
        return SimulatorCase.block("for (Player p : bukkit.getOnlinePlayers()) {");
    }

    @SimCase(name = "kotlin: val declaration")
    public static SimulatorCase kotlinVal() {
        return SimulatorCase.statement("val count: Int = 5");
    }

    @SimCase(name = "kotlin: lambda with arrow")
    public static SimulatorCase kotlinLambda() {
        return SimulatorCase.statement("players.forEach { it.kill() }");
    }

    @SimCase(name = "kotlin: when expression")
    public static SimulatorCase kotlinWhen() {
        return SimulatorCase.block("when (player.health) {");
    }

    @SimCase(name = "rust: let binding with type")
    public static SimulatorCase rustLet() {
        return SimulatorCase.statement("let count: i32 = 5;");
    }

    @SimCase(name = "rust: mut binding")
    public static SimulatorCase rustMut() {
        return SimulatorCase.statement("let mut player = bukkit.player();");
    }

    @SimCase(name = "rust: match arm")
    public static SimulatorCase rustMatch() {
        return SimulatorCase.block("match player.health() {");
    }

    @SimCase(name = "rust: function definition")
    public static SimulatorCase rustFn() {
        return SimulatorCase.statement("fn heal(p: &mut Player) { p.set_health(20); }");
    }

    @SimCase(name = "c: variable declaration")
    public static SimulatorCase cDecl() {
        return SimulatorCase.statement("int count = 5;");
    }

    @SimCase(name = "c: pointer dereference")
    public static SimulatorCase cPointer() {
        return SimulatorCase.statement("(*player).health = 20;");
    }

    @SimCase(name = "c++: namespace scope")
    public static SimulatorCase cppNamespace() {
        return SimulatorCase.statement("bukkit::server.kill(player);");
    }

    @SimCase(name = "c++: template type")
    public static SimulatorCase cppTemplate() {
        return SimulatorCase.statement("std::vector<Player> players;");
    }

    @SimCase(name = "go: short var assignment")
    public static SimulatorCase goShortAssign() {
        return SimulatorCase.statement("count := 5");
    }

    @SimCase(name = "go: function definition")
    public static SimulatorCase goFunc() {
        return SimulatorCase.statement("func heal(p *Player) { p.SetHealth(20) }");
    }

    @SimCase(name = "swift: var declaration")
    public static SimulatorCase swiftVar() {
        return SimulatorCase.statement("var count: Int = 5");
    }

    @SimCase(name = "swift: optional unwrap")
    public static SimulatorCase swiftOptional() {
        return SimulatorCase.statement("let p = player?.target");
    }
}
