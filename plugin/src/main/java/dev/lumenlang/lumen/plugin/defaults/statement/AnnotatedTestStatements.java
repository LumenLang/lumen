package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.pattern.annotation.Condition;
import dev.lumenlang.lumen.api.pattern.annotation.Description;
import dev.lumenlang.lumen.api.pattern.annotation.Example;
import dev.lumenlang.lumen.api.pattern.annotation.Expression;
import dev.lumenlang.lumen.api.pattern.annotation.Inject;
import dev.lumenlang.lumen.api.pattern.annotation.Pattern;
import dev.lumenlang.lumen.api.pattern.annotation.Since;
import dev.lumenlang.lumen.api.pattern.annotation.Statement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Smoke-test handlers exercising the annotation-form pipeline end-to-end.
 * Mixes single-line and multi-line bodies across statement, condition, and
 * expression to confirm method-based fallback kicks in correctly. Uses
 * external Bukkit types so import propagation is exercised.
 */
public final class AnnotatedTestStatements {

    private AnnotatedTestStatements() {
    }

    @Statement
    @Pattern("set fire to %p:PLAYER%")
    @Description("Sets a player on fire for 10 seconds.")
    @Example("set fire to player")
    @Since("1.4.0")
    public static void setFire(@Inject Player p) {
        p.setFireTicks(200);
        p.sendMessage("You're on fire!");
    }

    @Statement
    @Pattern("give iron sword to %p:PLAYER%")
    @Description("Drops an iron sword into the player's inventory.")
    @Example("give iron sword to player")
    @Since("1.4.0")
    public static void giveIronSword(@Inject Player p) {
        ItemStack sword = new ItemStack(Material.IRON_SWORD, 1);
        p.getInventory().addItem(sword);
        p.sendMessage("Take this sword, " + p.getName() + "!");
    }

    @Statement
    @Pattern("teleport %p:PLAYER% to spawn")
    @Description("Teleports a player to the world's spawn location.")
    @Example("teleport player to spawn")
    @Since("1.4.0")
    public static void teleportToSpawn(@Inject Player p) {
        Location spawn = p.getWorld().getSpawnLocation();
        p.teleport(spawn);
        Bukkit.broadcastMessage(p.getName() + " went home.");
    }

    @Condition
    @Pattern("%p:PLAYER% is on fire")
    @Description("True when a player has burning fire ticks.")
    @Example("if player is on fire:")
    @Since("1.4.0")
    public static boolean isOnFire(@Inject Player p) {
        return p.getFireTicks() > 0;
    }

    @Condition
    @Pattern("%p:PLAYER% is critically wounded")
    @Description("True when a player is below 25% of their max health.")
    @Example("if player is critically wounded:")
    @Since("1.4.0")
    public static boolean isCriticallyWounded(@Inject Player p) {
        double max = p.getMaxHealth();
        double cur = p.getHealth();
        return cur < max * 0.25;
    }

    @Expression
    @Pattern("location of %p:PLAYER%")
    @Description("Returns the player's current location.")
    @Example("set loc to location of player")
    @Since("1.4.0")
    public static Location locationOf(@Inject Player p) {
        return p.getLocation();
    }

    @Expression
    @Pattern("home spawn for %p:PLAYER%")
    @Description("Returns a tweaked spawn location for the player's world.")
    @Example("set home to home spawn for player")
    @Since("1.4.0")
    public static Location homeSpawnFor(@Inject Player p) {
        Location spawn = p.getWorld().getSpawnLocation().clone();
        spawn.setY(spawn.getY() + 1.0);
        return spawn;
    }
}
