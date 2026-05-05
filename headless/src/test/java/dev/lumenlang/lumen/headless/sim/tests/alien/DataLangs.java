package dev.lumenlang.lumen.headless.sim.tests.alien;

import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs in declarative or query languages: SQL, YAML, JSON, TOML, GraphQL, regex. None are
 * valid Lumen.
 */
@SimulatorTest
public final class DataLangs {

    private DataLangs() {
    }

    @SimCase(name = "sql: select")
    public static SimulatorCase sqlSelect() {
        return SimulatorCase.statement("SELECT health FROM players WHERE name = \"alice\"");
    }

    @SimCase(name = "sql: insert")
    public static SimulatorCase sqlInsert() {
        return SimulatorCase.statement("INSERT INTO players (name, health) VALUES (\"alice\", 20)");
    }

    @SimCase(name = "sql: update")
    public static SimulatorCase sqlUpdate() {
        return SimulatorCase.statement("UPDATE players SET health = 20 WHERE name = \"alice\"");
    }

    @SimCase(name = "sql: delete")
    public static SimulatorCase sqlDelete() {
        return SimulatorCase.statement("DELETE FROM players WHERE health <= 0");
    }

    @SimCase(name = "yaml: simple key value")
    public static SimulatorCase yamlPair() {
        return SimulatorCase.statement("name: alice");
    }

    @SimCase(name = "yaml: nested mapping")
    public static SimulatorCase yamlNested() {
        return SimulatorCase.statement("player:\n  name: alice\n  health: 20");
    }

    @SimCase(name = "yaml: list bullet")
    public static SimulatorCase yamlList() {
        return SimulatorCase.statement("- alice");
    }

    @SimCase(name = "json: object")
    public static SimulatorCase jsonObject() {
        return SimulatorCase.statement("{ \"name\": \"alice\", \"health\": 20 }");
    }

    @SimCase(name = "json: array")
    public static SimulatorCase jsonArray() {
        return SimulatorCase.statement("[\"alice\", \"bob\", \"charlie\"]");
    }

    @SimCase(name = "toml: section header")
    public static SimulatorCase tomlSection() {
        return SimulatorCase.statement("[player.config]");
    }

    @SimCase(name = "toml: key value")
    public static SimulatorCase tomlPair() {
        return SimulatorCase.statement("health = 20 # default");
    }

    @SimCase(name = "graphql: query")
    public static SimulatorCase graphqlQuery() {
        return SimulatorCase.statement("query { player(name: \"alice\") { health } }");
    }

    @SimCase(name = "regex: anchored pattern")
    public static SimulatorCase regexAnchored() {
        return SimulatorCase.statement("^player_[a-z]+$");
    }

    @SimCase(name = "xml: tag")
    public static SimulatorCase xmlTag() {
        return SimulatorCase.statement("<player name=\"alice\" health=\"20\" />");
    }

    @SimCase(name = "css: rule")
    public static SimulatorCase cssRule() {
        return SimulatorCase.statement(".player { color: red; }");
    }
}
