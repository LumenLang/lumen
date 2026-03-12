package dev.lumenlang.lumen.plugin.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Runtime helper used by generated script code to launch an entity
 * toward a target location with a given speed.
 */
@SuppressWarnings("unused")
public final class LaunchHelper {

    private LaunchHelper() {
    }

    /**
     * Launches an entity toward a target location by setting its velocity
     * to a normalized direction vector multiplied by the given speed.
     *
     * @param entity the entity to launch
     * @param target the location to launch toward
     * @param speed  the speed multiplier
     */
    public static void launch(@NotNull Entity entity, @NotNull Location target, double speed) {
        Location from = entity.getLocation();
        Vector direction = target.toVector().subtract(from.toVector());
        if (direction.lengthSquared() < 0.0001) return;
        direction.normalize().multiply(speed);
        entity.setVelocity(direction);
    }
}
