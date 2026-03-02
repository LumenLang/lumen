package net.vansencool.lumen.plugin.defaults.attributes;

import net.vansencool.lumen.api.version.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Version-aware registry that maps user-friendly attribute names to the correct
 * Bukkit {@code Attribute} enum constant for the running server version.
 *
 * <h2>Why this class exists</h2>
 * <p>Minecraft 1.21.2 renamed every {@code Attribute} enum constant, dropping
 * the {@code GENERIC_}, {@code HORSE_}, {@code ZOMBIE_}, and {@code PLAYER_}
 * prefixes (for example {@code GENERIC_MAX_HEALTH} became {@code MAX_HEALTH}).
 * On top of that, several attributes were only introduced in 1.20.5 or 1.21,
 * so they do not exist at all on older servers. This class hides both concerns
 * behind a single {@link #resolve(String)} call that always returns the correct
 * enum name, or {@code null} when the attribute is not available.
 *
 * <h2>How resolution works</h2>
 * <ol>
 *   <li>The input is lowercased, trimmed, and any spaces or hyphens are
 *       replaced with underscores.</li>
 *   <li>Common prefixes ({@code generic_}, {@code horse_}, {@code zombie_},
 *       {@code player_}) are stripped so all input converges to a canonical
 *       form like {@code "max_health"} or {@code "attack_damage"}.</li>
 *   <li>The canonical form is looked up in the internal registry.</li>
 *   <li>If the current server version is older than the attribute's
 *       introduction version, {@code null} is returned.</li>
 *   <li>Otherwise, the legacy (pre-1.21.2) or new (1.21.2+) enum name is
 *       returned depending on the server version.</li>
 * </ol>
 *
 * <h2>Accepted input formats</h2>
 * <p>All of the following resolve to the same attribute:
 * <ul>
 *   <li>{@code "max_health"} (canonical form)</li>
 *   <li>{@code "max health"} or {@code "max-health"} (spaces and hyphens)</li>
 *   <li>{@code "GENERIC_MAX_HEALTH"} (legacy enum name)</li>
 *   <li>{@code "MAX_HEALTH"} (new enum name)</li>
 * </ul>
 *
 * <h2>Attribute availability by version</h2>
 * <table>
 *   <tr><th>Canonical Name</th><th>Pre-1.21.2</th><th>1.21.2+</th><th>Since</th></tr>
 *   <tr><td>max_health</td><td>GENERIC_MAX_HEALTH</td><td>MAX_HEALTH</td><td>1.20</td></tr>
 *   <tr><td>follow_range</td><td>GENERIC_FOLLOW_RANGE</td><td>FOLLOW_RANGE</td><td>1.20</td></tr>
 *   <tr><td>knockback_resistance</td><td>GENERIC_KNOCKBACK_RESISTANCE</td><td>KNOCKBACK_RESISTANCE</td><td>1.20</td></tr>
 *   <tr><td>movement_speed</td><td>GENERIC_MOVEMENT_SPEED</td><td>MOVEMENT_SPEED</td><td>1.20</td></tr>
 *   <tr><td>flying_speed</td><td>GENERIC_FLYING_SPEED</td><td>FLYING_SPEED</td><td>1.20</td></tr>
 *   <tr><td>attack_damage</td><td>GENERIC_ATTACK_DAMAGE</td><td>ATTACK_DAMAGE</td><td>1.20</td></tr>
 *   <tr><td>attack_knockback</td><td>GENERIC_ATTACK_KNOCKBACK</td><td>ATTACK_KNOCKBACK</td><td>1.20</td></tr>
 *   <tr><td>attack_speed</td><td>GENERIC_ATTACK_SPEED</td><td>ATTACK_SPEED</td><td>1.20</td></tr>
 *   <tr><td>armor</td><td>GENERIC_ARMOR</td><td>ARMOR</td><td>1.20</td></tr>
 *   <tr><td>armor_toughness</td><td>GENERIC_ARMOR_TOUGHNESS</td><td>ARMOR_TOUGHNESS</td><td>1.20</td></tr>
 *   <tr><td>luck</td><td>GENERIC_LUCK</td><td>LUCK</td><td>1.20</td></tr>
 *   <tr><td>jump_strength</td><td>HORSE_JUMP_STRENGTH</td><td>JUMP_STRENGTH</td><td>1.20</td></tr>
 *   <tr><td>spawn_reinforcements</td><td>ZOMBIE_SPAWN_REINFORCEMENTS</td><td>SPAWN_REINFORCEMENTS</td><td>1.20</td></tr>
 *   <tr><td>max_absorption</td><td>GENERIC_MAX_ABSORPTION</td><td>MAX_ABSORPTION</td><td>1.20.5</td></tr>
 *   <tr><td>scale</td><td>GENERIC_SCALE</td><td>SCALE</td><td>1.20.5</td></tr>
 *   <tr><td>step_height</td><td>GENERIC_STEP_HEIGHT</td><td>STEP_HEIGHT</td><td>1.20.5</td></tr>
 *   <tr><td>gravity</td><td>GENERIC_GRAVITY</td><td>GRAVITY</td><td>1.20.5</td></tr>
 *   <tr><td>safe_fall_distance</td><td>GENERIC_SAFE_FALL_DISTANCE</td><td>SAFE_FALL_DISTANCE</td><td>1.20.5</td></tr>
 *   <tr><td>fall_damage_multiplier</td><td>GENERIC_FALL_DAMAGE_MULTIPLIER</td><td>FALL_DAMAGE_MULTIPLIER</td><td>1.20.5</td></tr>
 *   <tr><td>block_interaction_range</td><td>PLAYER_BLOCK_INTERACTION_RANGE</td><td>BLOCK_INTERACTION_RANGE</td><td>1.20.5</td></tr>
 *   <tr><td>entity_interaction_range</td><td>PLAYER_ENTITY_INTERACTION_RANGE</td><td>ENTITY_INTERACTION_RANGE</td><td>1.20.5</td></tr>
 *   <tr><td>block_break_speed</td><td>PLAYER_BLOCK_BREAK_SPEED</td><td>BLOCK_BREAK_SPEED</td><td>1.20.5</td></tr>
 *   <tr><td>burning_time</td><td>GENERIC_BURNING_TIME</td><td>BURNING_TIME</td><td>1.21</td></tr>
 *   <tr><td>explosion_knockback_resistance</td><td>GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE</td><td>EXPLOSION_KNOCKBACK_RESISTANCE</td><td>1.21</td></tr>
 *   <tr><td>mining_efficiency</td><td>PLAYER_MINING_EFFICIENCY</td><td>MINING_EFFICIENCY</td><td>1.21</td></tr>
 *   <tr><td>movement_efficiency</td><td>GENERIC_MOVEMENT_EFFICIENCY</td><td>MOVEMENT_EFFICIENCY</td><td>1.21</td></tr>
 *   <tr><td>oxygen_bonus</td><td>GENERIC_OXYGEN_BONUS</td><td>OXYGEN_BONUS</td><td>1.21</td></tr>
 *   <tr><td>sneaking_speed</td><td>PLAYER_SNEAKING_SPEED</td><td>SNEAKING_SPEED</td><td>1.21</td></tr>
 *   <tr><td>submerged_mining_speed</td><td>PLAYER_SUBMERGED_MINING_SPEED</td><td>SUBMERGED_MINING_SPEED</td><td>1.21</td></tr>
 *   <tr><td>sweeping_damage_ratio</td><td>PLAYER_SWEEPING_DAMAGE_RATIO</td><td>SWEEPING_DAMAGE_RATIO</td><td>1.21</td></tr>
 *   <tr><td>water_movement_efficiency</td><td>GENERIC_WATER_MOVEMENT_EFFICIENCY</td><td>WATER_MOVEMENT_EFFICIENCY</td><td>1.21</td></tr>
 *   <tr><td>tempt_range</td><td>GENERIC_TEMPT_RANGE</td><td>TEMPT_RANGE</td><td>1.21</td></tr>
 * </table>
 *
 * @see MinecraftVersion
 * @see #resolve(String)
 * @see #knownNames()
 * @see #availableAttributes()
 */
@SuppressWarnings("unused")
public final class AttributeNames {

    private static final Map<String, AttributeEntry> ENTRIES = new LinkedHashMap<>();

    static {
        add("max_health", "GENERIC_MAX_HEALTH", "MAX_HEALTH", MinecraftVersion.V1_20);
        add("follow_range", "GENERIC_FOLLOW_RANGE", "FOLLOW_RANGE", MinecraftVersion.V1_20);
        add("knockback_resistance", "GENERIC_KNOCKBACK_RESISTANCE", "KNOCKBACK_RESISTANCE", MinecraftVersion.V1_20);
        add("movement_speed", "GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED", MinecraftVersion.V1_20);
        add("flying_speed", "GENERIC_FLYING_SPEED", "FLYING_SPEED", MinecraftVersion.V1_20);
        add("attack_damage", "GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE", MinecraftVersion.V1_20);
        add("attack_knockback", "GENERIC_ATTACK_KNOCKBACK", "ATTACK_KNOCKBACK", MinecraftVersion.V1_20);
        add("attack_speed", "GENERIC_ATTACK_SPEED", "ATTACK_SPEED", MinecraftVersion.V1_20);
        add("armor", "GENERIC_ARMOR", "ARMOR", MinecraftVersion.V1_20);
        add("armor_toughness", "GENERIC_ARMOR_TOUGHNESS", "ARMOR_TOUGHNESS", MinecraftVersion.V1_20);
        add("luck", "GENERIC_LUCK", "LUCK", MinecraftVersion.V1_20);
        add("jump_strength", "HORSE_JUMP_STRENGTH", "JUMP_STRENGTH", MinecraftVersion.V1_20);
        add("spawn_reinforcements", "ZOMBIE_SPAWN_REINFORCEMENTS", "SPAWN_REINFORCEMENTS", MinecraftVersion.V1_20);

        add("max_absorption", "GENERIC_MAX_ABSORPTION", "MAX_ABSORPTION", MinecraftVersion.V1_20_5);
        add("scale", "GENERIC_SCALE", "SCALE", MinecraftVersion.V1_20_5);
        add("step_height", "GENERIC_STEP_HEIGHT", "STEP_HEIGHT", MinecraftVersion.V1_20_5);
        add("gravity", "GENERIC_GRAVITY", "GRAVITY", MinecraftVersion.V1_20_5);
        add("safe_fall_distance", "GENERIC_SAFE_FALL_DISTANCE", "SAFE_FALL_DISTANCE", MinecraftVersion.V1_20_5);
        add("fall_damage_multiplier", "GENERIC_FALL_DAMAGE_MULTIPLIER", "FALL_DAMAGE_MULTIPLIER", MinecraftVersion.V1_20_5);
        add("block_interaction_range", "PLAYER_BLOCK_INTERACTION_RANGE", "BLOCK_INTERACTION_RANGE", MinecraftVersion.V1_20_5);
        add("entity_interaction_range", "PLAYER_ENTITY_INTERACTION_RANGE", "ENTITY_INTERACTION_RANGE", MinecraftVersion.V1_20_5);
        add("block_break_speed", "PLAYER_BLOCK_BREAK_SPEED", "BLOCK_BREAK_SPEED", MinecraftVersion.V1_20_5);

        add("burning_time", "GENERIC_BURNING_TIME", "BURNING_TIME", MinecraftVersion.V1_21);
        add("explosion_knockback_resistance", "GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE", "EXPLOSION_KNOCKBACK_RESISTANCE", MinecraftVersion.V1_21);
        add("mining_efficiency", "PLAYER_MINING_EFFICIENCY", "MINING_EFFICIENCY", MinecraftVersion.V1_21);
        add("movement_efficiency", "GENERIC_MOVEMENT_EFFICIENCY", "MOVEMENT_EFFICIENCY", MinecraftVersion.V1_21);
        add("oxygen_bonus", "GENERIC_OXYGEN_BONUS", "OXYGEN_BONUS", MinecraftVersion.V1_21);
        add("sneaking_speed", "PLAYER_SNEAKING_SPEED", "SNEAKING_SPEED", MinecraftVersion.V1_21);
        add("submerged_mining_speed", "PLAYER_SUBMERGED_MINING_SPEED", "SUBMERGED_MINING_SPEED", MinecraftVersion.V1_21);
        add("sweeping_damage_ratio", "PLAYER_SWEEPING_DAMAGE_RATIO", "SWEEPING_DAMAGE_RATIO", MinecraftVersion.V1_21);
        add("water_movement_efficiency", "GENERIC_WATER_MOVEMENT_EFFICIENCY", "WATER_MOVEMENT_EFFICIENCY", MinecraftVersion.V1_21);
        add("tempt_range", "GENERIC_TEMPT_RANGE", "TEMPT_RANGE", MinecraftVersion.V1_21);
    }

    private AttributeNames() {
    }

    /**
     * Resolves a user-friendly attribute name to the correct {@code Attribute} enum
     * constant name for the currently running server version.
     *
     * <p>The input is normalized through the following steps:
     * <ol>
     *   <li>Trimmed, lowercased, spaces and hyphens converted to underscores</li>
     *   <li>Prefixes {@code generic_}, {@code horse_}, {@code zombie_}, and
     *       {@code player_} are stripped</li>
     *   <li>Result is matched against the canonical name registry</li>
     * </ol>
     *
     * <p>This means all of the following inputs resolve identically:
     * <ul>
     *   <li>{@code "max_health"} or {@code "max health"} or {@code "max-health"}</li>
     *   <li>{@code "GENERIC_MAX_HEALTH"} (legacy Bukkit enum name)</li>
     *   <li>{@code "MAX_HEALTH"} (modern Bukkit enum name)</li>
     * </ul>
     *
     * <p>Returns {@code null} in two cases:
     * <ul>
     *   <li>The input does not match any known attribute name</li>
     *   <li>The attribute exists but was introduced in a version newer than the
     *       current server (e.g. {@code "scale"} on a 1.20 server, since it
     *       was added in 1.20.5)</li>
     * </ul>
     *
     * @param input the user-provided attribute name, in any supported format
     * @return the version-correct {@code Attribute} enum constant name
     *         (e.g. {@code "GENERIC_MAX_HEALTH"} on 1.20 or {@code "MAX_HEALTH"}
     *         on 1.21.2+), or {@code null} if unrecognized or unavailable
     */
    public static @Nullable String resolve(@NotNull String input) {
        String normalized = input.trim().toLowerCase().replace(' ', '_').replace('-', '_');
        normalized = normalized.replace("generic_", "")
                .replace("horse_", "")
                .replace("zombie_", "")
                .replace("player_", "");

        AttributeEntry entry = ENTRIES.get(normalized);
        if (entry == null) return null;

        MinecraftVersion current = MinecraftVersion.current();
        if (current.isBefore(entry.since)) return null;

        if (current.isAtLeast(MinecraftVersion.V1_21_2)) {
            return entry.newName;
        }
        return entry.legacyName;
    }

    /**
     * Returns the set of all known canonical (short-form) attribute names,
     * regardless of whether they are available on the current server version.
     *
     * <p>These are the normalized keys used internally and are also the
     * recommended input format for {@link #resolve(String)}. Examples include
     * {@code "max_health"}, {@code "attack_damage"}, {@code "scale"}, etc.
     *
     * <p>Use {@link #availableAttributes()} instead if you only want attributes
     * that exist on the running server.
     *
     * @return an unmodifiable set of all canonical attribute names
     */
    public static @NotNull Set<String> knownNames() {
        return Set.copyOf(ENTRIES.keySet());
    }

    /**
     * Returns every attribute that is available on the current server version,
     * mapped from canonical name to the version-correct enum constant name.
     *
     * <p>Attributes introduced in a newer version than the running server are
     * excluded from the result. On a 1.20 server, for instance, {@code "scale"}
     * (added in 1.20.5) will not appear in the returned map.
     *
     * <p>The returned map preserves insertion order (registration order).
     *
     * @return an ordered map where keys are canonical names (e.g. {@code "max_health"})
     *         and values are the Bukkit enum constant names for the current version
     *         (e.g. {@code "GENERIC_MAX_HEALTH"} on 1.20 or {@code "MAX_HEALTH"} on 1.21.2+)
     */
    public static @NotNull Map<String, String> availableAttributes() {
        Map<String, String> result = new LinkedHashMap<>();
        MinecraftVersion current = MinecraftVersion.current();
        boolean useNew = current.isAtLeast(MinecraftVersion.V1_21_2);
        for (Map.Entry<String, AttributeEntry> e : ENTRIES.entrySet()) {
            if (current.isAtLeast(e.getValue().since)) {
                result.put(e.getKey(), useNew ? e.getValue().newName : e.getValue().legacyName);
            }
        }
        return result;
    }

    /**
     * Registers an attribute in the internal registry.
     *
     * @param canonical  the short-form user-facing name (e.g. {@code "max_health"}), used as
     *                   the lookup key after normalization
     * @param legacyName the Bukkit {@code Attribute} enum constant name for servers before 1.21.2
     *                   (e.g. {@code "GENERIC_MAX_HEALTH"})
     * @param newName    the Bukkit {@code Attribute} enum constant name for servers running 1.21.2
     *                   or later (e.g. {@code "MAX_HEALTH"})
     * @param since      the earliest Minecraft version where this attribute exists; it will not
     *                   be resolved on older servers
     */
    private static void add(@NotNull String canonical,
                            @NotNull String legacyName,
                            @NotNull String newName,
                            @NotNull MinecraftVersion since) {
        ENTRIES.put(canonical, new AttributeEntry(legacyName, newName, since));
    }

    /**
     * Holds the two version-dependent enum names for a single attribute,
     * along with the minimum version required for the attribute to exist.
     *
     * @param legacyName the enum constant name used on servers before 1.21.2
     * @param newName    the enum constant name used on servers running 1.21.2 or later
     * @param since      the earliest version where this attribute is available
     */
    private record AttributeEntry(@NotNull String legacyName,
                                  @NotNull String newName,
                                  @NotNull MinecraftVersion since) {
    }
}
