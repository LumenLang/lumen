package dev.lumenlang.lumen.api.version;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents all known Minecraft versions that Lumen supports, from 1.20 through 1.21.11.
 *
 * <p>Use {@link #current()} to get the server's detected version after
 * {@link #detect(String)} has been called during plugin initialization.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * MinecraftVersion ver = MinecraftVersion.current();
 * if (ver.isAtLeast(MinecraftVersion.V1_21_2)) {
 *     // use new attribute names
 * }
 * }</pre>
 *
 * <h2>Ordering</h2>
 * <p>Enum constants are declared in chronological release order. All comparison
 * methods ({@link #isAtLeast}, {@link #isBefore}, etc.) rely on ordinal comparison.
 */
@SuppressWarnings("unused")
public enum MinecraftVersion {

    V1_20("1.20"),
    V1_20_1("1.20.1"),
    V1_20_2("1.20.2"),
    V1_20_3("1.20.3"),
    V1_20_4("1.20.4"),
    V1_20_5("1.20.5"),
    V1_20_6("1.20.6"),
    V1_21("1.21"),
    V1_21_1("1.21.1"),
    V1_21_2("1.21.2"),
    V1_21_3("1.21.3"),
    V1_21_4("1.21.4"),
    V1_21_5("1.21.5"),
    V1_21_6("1.21.6"),
    V1_21_7("1.21.7"),
    V1_21_8("1.21.8"),
    V1_21_9("1.21.9"),
    V1_21_10("1.21.10"),
    V1_21_11("1.21.11"),
    UNKNOWN("unknown");

    private static final Map<String, MinecraftVersion> BY_ID = new ConcurrentHashMap<>();
    private static volatile MinecraftVersion current = UNKNOWN;

    static {
        for (MinecraftVersion v : values()) {
            if (v != UNKNOWN) {
                BY_ID.put(v.id, v);
            }
        }
    }

    private final String id;

    MinecraftVersion(@NotNull String id) {
        this.id = id;
    }

    /**
     * Returns the currently detected server version.
     *
     * <p>Returns {@link #UNKNOWN} if {@link #detect(String)} has not been called yet.
     *
     * @return the current version, never null
     */
    public static @NotNull MinecraftVersion current() {
        return current;
    }

    /**
     * Detects and stores the current Minecraft version from the raw version string.
     *
     * @param versionString the raw Minecraft version string (e.g. {@code "1.21.4"})
     * @return the detected version
     */
    public static @NotNull MinecraftVersion detect(@NotNull String versionString) {
        String trimmed = versionString.trim();
        MinecraftVersion found = BY_ID.get(trimmed);
        current = found != null ? found : UNKNOWN;
        return current;
    }

    /**
     * Looks up a version constant by its id string.
     *
     * @param versionString the version string (e.g. {@code "1.20.4"})
     * @return the matching version, or null if not found
     */
    public static @Nullable MinecraftVersion fromString(@NotNull String versionString) {
        return BY_ID.get(versionString.trim());
    }

    /**
     * Returns the version string as it appears in the Minecraft protocol
     * (e.g. {@code "1.21.4"}).
     *
     * @return the version id
     */
    public @NotNull String id() {
        return id;
    }

    /**
     * Checks whether this version is the same as or newer than the given version.
     *
     * @param other the version to compare against
     * @return true if this version is at least {@code other}
     */
    public boolean isAtLeast(@NotNull MinecraftVersion other) {
        return this.ordinal() >= other.ordinal();
    }

    /**
     * Checks whether this version is strictly older than the given version.
     *
     * @param other the version to compare against
     * @return true if this version is before {@code other}
     */
    public boolean isBefore(@NotNull MinecraftVersion other) {
        return this.ordinal() < other.ordinal();
    }

    /**
     * Checks whether this version is the same as or older than the given version.
     *
     * @param other the version to compare against
     * @return true if this version is at most {@code other}
     */
    public boolean isAtMost(@NotNull MinecraftVersion other) {
        return this.ordinal() <= other.ordinal();
    }

    /**
     * Checks whether this version is strictly newer than the given version.
     *
     * @param other the version to compare against
     * @return true if this version is after {@code other}
     */
    public boolean isAfter(@NotNull MinecraftVersion other) {
        return this.ordinal() > other.ordinal();
    }

    /**
     * Checks whether this version falls within the given range (inclusive).
     *
     * @param from the lower bound (inclusive)
     * @param to   the upper bound (inclusive)
     * @return true if this version is between {@code from} and {@code to}
     */
    public boolean isBetween(@NotNull MinecraftVersion from, @NotNull MinecraftVersion to) {
        return isAtLeast(from) && isAtMost(to);
    }

    @Override
    public String toString() {
        return id;
    }
}
