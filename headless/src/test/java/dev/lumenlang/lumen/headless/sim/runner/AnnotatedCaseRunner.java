package dev.lumenlang.lumen.headless.sim.runner;

import dev.lumenlang.lumen.headless.sim.annotations.SimCase;
import dev.lumenlang.lumen.headless.sim.annotations.SimulatorTest;
import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DynamicTest;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Discovers {@link SimulatorTest}-annotated classes under a base package and yields one
 * {@link DynamicTest} per {@link SimCase}-annotated static method.
 */
public final class AnnotatedCaseRunner {

    private AnnotatedCaseRunner() {
    }

    /**
     * All {@link DynamicTest}s collected from the base package, sorted by class then method name.
     *
     * @param basePackage dotted package to scan, e.g. {@code "dev.lumenlang.lumen.headless.sim.tests"}
     */
    public static @NotNull Stream<DynamicTest> discover(@NotNull String basePackage) {
        List<Class<?>> containers = scanContainers(basePackage);
        containers.sort(Comparator.comparing(Class::getName));
        List<DynamicTest> tests = new ArrayList<>();
        for (Class<?> container : containers) {
            Method[] methods = container.getDeclaredMethods();
            List<Method> annotated = new ArrayList<>();
            for (Method method : methods) {
                if (method.isAnnotationPresent(SimCase.class)) annotated.add(method);
            }
            annotated.sort(Comparator.comparing(Method::getName));
            for (Method method : annotated) {
                tests.add(toDynamicTest(method));
            }
        }
        return tests.stream();
    }

    private static @NotNull DynamicTest toDynamicTest(@NotNull Method method) {
        SimCase ann = method.getAnnotation(SimCase.class);
        String displayName = ann.name().isEmpty() ? method.getDeclaringClass().getSimpleName() + "." + method.getName() : ann.name();
        return DynamicTest.dynamicTest(displayName, () -> invoke(method, displayName));
    }

    private static void invoke(@NotNull Method method, @NotNull String displayName) throws Throwable {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException(method + " annotated with @SimCase must be static");
        }
        if (!SimulatorCase.class.isAssignableFrom(method.getReturnType())) {
            throw new IllegalStateException(method + " annotated with @SimCase must return SimulatorCase");
        }
        method.setAccessible(true);
        SimulatorCase simulatorCase = (SimulatorCase) method.invoke(null);
        simulatorCase.named(displayName).execute();
    }

    private static @NotNull List<Class<?>> scanContainers(@NotNull String basePackage) {
        List<Class<?>> out = new ArrayList<>();
        String resourcePath = basePackage.replace('.', '/');
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            var resources = loader.getResources(resourcePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                URI uri = url.toURI();
                Path root;
                if ("jar".equals(uri.getScheme())) {
                    root = FileSystems.newFileSystem(uri, Map.of()).getPath(resourcePath);
                } else {
                    root = Paths.get(uri);
                }
                try (Stream<Path> files = Files.walk(root)) {
                    files.filter(p -> p.toString().endsWith(".class")).forEach(p -> {
                        String relative = root.relativize(p).toString().replace(java.io.File.separatorChar, '/');
                        String className = basePackage + "." + relative.replace('/', '.').replaceFirst("\\.class$", "");
                        try {
                            Class<?> cls = Class.forName(className, false, loader);
                            if (cls.isAnnotationPresent(SimulatorTest.class)) out.add(cls);
                        } catch (ClassNotFoundException ignored) {
                        }
                    });
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to scan " + basePackage, e);
        }
        return out;
    }
}
