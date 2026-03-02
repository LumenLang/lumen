package net.vansencool.lumen.api.scanner;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Scans a package and its sub-packages for classes annotated with {@link Registration},
 * instantiates them, and invokes their {@link Call @Call} methods with the provided
 * {@link LumenAPI} instance.
 *
 * <p>Classes are sorted by {@link Registration#order()} before processing. Lower values
 * are processed first.
 *
 * <p>Supports both JAR-based and directory-based class scanning.
 *
 * @see Registration
 * @see Call
 */
public final class RegistrationScanner {

    private static final Logger LOGGER = Logger.getLogger(RegistrationScanner.class.getName());

    private RegistrationScanner() {
    }

    /**
     * Scans the given base package for registration classes and invokes their call methods.
     *
     * @param basePackage the base package to scan (e.g. {@code "net.vansencool.lumen.defaults"})
     * @param api         the LumenAPI instance to pass to call methods
     */
    public static void scan(@NotNull String basePackage, @NotNull LumenAPI api) {
        String basePath = basePackage.replace('.', '/');
        try {
            URI uri = RegistrationScanner.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            Path path = Paths.get(uri);
            List<String> classNames = new ArrayList<>();
            if (Files.isDirectory(path)) {
                collectFromDirectory(path.resolve(basePath), basePackage, classNames);
            } else {
                collectFromJar(path, basePath, classNames);
            }

            classNames.sort(Comparator.comparingInt(RegistrationScanner::getOrder));

            for (String className : classNames) {
                processClass(className, api);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to scan registrations in package " + basePackage, e);
        }
    }

    private static int getOrder(@NotNull String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Registration reg = clazz.getAnnotation(Registration.class);
            return reg != null ? reg.order() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static void collectFromJar(@NotNull Path jarPath, @NotNull String basePath,
                                       @NotNull List<String> out) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            jar.stream()
                    .filter(entry -> entry.getName().startsWith(basePath) && entry.getName().endsWith(".class"))
                    .forEach(entry -> {
                        String className = entry.getName()
                                .replace('/', '.')
                                .substring(0, entry.getName().length() - 6);
                        out.add(className);
                    });
        }
    }

    private static void collectFromDirectory(@NotNull Path dir, @NotNull String basePackage,
                                             @NotNull List<String> out) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        String relative = dir.relativize(p).toString()
                                .replace(FileSystems.getDefault().getSeparator(), ".")
                                .replace(".class", "");
                        out.add(basePackage + "." + relative);
                    });
        }
    }

    private static void processClass(@NotNull String className, @NotNull LumenAPI api) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!clazz.isAnnotationPresent(Registration.class)) return;

            Object instance = clazz.getDeclaredConstructor().newInstance();
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Call.class)) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1 || !LumenAPI.class.isAssignableFrom(params[0])) {
                    LOGGER.warning("@Call method " + className + "#" + method.getName()
                            + " must accept exactly one LumenAPI parameter");
                    continue;
                }
                method.setAccessible(true);
                method.invoke(instance, api);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to process registration class: " + className, e);
        }
    }
}
