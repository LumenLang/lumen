package net.vansencool.lumen.plugin.defaults.event;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.BindingAccess;
import net.vansencool.lumen.api.codegen.JavaOutput;
import net.vansencool.lumen.api.handler.BlockHandler;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.Types;
import net.vansencool.lumen.plugin.Lumen;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerFishEvent;
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
@SuppressWarnings("unused")
public final class DefaultEvents {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.events().register(api.events().builder("join").by("Lumen")
                .className(PlayerJoinEvent.class.getName())
                .description("Fires when a player joins the server.")
                .example("on join:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(false)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .build());
        api.events().register(api.events().builder("respawn").by("Lumen")
                .className(PlayerRespawnEvent.class.getName())
                .description("Fires when a player respawns after dying.")
                .example("on respawn:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(false)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .addVar("respawnLocation", Types.LOCATION, "event.getRespawnLocation()")
                .build());
        api.events().register(api.events().builder("teleport").by("Lumen")
                .className(PlayerTeleportEvent.class.getName())
                .description("Fires when a player teleports.")
                .example("on teleport:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .addVar("from", Types.LOCATION, "event.getFrom()")
                .addVar("to", Types.LOCATION, "event.getTo()")
                .build());
        api.events().register(api.events().builder("quit").by("Lumen")
                .className(PlayerQuitEvent.class.getName())
                .description("Fires when a player leaves the server.")
                .example("on quit:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(false)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .build());
        api.events().register(api.events().builder("move").by("Lumen")
                .className(PlayerMoveEvent.class.getName())
                .description("Fires when a player moves.")
                .example("on move:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .addVar("from", Types.LOCATION, "event.getFrom()")
                .addVar("to", Types.LOCATION, "event.getTo()")
                .build());
        api.events().register(api.events().builder("toggle_sneak").by("Lumen")
                .className(PlayerToggleSneakEvent.class.getName())
                .description("Fires when a player starts or stops sneaking.")
                .example("on toggle_sneak:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .build());
        api.events().register(api.events().builder("toggle_sprint").by("Lumen")
                .className(PlayerToggleSprintEvent.class.getName())
                .description("Fires when a player starts or stops sprinting.")
                .example("on toggle_sprint:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .build());
        api.events().register(api.events().builder("toggle_flight").by("Lumen")
                .className(PlayerToggleFlightEvent.class.getName())
                .description("Fires when a player toggles flight mode.")
                .example("on toggle_flight:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .addVar("flying", Types.BOOLEAN, "event.isFlying()")
                .build());

        api.events().register(api.events().builder("entity_spawn").by("Lumen")
                .className(CreatureSpawnEvent.class.getName())
                .description("Fires when a creature spawns in the world.")
                .example("on entity_spawn:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("entity", Types.ENTITY, "event.getEntity()")
                .withMeta("javaClass", LivingEntity.class.getName())
                .addVar("location", Types.LOCATION, "event.getLocation()")
                .build());
        api.events().register(api.events().builder("entity_death").by("Lumen")
                .className(EntityDeathEvent.class.getName())
                .description("Fires when an entity dies.")
                .example("on entity_death:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(false)
                .addVar("entity", Types.ENTITY, "event.getEntity()")
                .withMeta("javaClass", LivingEntity.class.getName())
                .addVar("killer", Types.PLAYER, Player.class.getName(),
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
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("entity", Types.ENTITY, "event.getEntity()")
                .addVar("damage", Types.DOUBLE, "event.getDamage()")
                .build());
        api.events().register(api.events().builder("entity_damage_by_entity").by("Lumen")
                .className(EntityDamageByEntityEvent.class.getName())
                .description("Fires when an entity takes damage from another entity.")
                .example("on entity_damage_by_entity:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("entity", Types.ENTITY, "event.getEntity()")
                .addVar("damager", Types.ENTITY, "event.getDamager()")
                .addVar("damagerPlayer", Types.PLAYER, Player.class.getName(),
                        """
                                if (event.getDamager() instanceof Player __dp) {
                                    damagerPlayer = __dp;
                                } else {
                                    damagerPlayer = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("damage", Types.DOUBLE, "event.getDamage()")
                .build());
        api.events().register(api.events().builder("entity_interact").by("Lumen")
                .className(PlayerInteractEntityEvent.class.getName())
                .description("Fires when a player right-clicks an entity.")
                .example("on entity_interact:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .addVar("entity", Types.ENTITY, "event.getRightClicked()")
                .build());

        api.events().register(api.events().builder("player_death").by("Lumen")
                .className(PlayerDeathEvent.class.getName())
                .description("Fires when a player dies.")
                .example("on player_death:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(false)
                .addVar("player", Types.PLAYER, "event.getEntity()")
                .addVar("killer", Types.PLAYER, Player.class.getName(),
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
                .category(Categories.BLOCK)
                .cancellable(true)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .addVar("block", Types.BLOCK, "event.getBlock()")
                .addVar("item", Types.ITEMSTACK, ItemStack.class.getName(),
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
                .category(Categories.BLOCK)
                .cancellable(true)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .addVar("block", Types.BLOCK, "event.getBlock()")
                .addVar("item", Types.ITEMSTACK, "event.getItemInHand()")
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
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .addVar("action", "String", "event.getAction().name()")
                .addVar("block", Types.BLOCK, Block.class.getName(),
                        """
                                if (event.getClickedBlock() != null) {
                                    block = event.getClickedBlock();
                                } else {
                                    block = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("item", Types.ITEMSTACK, ItemStack.class.getName(),
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
                .category(Categories.INVENTORY)
                .cancellable(true)
                .addVar("player", Types.PLAYER, Player.class.getName(),
                        """
                                if (event.getWhoClicked() instanceof Player __inv_p) {
                                    player = __inv_p;
                                } else {
                                    player = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("inventory", Inventory.class.getName(), "event.getView().getTopInventory()")
                .addVar("name", null, Types.STRING,
                        """
                                if (event.getView().getTopInventory().getHolder() instanceof net.vansencool.lumen.plugin.util.LumenInventoryHolder __lh) {
                                    name = __lh.getName();
                                } else {
                                    name = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("slot", Types.INT, "event.getSlot()")
                .addVar("rawSlot", Types.INT, "event.getRawSlot()")
                .addVar("clickType", "String", "event.getClick().name()")
                .addVar("action", "String", "event.getAction().name()")
                .addVar("title", "String", "event.getView().getTitle()")
                .addVar("item", Types.ITEMSTACK, ItemStack.class.getName(),
                        """
                                if (event.getCurrentItem() != null) {
                                    item = event.getCurrentItem();
                                } else {
                                    item = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("cursor", Types.ITEMSTACK, ItemStack.class.getName(),
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
                .category(Categories.INVENTORY)
                .cancellable(false)
                .addVar("player", Types.PLAYER, Player.class.getName(),
                        """
                                if (event.getPlayer() instanceof Player __inv_p) {
                                    player = __inv_p;
                                } else {
                                    player = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("inventory", Inventory.class.getName(), "event.getView().getTopInventory()")
                .addVar("name", null, Types.STRING,
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
                .category(Categories.INVENTORY)
                .cancellable(true)
                .addVar("player", Types.PLAYER, Player.class.getName(),
                        """
                                if (event.getPlayer() instanceof Player __inv_p) {
                                    player = __inv_p;
                                } else {
                                    player = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("inventory", Inventory.class.getName(), "event.getView().getTopInventory()")
                .addVar("name", null, Types.STRING,
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
                .category(Categories.INVENTORY)
                .cancellable(true)
                .addVar("player", Types.PLAYER, Player.class.getName(),
                        """
                                if (event.getWhoClicked() instanceof Player __inv_p) {
                                    player = __inv_p;
                                } else {
                                    player = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("inventory", Inventory.class.getName(), "event.getView().getTopInventory()")
                .addVar("name", null, Types.STRING,
                        """
                                if (event.getView().getTopInventory().getHolder() instanceof net.vansencool.lumen.plugin.util.LumenInventoryHolder __lh) {
                                    name = __lh.getName();
                                } else {
                                    name = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("title", "String", "event.getView().getTitle()")
                .build());

        api.events().advanced(b -> b
                .name("chat")
                .by("Lumen")
                .description("Fires when a player sends a chat message. The handler body runs on the main server thread, so world-modifying actions are safe. Use 'async_chat' if you need the raw async event.")
                .example("on chat:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(false)
                .addImport(AsyncPlayerChatEvent.class.getName())
                .addImport(Lumen.class.getName())
                .addVar("player", Types.PLAYER, "player")
                .addVar("text", "String", "text")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("@LumenEvent(AsyncPlayerChatEvent.class)");
                        out.line("public void __lumen_evt_chat_" + out.lineNum() + "(AsyncPlayerChatEvent event) {");
                        out.line("final Player __chat_player = event.getPlayer();");
                        out.line("final String __chat_text = event.getMessage();");
                        out.line("Bukkit.getScheduler().runTask(Lumen.instance(), () -> {");
                        out.line("Player player = __chat_player;");
                        out.line("String text = __chat_text;");
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("});");
                        out.line("}");
                    }
                }));

        api.events().register(api.events().builder("async_chat").by("Lumen")
                .className(AsyncPlayerChatEvent.class.getName())
                .description("Fires when a player sends a chat message. Runs asynchronously on the chat thread. Use 'after 1 ticks:' to schedule world-modifying actions, or prefer 'chat' for main-thread safety.")
                .example("on async_chat:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .addVar("text", "String", "event.getMessage()")
                .build());

        api.events().register(api.events().builder("fish").by("Lumen")
                .className(PlayerFishEvent.class.getName())
                .description("Fires when a player uses a fishing rod. The 'state' variable contains the fishing state as a string: FISHING, CAUGHT_FISH, CAUGHT_ENTITY, IN_GROUND, FAILED_ATTEMPT, REEL_IN, BITE, or LURED.")
                .example("on fish:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", Types.PLAYER, "event.getPlayer()")
                .addVar("entity", Types.ENTITY, Entity.class.getName(),
                        """
                                if (event.getCaught() != null) {
                                    entity = event.getCaught();
                                } else {
                                    entity = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("hook", Types.ENTITY, "event.getHook()")
                .addVar("state", "String", "event.getState().name()")
                .build());

        api.events().register(api.events().builder("projectile_launch").by("Lumen")
                .className(ProjectileLaunchEvent.class.getName())
                .description("Fires when a projectile is launched (arrows, snowballs, eggs, etc.).")
                .example("on projectile_launch:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("entity", Types.ENTITY, "event.getEntity()")
                .addVar("shooter", Types.PLAYER, Player.class.getName(),
                        """
                                if (event.getEntity().getShooter() instanceof Player __sp) {
                                    shooter = __sp;
                                } else {
                                    shooter = null;
                                }""")
                .withMeta("nullable", true)
                .build());

        api.events().register(api.events().builder("projectile_hit").by("Lumen")
                .className(ProjectileHitEvent.class.getName())
                .description("Fires when a projectile hits a block or entity.")
                .example("on projectile_hit:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("entity", Types.ENTITY, "event.getEntity()")
                .addVar("shooter", Types.PLAYER, Player.class.getName(),
                        """
                                if (event.getEntity().getShooter() instanceof Player __sp) {
                                    shooter = __sp;
                                } else {
                                    shooter = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("hit_entity", Types.ENTITY, Entity.class.getName(),
                        """
                                if (event.getHitEntity() != null) {
                                    hit_entity = event.getHitEntity();
                                } else {
                                    hit_entity = null;
                                }""")
                .withMeta("nullable", true)
                .addVar("hit_block", Types.BLOCK, Block.class.getName(),
                        """
                                if (event.getHitBlock() != null) {
                                    hit_block = event.getHitBlock();
                                } else {
                                    hit_block = null;
                                }""")
                .withMeta("nullable", true)
                .build());
    }
}
