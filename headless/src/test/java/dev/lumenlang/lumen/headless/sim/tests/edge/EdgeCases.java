package dev.lumenlang.lumen.headless.sim.tests.edge;

import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs that stress empty, single-token, garbage, and casing extremes.
 */
@SimulatorTest
public final class EdgeCases {

    private EdgeCases() {
    }

    @SimCase(name = "edge: empty input")
    public static SimulatorCase empty() {
        return SimulatorCase.statement("");
    }

    @SimCase(name = "edge: whitespace-only input")
    public static SimulatorCase whitespaceOnly() {
        return SimulatorCase.statement("    ");
    }

    @SimCase(name = "edge: single unknown identifier")
    public static SimulatorCase unknownIdent() {
        return SimulatorCase.statement("xyzzy");
    }

    @SimCase(name = "edge: single number literal")
    public static SimulatorCase numberOnly() {
        return SimulatorCase.statement("42");
    }

    @SimCase(name = "edge: single string literal")
    public static SimulatorCase stringOnly() {
        return SimulatorCase.statement("\"hello\"");
    }

    @SimCase(name = "edge: lone punctuation")
    public static SimulatorCase lonePunct() {
        return SimulatorCase.statement(":");
    }

    @SimCase(name = "edge: repeated literal three times")
    public static SimulatorCase repeatedLiteral() {
        return SimulatorCase.statement("set set set");
    }

    @SimCase(name = "edge: ALL CAPS verb")
    public static SimulatorCase allCapsVerb() {
        return SimulatorCase.statement("KILL P");
    }

    @SimCase(name = "edge: mixed-case verb 'KiLl P'")
    public static SimulatorCase mixedCaseVerb() {
        return SimulatorCase.statement("KiLl P");
    }

    @SimCase(name = "edge: random symbols")
    public static SimulatorCase randomSymbols() {
        return SimulatorCase.statement("@#$ %^&");
    }
}
