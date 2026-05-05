package dev.lumenlang.lumen.headless.sim.tests.alien;

import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs in JS, Python, Lua, Ruby, Perl, PHP. AI agents commonly emit these against Lumen and
 * the simulator should not pretend they are valid.
 */
@SimulatorTest
public final class ScriptingLangs {

    private ScriptingLangs() {
    }

    @SimCase(name = "js: dot method call")
    public static SimulatorCase jsMethodCall() {
        return SimulatorCase.statement("player.kill()");
    }

    @SimCase(name = "js: chained method call")
    public static SimulatorCase jsChained() {
        return SimulatorCase.statement("player.getInventory().clear()");
    }

    @SimCase(name = "js: property assignment")
    public static SimulatorCase jsPropertyAssign() {
        return SimulatorCase.statement("player.health = 20");
    }

    @SimCase(name = "js: const declaration")
    public static SimulatorCase jsConst() {
        return SimulatorCase.statement("const target = bukkit.getPlayer(\"alice\");");
    }

    @SimCase(name = "js: arrow function")
    public static SimulatorCase jsArrow() {
        return SimulatorCase.statement("const heal = (p) => p.setHealth(20);");
    }

    @SimCase(name = "js: template literal")
    public static SimulatorCase jsTemplate() {
        return SimulatorCase.statement("send(`hello ${player.name}`)");
    }

    @SimCase(name = "js: console.log")
    public static SimulatorCase jsConsoleLog() {
        return SimulatorCase.statement("console.log(\"hello\")");
    }

    @SimCase(name = "python: function call")
    public static SimulatorCase pyCall() {
        return SimulatorCase.statement("kill(player)");
    }

    @SimCase(name = "python: equals assignment")
    public static SimulatorCase pyAssign() {
        return SimulatorCase.statement("count = 5");
    }

    @SimCase(name = "python: f-string")
    public static SimulatorCase pyFstring() {
        return SimulatorCase.statement("send(f\"hello {player.name}\")");
    }

    @SimCase(name = "python: def keyword")
    public static SimulatorCase pyDef() {
        return SimulatorCase.statement("def heal_player(p):");
    }

    @SimCase(name = "python: import statement")
    public static SimulatorCase pyImport() {
        return SimulatorCase.statement("from bukkit import Player");
    }

    @SimCase(name = "python: list comprehension")
    public static SimulatorCase pyListComp() {
        return SimulatorCase.statement("alive = [p for p in players if p.health > 0]");
    }

    @SimCase(name = "lua: local declaration")
    public static SimulatorCase luaLocal() {
        return SimulatorCase.statement("local count = 5");
    }

    @SimCase(name = "lua: function definition")
    public static SimulatorCase luaFunction() {
        return SimulatorCase.statement("function heal(p) p:setHealth(20) end");
    }

    @SimCase(name = "lua: nil check")
    public static SimulatorCase luaNil() {
        return SimulatorCase.condition("if player == nil then");
    }

    @SimCase(name = "ruby: do block")
    public static SimulatorCase rubyDo() {
        return SimulatorCase.block("players.each do |p|");
    }

    @SimCase(name = "ruby: symbol literal")
    public static SimulatorCase rubySymbol() {
        return SimulatorCase.statement("player.send :hello");
    }

    @SimCase(name = "ruby: instance variable")
    public static SimulatorCase rubyIvar() {
        return SimulatorCase.statement("@count = 5");
    }

    @SimCase(name = "perl: scalar variable")
    public static SimulatorCase perlScalar() {
        return SimulatorCase.statement("$count = 5;");
    }

    @SimCase(name = "perl: print with newline")
    public static SimulatorCase perlPrint() {
        return SimulatorCase.statement("print \"hello\\n\";");
    }

    @SimCase(name = "php: variable assignment")
    public static SimulatorCase phpAssign() {
        return SimulatorCase.statement("$player->setHealth(20);");
    }
}
