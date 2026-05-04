package dev.lumenlang.lumen.headless.sim;

import dev.lumenlang.lumen.headless.sim.base.HeadlessTestBase;
import dev.lumenlang.lumen.headless.sim.report.SimulatorReport;
import dev.lumenlang.lumen.headless.sim.runner.AnnotatedCaseRunner;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Stream;

/**
 * JUnit entry point that discovers every {@code @SimCase} method under
 * {@code dev.lumenlang.lumen.headless.sim.tests} and runs each as its own dynamic test.
 */
@ExtendWith(SimulatorReport.class)
public final class PatternSimulatorSuite extends HeadlessTestBase {

    @TestFactory
    public Stream<DynamicTest> simulator() {
        return AnnotatedCaseRunner.discover("dev.lumenlang.lumen.headless.sim.tests");
    }
}
