package net.vansencool.lumen.plugin.defaults.events;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.type.JavaTypes;
import net.vansencool.lumen.api.type.RefTypes;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static net.vansencool.lumen.api.pattern.LumaExample.of;
import static net.vansencool.lumen.api.pattern.LumaExample.secondly;
import static net.vansencool.lumen.api.pattern.LumaExample.thirdly;
import static net.vansencool.lumen.api.pattern.LumaExample.top;

/**
 * Registers built-in event definitions.
 */
@Registration
@Description("Registers built-in event definitions: join, quit, move, sneak, sprint, teleport, respawn, flight, entity events")
@SuppressWarnings("unused")
public final class DefaultEvents {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.events().register(api.events().builder("join").by("Lumen")
                .className(PlayerJoinEvent.class.getName())
                .description("Fires when a player joins the server.")
                .example("on join:")
                .since("1.0.0")
                .category("Player")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .build());
        api.events().register(api.events().builder("respawn").by("Lumen")
                .className(PlayerRespawnEvent.class.getName())
                .description("Fires when a player respawns after dying.")
                .example("on respawn:")
                .since("1.0.0")
                .category("Player")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .addVar("respawnLocation", RefTypes.LOCATION, "event.getRespawnLocation()")
                .build());
        api.events().register(api.events().builder("teleport").by("Lumen")
                .className(PlayerTeleportEvent.class.getName())
                .description("Fires when a player teleports.")
                .example("on teleport:")
                .since("1.0.0")
                .category("Player")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .addVar("from", RefTypes.LOCATION, "event.getFrom()")
                .addVar("to", RefTypes.LOCATION, "event.getTo()")
                .build());
        api.events().register(api.events().builder("quit").by("Lumen")
                .className(PlayerQuitEvent.class.getName())
                .description("Fires when a player leaves the server.")
                .example("on quit:")
                .since("1.0.0")
                .category("Player")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .build());
        api.events().register(api.events().builder("move").by("Lumen")
                .className(PlayerMoveEvent.class.getName())
                .description("Fires when a player moves.")
                .example("on move:")
                .since("1.0.0")
                .category("Player")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .addVar("from", RefTypes.LOCATION, "event.getFrom()")
                .addVar("to", RefTypes.LOCATION, "event.getTo()")
                .build());
        api.events().register(api.events().builder("toggle_sneak").by("Lumen")
                .className(PlayerToggleSneakEvent.class.getName())
                .description("Fires when a player starts or stops sneaking.")
                .example("on toggle_sneak:")
                .since("1.0.0")
                .category("Player")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .build());
        api.events().register(api.events().builder("toggle_sprint").by("Lumen")
                .className(PlayerToggleSprintEvent.class.getName())
                .description("Fires when a player starts or stops sprinting.")
                .example("on toggle_sprint:")
                .since("1.0.0")
                .category("Player")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .build());
        api.events().register(api.events().builder("toggle_flight").by("Lumen")
                .className(PlayerToggleFlightEvent.class.getName())
                .description("Fires when a player toggles flight mode.")
                .example("on toggle_flight:")
                .since("1.0.0")
                .category("Player")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .addVar("flying", JavaTypes.BOOLEAN, "event.isFlying()")
                .build());

        api.events().register(api.events().builder("entity_spawn").by("Lumen")
                .className(CreatureSpawnEvent.class.getName())
                .description("Fires when a creature spawns in the world.")
                .example("on entity_spawn:")
                .since("1.0.0")
                .category("Entity")
                .addVar("entity", RefTypes.ENTITY, "event.getEntity()")
                .withMeta("javaClass", LivingEntity.class.getName())
                .addVar("location", RefTypes.LOCATION, "event.getLocation()")
                .build());
        api.events().register(api.events().builder("entity_death").by("Lumen")
                .className(EntityDeathEvent.class.getName())
                .description("Fires when an entity dies.")
                .example("on entity_death:")
                .since("1.0.0")
                .category("Entity")
                .addVar("entity", RefTypes.ENTITY, "event.getEntity()")
                .withMeta("javaClass", LivingEntity.class.getName())
                .addVar("killer", RefTypes.PLAYER, Player.class.getName(),
                        """
                                if (event.getEntity().getKiller() != null) {
                                    killer = event.getEntity().getKiller();
                                } else {
                                    killer = null;
                                }""")
                .withMeta("nullable", true)
                .build());
        api.events().register(api.events().builder("entity_damage").by("Lumen")
                .className(EntityDamageEvent.class.getName())
                .description("Fires when an entity takes damage.")
                .example("on entity_damage:")
                .since("1.0.0")
                .category("Entity")
                .addVar("entity", RefTypes.ENTITY, "event.getEntity()")
                .addVar("damage", JavaTypes.DOUBLE, "event.getDamage()")
                .build());
        api.events().register(api.events().builder("entity_damage_by_entity").by("Lumen")
                .className(EntityDamageByEntityEvent.class.getName())
                .description("Fires when an entity takes damage from another entity.")
                .example("on entity_damage_by_entity:")
                .since("1.0.0")
                .category("Entity")
                .addVar("entity", RefTypes.ENTITY, "event.getEntity()")
                .addVar("damager", RefTypes.ENTITY, "event.getDamager()")
                .addVar("damagerPlayer", RefTypes.PLAYER, Player.class.getName(),
                        """
                                if (event.getDamager() instanceof Player __dp) {
                                    damagerPlayer = __dp;
                                } else {
                                    damagerPlayer = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("damage", JavaTypes.DOUBLE, "event.getDamage()")
                .build());
        api.events().register(api.events().builder("entity_interact").by("Lumen")
                .className(PlayerInteractEntityEvent.class.getName())
                .description("Fires when a player right-clicks an entity.")
                .example("on entity_interact:")
                .since("1.0.0")
                .category("Entity")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .addVar("entity", RefTypes.ENTITY, "event.getRightClicked()")
                .build());

        api.events().register(api.events().builder("player_death").by("Lumen")
                .className(PlayerDeathEvent.class.getName())
                .description("Fires when a player dies.")
                .example("on player_death:")
                .since("1.0.0")
                .category("Player")
                .addVar("player", RefTypes.PLAYER, "event.getEntity()")
                .addVar("killer", RefTypes.PLAYER, Player.class.getName(),
                        """
                                if (event.getEntity().getKiller() != null) {
                                    killer = event.getEntity().getKiller();
                                } else {
                                    killer = null;
                                }""")
                .withMeta("nullable", true)
                .build());

        api.events().register(api.events().builder("block_break").by("Lumen")
                .className(BlockBreakEvent.class.getName())
                .description("Fires when a player breaks a block.")
                .example("on block_break:")
                .since("1.0.0")
                .category("Block")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .addVar("block", RefTypes.BLOCK, "event.getBlock()")
                .addVar("item", RefTypes.ITEMSTACK, ItemStack.class.getName(),
                        """
                                if (event.getPlayer().getInventory().getItemInMainHand().getType() != org.bukkit.Material.AIR) {
                                    item = event.getPlayer().getInventory().getItemInMainHand();
                                } else {
                                    item = null;
                                }""")
                .withMeta("nullable", true)
                .build());

        api.events().register(api.events().builder("block_place").by("Lumen")
                .className(BlockPlaceEvent.class.getName())
                .description("Fires when a player places a block.")
                .example("on block_place:")
                .since("1.0.0")
                .category("Block")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .addVar("block", RefTypes.BLOCK, "event.getBlock()")
                .addVar("item", RefTypes.ITEMSTACK, "event.getItemInHand()")
                .build());

        api.events().register(api.events().builder("interact").by("Lumen")
                .className(PlayerInteractEvent.class.getName())
                .description("Fires when a player interacts (left or right click) with a block or air. The 'action' variable contains the action type as a string: LEFT_CLICK_BLOCK, RIGHT_CLICK_BLOCK, LEFT_CLICK_AIR, RIGHT_CLICK_AIR, or PHYSICAL.")
                .example("on interact:")
                .example(of(
                        top("on interact:"),
                        secondly("if action is \"LEFT_CLICK_BLOCK\":"),
                        thirdly("message player \"You left clicked a block!\"")))
                .example(of(
                        top("on interact:"),
                        secondly("if action is \"RIGHT_CLICK_BLOCK\":"),
                        thirdly("message player \"You right clicked a block!\""),
                        secondly("else if action is \"LEFT_CLICK_AIR\":"),
                        thirdly("message player \"You left clicked in the air!\"")))
                .since("1.0.0")
                .category("Player")
                .addVar("player", RefTypes.PLAYER, "event.getPlayer()")
                .addVar("action", "String", "event.getAction().name()")
                .addVar("block", RefTypes.BLOCK, Block.class.getName(),
                        """
                                if (event.getClickedBlock() != null) {
                                    block = event.getClickedBlock();
                                } else {
                                    block = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("item", RefTypes.ITEMSTACK, ItemStack.class.getName(),
                        """
                                if (event.getItem() != null) {
                                    item = event.getItem();
                                } else {
                                    item = null;
                                }""")
                .withMeta("nullable", true)
                .build());

        api.events().register(api.events().builder("inventory_click").by("Lumen")
                .className(InventoryClickEvent.class.getName())
                .description("Fires when a player clicks inside an inventory. Provides the clicked slot, click type, inventory title, clicked item, and cursor item.")
                .example("on inventory_click:")
                .example(of(
                        top("on inventory_click:"),
                        secondly("if name is \"main_menu\":"),
                        thirdly("cancel event")))
                .since("1.0.0")
                .category("Inventory")
                .addVar("player", RefTypes.PLAYER, Player.class.getName(),
                        """
                                if (event.getWhoClicked() instanceof Player __inv_p) {
                                    player = __inv_p;
                                } else {
                                    player = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("inventory", Inventory.class.getName(), "event.getView().getTopInventory()")
                .addVar("name", null, "String",
                        """
                                if (event.getView().getTopInventory().getHolder() instanceof net.vansencool.lumen.plugin.util.LumenInventoryHolder __lh) {
                                    name = __lh.getName();
                                } else {
                                    name = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("slot", JavaTypes.INT, "event.getSlot()")
                .addVar("rawSlot", JavaTypes.INT, "event.getRawSlot()")
                .addVar("clickType", "String", "event.getClick().name()")
                .addVar("action", "String", "event.getAction().name()")
                .addVar("title", "String", "event.getView().getTitle()")
                .addVar("item", RefTypes.ITEMSTACK, ItemStack.class.getName(),
                        """
                                if (event.getCurrentItem() != null) {
                                    item = event.getCurrentItem();
                                } else {
                                    item = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("cursor", RefTypes.ITEMSTACK, ItemStack.class.getName(),
                        """
                                if (event.getCursor() != null && event.getCursor().getType() != org.bukkit.Material.AIR) {
                                    cursor = event.getCursor();
                                } else {
                                    cursor = null;
                                }""")
                .withMeta("nullable", true)
                .build());

        api.events().register(api.events().builder("inventory_close").by("Lumen")
                .className(InventoryCloseEvent.class.getName())
                .description("Fires when a player closes an inventory.")
                .example("on inventory_close:")
                .since("1.0.0")
                .category("Inventory")
                .addVar("player", RefTypes.PLAYER, Player.class.getName(),
                        """
                                if (event.getPlayer() instanceof Player __inv_p) {
                                    player = __inv_p;
                                } else {
                                    player = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("inventory", Inventory.class.getName(), "event.getView().getTopInventory()")
                .addVar("name", null, "String",
                        """
                                if (event.getView().getTopInventory().getHolder() instanceof net.vansencool.lumen.plugin.util.LumenInventoryHolder __lh) {
                                    name = __lh.getName();
                                } else {
                                    name = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("title", "String", "event.getView().getTitle()")
                .build());

        api.events().register(api.events().builder("inventory_open").by("Lumen")
                .className(InventoryOpenEvent.class.getName())
                .description("Fires when a player opens an inventory.")
                .example("on inventory_open:")
                .since("1.0.0")
                .category("Inventory")
                .addVar("player", RefTypes.PLAYER, Player.class.getName(),
                        """
                                if (event.getPlayer() instanceof Player __inv_p) {
                                    player = __inv_p;
                                } else {
                                    player = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("inventory", Inventory.class.getName(), "event.getView().getTopInventory()")
                .addVar("name", null, "String",
                        """
                                if (event.getView().getTopInventory().getHolder() instanceof net.vansencool.lumen.plugin.util.LumenInventoryHolder __lh) {
                                    name = __lh.getName();
                                } else {
                                    name = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("title", "String", "event.getView().getTitle()")
                .build());

        api.events().register(api.events().builder("inventory_drag").by("Lumen")
                .className(InventoryDragEvent.class.getName())
                .description("Fires when a player drags items across slots in an inventory.")
                .example("on inventory_drag:")
                .since("1.0.0")
                .category("Inventory")
                .addVar("player", RefTypes.PLAYER, Player.class.getName(),
                        """
                                if (event.getWhoClicked() instanceof Player __inv_p) {
                                    player = __inv_p;
                                } else {
                                    player = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("inventory", Inventory.class.getName(), "event.getView().getTopInventory()")
                .addVar("name", null, "String",
                        """
                                if (event.getView().getTopInventory().getHolder() instanceof net.vansencool.lumen.plugin.util.LumenInventoryHolder __lh) {
                                    name = __lh.getName();
                                } else {
                                    name = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("title", "String", "event.getView().getTitle()")
                .build());
    }
}
