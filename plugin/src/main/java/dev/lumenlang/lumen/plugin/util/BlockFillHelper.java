package dev.lumenlang.lumen.plugin.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Runtime helper used by generated script code to fill a cuboid region of blocks
 * with a given material.
 */
@SuppressWarnings("unused") // Called by generated code
public final class BlockFillHelper {

    private BlockFillHelper() {
    }

    /**
     * Fills all blocks in the cuboid between two locations with the specified material.
     * Both locations must be in the same world.
     *
     * @param a   one corner of the region
     * @param b   the opposite corner of the region
     * @param mat the material to fill with
     */
    public static void fill(@NotNull Location a, @NotNull Location b, @NotNull Material mat) {
        World world = a.getWorld();
        if (world == null || world != b.getWorld()) {
            throw new RuntimeException("[Block Fill] Locations must be in the same world");
        }
        int x1 = Math.min(a.getBlockX(), b.getBlockX());
        int x2 = Math.max(a.getBlockX(), b.getBlockX());
        int y1 = Math.min(a.getBlockY(), b.getBlockY());
        int y2 = Math.max(a.getBlockY(), b.getBlockY());
        int z1 = Math.min(a.getBlockZ(), b.getBlockZ());
        int z2 = Math.max(a.getBlockZ(), b.getBlockZ());
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    world.getBlockAt(x, y, z).setType(mat);
                }
            }
        }
    }
}
