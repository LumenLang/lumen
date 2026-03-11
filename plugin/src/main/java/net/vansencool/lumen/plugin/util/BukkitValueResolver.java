package net.vansencool.lumen.plugin.util;

import net.vansencool.lumen.pipeline.persist.serializer.SerializedLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Value resolver that converts pipeline-level serialized types into their
 * Bukkit runtime equivalents.
 */
public final class BukkitValueResolver implements Function<Object, Object> {

    public static final BukkitValueResolver INSTANCE = new BukkitValueResolver();

    private BukkitValueResolver() {
    }

    private static @NotNull Object resolve(@NotNull Object value) {
        if (value instanceof SerializedLocation loc) {
            return resolveLocation(loc);
        }
        if (value instanceof List<?> list) {
            return resolveList(list);
        }
        if (value instanceof Map<?, ?> map) {
            return resolveMap(map);
        }
        return value;
    }

    private static @NotNull Object resolveLocation(@NotNull SerializedLocation loc) {
        World world = Bukkit.getWorld(loc.worldName());
        if (world == null) {
            return loc;
        }
        return new Location(world, loc.x(), loc.y(), loc.z());
    }

    private static @NotNull List<Object> resolveList(@NotNull List<?> list) {
        boolean changed = false;
        List<Object> result = new ArrayList<>(list.size());
        for (Object element : list) {
            Object resolved = resolve(element);
            if (resolved != element) changed = true;
            result.add(resolved);
        }
        return changed ? result : new ArrayList<>(list);
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Map<Object, Object> resolveMap(@NotNull Map<?, ?> map) {
        boolean changed = false;
        Map<Object, Object> result = new LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = resolve(entry.getKey());
            Object val = entry.getValue() != null ? resolve(entry.getValue()) : null;
            if (key != entry.getKey() || val != entry.getValue()) changed = true;
            result.put(key, val);
        }
        return changed ? result : (Map<Object, Object>) map;
    }

    @Override
    public @NotNull Object apply(@NotNull Object value) {
        return resolve(value);
    }
}
