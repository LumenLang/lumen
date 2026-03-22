package dev.lumenlang.lumen.plugin.scanner;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.scanner.RegistrationScanner;
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
 * Internal {@link RegistrationScanner.Backend} implementation that scans for
 * {@link Registration @Registration} classes, instantiates them, and invokes
 * their {@link Call @Call} methods with the provided {@link LumenAPI}.
 *
 * <p>Supports both JAR-based and directory-based class scanning.
 */
public final class RegistrationScannerBackend implements RegistrationScanner.Backend {

    private static final Logger LOGGER = Logger.getLogger(RegistrationScannerBackend.class.getName());

    private final LumenAPI api;

    public RegistrationScannerBackend(@NotNull LumenAPI api) {
        this.api = api;
    }

    @Override
    public void scan(@NotNull String basePackage) {
        String basePath = basePackage.replace('.', '/');
        try {
            URI uri = RegistrationScannerBackend.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            Path path = Paths.get(uri);
            List<String> classNames = new ArrayList<>();
            if (Files.isDirectory(path)) {
                collectFromDirectory(path.resolve(basePath), basePackage, classNames);
            } else {
                collectFromJar(path, basePath, classNames);
            }

            List<Class<?>> registrables = new ArrayList<>();
            for (String className : classNames) {
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Registration.class)) {
                        registrables.add(clazz);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load class: " + className, e);
                }
            }

            registrables.sort(Comparator.comparingInt(clazz -> {
                Registration reg = clazz.getAnnotation(Registration.class);
                return reg != null ? reg.order() : 0;
            }));

            for (Class<?> clazz : registrables) {
                processClass(clazz);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to scan registrations in package " + basePackage, e);
        }
    }

    private void collectFromJar(@NotNull Path jarPath, @NotNull String basePath,
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

    private void collectFromDirectory(@NotNull Path dir, @NotNull String basePackage,
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

    private void processClass(@NotNull Class<?> clazz) {
        try {
            try {
                clazz.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                LOGGER.warning("@Registration class " + clazz.getName()
                        + " has no default (no-arg) constructor, skipping");
                return;
            }

            Object instance = clazz.getDeclaredConstructor().newInstance();
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Call.class)) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1 || !LumenAPI.class.isAssignableFrom(params[0])) {
                    LOGGER.warning("@Call method " + clazz.getName() + "#" + method.getName()
                            + " must accept exactly one LumenAPI parameter");
                    continue;
                }
                method.setAccessible(true);
                method.invoke(instance, api);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to process registration class: " + clazz.getName(), e);
        }
    }
}
