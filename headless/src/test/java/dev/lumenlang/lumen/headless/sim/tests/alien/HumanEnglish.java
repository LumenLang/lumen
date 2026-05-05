package dev.lumenlang.lumen.headless.sim.tests.alien;

import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;

/**
 * Inputs in plain English, full sentences, instructions to an LLM. Sim must not pretend any of
 * these are Lumen.
 */
@SimulatorTest
public final class HumanEnglish {

    private HumanEnglish() {
    }

    @SimCase(name = "english: please verb the noun")
    public static SimulatorCase pleaseVerb() {
        return SimulatorCase.statement("please kill the player named alice");
    }

    @SimCase(name = "english: i want to verb")
    public static SimulatorCase iWantTo() {
        return SimulatorCase.statement("I want to send a message to every player on the server");
    }

    @SimCase(name = "english: when X then Y")
    public static SimulatorCase whenThen() {
        return SimulatorCase.statement("when a player joins the server send them a welcome message");
    }

    @SimCase(name = "english: question")
    public static SimulatorCase question() {
        return SimulatorCase.statement("How do I give a player a diamond sword in Lumen?");
    }

    @SimCase(name = "english: instruction to llm")
    public static SimulatorCase llmInstruction() {
        return SimulatorCase.statement("write a script that heals the player on join");
    }

    @SimCase(name = "english: sentence with comma")
    public static SimulatorCase comma() {
        return SimulatorCase.statement("First, kill the player, then send a message");
    }

    @SimCase(name = "english: imperative paragraph")
    public static SimulatorCase imperative() {
        return SimulatorCase.statement("Take the player and remove all of their items immediately");
    }

    @SimCase(name = "english: vague request")
    public static SimulatorCase vague() {
        return SimulatorCase.statement("do something cool");
    }

    @SimCase(name = "english: punctuation chain")
    public static SimulatorCase punct() {
        return SimulatorCase.statement("kill, kill!! kill??");
    }

    @SimCase(name = "english: hello world")
    public static SimulatorCase helloWorld() {
        return SimulatorCase.statement("hello world");
    }
}
