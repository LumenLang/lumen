package net.vansencool.lumen.api.pattern;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of documentation categories for Lumen patterns.
 *
 * <p>Categories are interned: calling {@link #createOrGet(String)} with the same name
 * (case-insensitive) always returns the same {@link Category} instance. Built-in categories
 * are provided as constants for convenience.
 *
 * <p>Addon developers can create their own categories, but do not create a category with your addon name.
 * <pre>{@code
 * Category myCategory = Categories.createOrGet("Type");
 * }</pre>
 *
 * @see Category
 */
public final class Categories {

    private static final Map<String, Category> CATEGORIES = new ConcurrentHashMap<>();

    public static final Category PLAYER = createOrGet("Player");
    public static final Category ENTITY = createOrGet("Entity");
    public static final Category WORLD = createOrGet("World");
    public static final Category LOCATION = createOrGet("Location");
    public static final Category SERVER = createOrGet("Server");
    public static final Category INVENTORY = createOrGet("Inventory");
    public static final Category LIST = createOrGet("List");
    public static final Category MAP = createOrGet("Map");
    public static final Category CONTROL_FLOW = createOrGet("Control Flow");
    public static final Category SCHEDULING = createOrGet("Scheduling");
    public static final Category EVENT = createOrGet("Event");
    public static final Category LIFECYCLE = createOrGet("Lifecycle");
    public static final Category COMMAND = createOrGet("Command");
    public static final Category VARIABLE = createOrGet("Variable");
    public static final Category OFFLINE_PLAYER = createOrGet("Offline Player");
    public static final Category MATH = createOrGet("Math");
    public static final Category TEXT = createOrGet("Text");
    public static final Category BLOCK = createOrGet("Block");
    public static final Category ATTRIBUTE = createOrGet("Attribute");
    public static final Category ITEM = createOrGet("Item");
    public static final Category DATA = createOrGet("Data");

    private Categories() {
    }

    /**
     * Returns an existing category with the given name, or creates a new one if none exists.
     *
     * <p>Category names are compared case-insensitively. The display name of a newly created
     * category preserves the casing of the first call.
     *
     * @param name the category name
     * @return the category instance
     */
    public static @NotNull Category createOrGet(@NotNull String name) {
        return CATEGORIES.computeIfAbsent(name.toLowerCase(), k -> new Category(name));
    }

    /**
     * Returns an unmodifiable view of all registered categories.
     *
     * @return all categories
     */
    public static @NotNull Collection<Category> all() {
        return Collections.unmodifiableCollection(CATEGORIES.values());
    }
}
