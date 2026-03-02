package net.vansencool.lumen.plugin.util;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Runtime helpers used by generated location condition code.
 */
@SuppressWarnings("unused")
public final class LocationUtils {

    private LocationUtils() {
    }

    /**
     * Returns {@code true} if {@code loc} is within the rectangular region
     * defined by {@code min} and {@code max} (inclusive on all axes).
     */
    public static boolean inside(@NotNull Location loc, @NotNull Location min, @NotNull Location max) {
        return loc.getX() >= Math.min(min.getX(), max.getX())
                && loc.getX() <= Math.max(min.getX(), max.getX())
                && loc.getY() >= Math.min(min.getY(), max.getY())
                && loc.getY() <= Math.max(min.getY(), max.getY())
                && loc.getZ() >= Math.min(min.getZ(), max.getZ())
                && loc.getZ() <= Math.max(min.getZ(), max.getZ());
    }

    /**
     * Negated form of {@link #inside(Location, Location, Location)}.
     */
    public static boolean notInside(@NotNull Location loc, @NotNull Location min, @NotNull Location max) {
        return !inside(loc, min, max);
    }

    /**
     * {@code true} if both locations reside in the same world. This uses
     * {@link Objects#equals(Object,Object)} to safely handle null worlds.
     */
    public static boolean sameWorld(@NotNull Location a, @NotNull Location b) {
        return Objects.equals(a.getWorld(), b.getWorld());
    }

    /**
     * {@code true} when {@code loc} is within {@code dist} units of {@code target}.
     * The check also ensures both locations belong to the same world to avoid
     * distance exceptions.
     */
    public static boolean near(@NotNull Location loc, @NotNull Location target, double dist) {
        return loc.getWorld() != null
                && loc.getWorld().equals(target.getWorld())
                && loc.distance(target) <= dist;
    }
}
