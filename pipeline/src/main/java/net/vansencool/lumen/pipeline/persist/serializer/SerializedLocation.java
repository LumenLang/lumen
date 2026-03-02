package net.vansencool.lumen.pipeline.persist.serializer;

import org.jetbrains.annotations.NotNull;

/**
 * Lightweight representation of a Bukkit {@code Location} that can be serialized
 * without depending on the Bukkit API.
 *
 * <p>When a {@code Location} object is persisted via {@link LumenSerializer}, it is
 * stored as a {@code SerializedLocation} containing the world name and coordinates.
 * On deserialization, the pipeline returns this object, and the plugin layer converts
 * it back to a real Bukkit {@code Location} using the registered value resolver.
 *
 * @param worldName the name of the world
 * @param x         the x coordinate
 * @param y         the y coordinate
 * @param z         the z coordinate
 */
public record SerializedLocation(@NotNull String worldName, double x, double y, double z) {

    @Override
    public @NotNull String toString() {
        return "SerializedLocation{world=" + worldName + ", x=" + x + ", y=" + y + ", z=" + z + "}";
    }
}
