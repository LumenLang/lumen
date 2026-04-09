package dev.lumenlang.lumen.plugin.defaults.event;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.plugin.Lumen;
import dev.lumenlang.lumen.plugin.util.LumenInventoryHolder;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
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
import org.jetbrains.annotations.NotNull;

import static dev.lumenlang.lumen.api.pattern.LumaExample.of;
import static dev.lumenlang.lumen.api.pattern.LumaExample.secondly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.thirdly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.top;

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
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who joined the server")
                .build());
        api.events().register(api.events().builder("respawn").by("Lumen")
                .className(PlayerRespawnEvent.class.getName())
                .description("Fires when a player respawns after dying.")
                .example("on respawn:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(false)
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who respawned")
                .addVar("respawnLocation", MinecraftTypes.LOCATION, "event.getRespawnLocation()")
                .varDescription("The location where the player will respawn")
                .build());
        api.events().register(api.events().builder("teleport").by("Lumen")
                .className(PlayerTeleportEvent.class.getName())
                .description("Fires when a player teleports.")
                .example("on teleport:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who teleported")
                .addVar("from", MinecraftTypes.LOCATION, "event.getFrom()")
                .varDescription("The location the player teleported from")
                .addVar("to", MinecraftTypes.LOCATION, "event.getTo()")
                .varDescription("The location the player teleported to")
                .build());
        api.events().register(api.events().builder("quit").by("Lumen")
                .className(PlayerQuitEvent.class.getName())
                .description("Fires when a player leaves the server.")
                .example("on quit:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(false)
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who left the server")
                .build());
        api.events().register(api.events().builder("move").by("Lumen")
                .className(PlayerMoveEvent.class.getName())
                .description("Fires when a player moves.")
                .example("on move:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who moved")
                .addVar("from", MinecraftTypes.LOCATION, "event.getFrom()")
                .varDescription("The location the player moved from")
                .addVar("to", MinecraftTypes.LOCATION, "event.getTo()")
                .varDescription("The location the player moved to")
                .build());
        api.events().register(api.events().builder("toggle_sneak").by("Lumen")
                .className(PlayerToggleSneakEvent.class.getName())
                .description("Fires when a player starts or stops sneaking.")
                .example("on toggle_sneak:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who toggled sneaking")
                .build());
        api.events().register(api.events().builder("toggle_sprint").by("Lumen")
                .className(PlayerToggleSprintEvent.class.getName())
                .description("Fires when a player starts or stops sprinting.")
                .example("on toggle_sprint:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who toggled sprinting")
                .build());
        api.events().register(api.events().builder("toggle_flight").by("Lumen")
                .className(PlayerToggleFlightEvent.class.getName())
                .description("Fires when a player toggles flight mode.")
                .example("on toggle_flight:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who toggled flight")
                .addVar("flying", PrimitiveType.BOOLEAN, "event.isFlying()")
                .varDescription("Whether the player is now flying")
                .build());

        api.events().register(api.events().builder("entity_spawn").by("Lumen")
                .className(CreatureSpawnEvent.class.getName())
                .description("Fires when a creature spawns in the world.")
                .example("on entity_spawn:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("entity", MinecraftTypes.ENTITY, "event.getEntity()")
                .varDescription("The entity that spawned")
                .withMeta("javaClass", LivingEntity.class.getName())
                .addVar("location", MinecraftTypes.LOCATION, "event.getLocation()")
                .varDescription("The location where the entity spawned")
                .build());
        api.events().register(api.events().builder("entity_death").by("Lumen")
                .className(EntityDeathEvent.class.getName())
                .description("Fires when an entity dies.")
                .example("on entity_death:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(false)
                .addVar("entity", MinecraftTypes.ENTITY, "event.getEntity()")
                .varDescription("The entity that died")
                .withMeta("javaClass", LivingEntity.class.getName())
                .addVar("killer", MinecraftTypes.PLAYER,
                        """
                                if (event.getEntity().getKiller() != null) {
                                    killer = event.getEntity().getKiller();
                                } else {
                                    killer = null;
                                }""")
                .varDescription("The player who killed the entity, or null if not killed by a player")
                .withMeta("nullable", true)
                .build());
        api.events().register(api.events().builder("entity_damage").by("Lumen")
                .className(EntityDamageEvent.class.getName())
                .description("Fires when an entity takes damage.")
                .example("on entity_damage:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("entity", MinecraftTypes.ENTITY, "event.getEntity()")
                .varDescription("The entity that took damage")
                .addVar("damage", PrimitiveType.DOUBLE, "event.getDamage()")
                .varDescription("The amount of damage dealt")
                .build());
        api.events().register(api.events().builder("entity_damage_by_entity").by("Lumen")
                .className(EntityDamageByEntityEvent.class.getName())
                .description("Fires when an entity takes damage from another entity.")
                .example("on entity_damage_by_entity:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("entity", MinecraftTypes.ENTITY, "event.getEntity()")
                .varDescription("The entity that took damage")
                .addVar("damager", MinecraftTypes.ENTITY, "event.getDamager()")
                .varDescription("The entity that dealt the damage")
                .addVar("damagerPlayer", MinecraftTypes.PLAYER,
                        """
                                if (event.getDamager() instanceof Player __dp) {
                                    damagerPlayer = __dp;
                                } else {
                                    damagerPlayer = null;
                                }""")
                .varDescription("The player who dealt the damage, or null if the damager is not a player")
                .withMeta("nullable", true)
                .addVar("damage", PrimitiveType.DOUBLE, "event.getDamage()")
                .varDescription("The amount of damage dealt")
                .addImport(Material.class.getName())
                .addVar("item", MinecraftTypes.ITEMSTACK,
                        """
                                if (event.getDamager() instanceof Player __dw && __dw.getInventory().getItemInMainHand().getType() != Material.AIR) {
                                    item = __dw.getInventory().getItemInMainHand();
                                } else {
                                    item = null;
                                }""")
                .varDescription("The weapon the damager used, or null if the damager is not a player or has an empty hand")
                .withMeta("nullable", true)
                .build());
        api.events().register(api.events().builder("entity_interact").by("Lumen")
                .className(PlayerInteractEntityEvent.class.getName())
                .description("Fires when a player right-clicks an entity.")
                .example("on entity_interact:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who right clicked the entity")
                .addVar("entity", MinecraftTypes.ENTITY, "event.getRightClicked()")
                .varDescription("The entity that was right clicked")
                .addImport(Material.class.getName())
                .addVar("item", MinecraftTypes.ITEMSTACK,
                        """
                                if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR) {
                                    item = event.getPlayer().getInventory().getItemInMainHand();
                                } else {
                                    item = null;
                                }""")
                .varDescription("The item in the player's main hand, or null if empty")
                .withMeta("nullable", true)
                .build());

        api.events().register(api.events().builder("player_death").by("Lumen")
                .className(PlayerDeathEvent.class.getName())
                .description("Fires when a player dies.")
                .example("on player_death:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(false)
                .addVar("player", MinecraftTypes.PLAYER, "event.getEntity()")
                .varDescription("The player who died")
                .addVar("killer", MinecraftTypes.PLAYER,
                        """
                                if (event.getEntity().getKiller() != null) {
                                    killer = event.getEntity().getKiller();
                                } else {
                                    killer = null;
                                }""")
                .varDescription("The player who killed this player, or null if not killed by a player")
                .withMeta("nullable", true)
                .build());

        api.events().register(api.events().builder("block_break").by("Lumen")
                .className(BlockBreakEvent.class.getName())
                .description("Fires when a player breaks a block.")
                .example("on block_break:")
                .since("1.0.0")
                .category(Categories.BLOCK)
                .cancellable(true)
                .addImport(Material.class.getName())
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who broke the block")
                .addVar("block", MinecraftTypes.BLOCK, "event.getBlock()")
                .varDescription("The block that was broken")
                .addVar("item", MinecraftTypes.ITEMSTACK,
                        """
                                if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR) {
                                    item = event.getPlayer().getInventory().getItemInMainHand();
                                } else {
                                    item = null;
                                }""")
                .varDescription("The item the player used to break the block, or null if empty hand")
                .withMeta("nullable", true)
                .build());

        api.events().register(api.events().builder("block_place").by("Lumen")
                .className(BlockPlaceEvent.class.getName())
                .description("Fires when a player places a block.")
                .example("on block_place:")
                .since("1.0.0")
                .category(Categories.BLOCK)
                .cancellable(true)
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who placed the block")
                .addVar("block", MinecraftTypes.BLOCK, "event.getBlock()")
                .varDescription("The block that was placed")
                .addVar("item", MinecraftTypes.ITEMSTACK, "event.getItemInHand()")
                .varDescription("The item that was used to place the block")
                .build());

        api.events().register(api.events().builder("interact").by("Lumen")
                .className(PlayerInteractEvent.class.getName())
                .description("Fires when a player interacts with a block or air.")
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
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who interacted")
                .addVar("action", PrimitiveType.STRING, "event.getAction().name()")
                .varDescription("The action type: LEFT_CLICK_BLOCK, RIGHT_CLICK_BLOCK, LEFT_CLICK_AIR, RIGHT_CLICK_AIR, " +
                        "or PHYSICAL (triggering a block by stepping on or colliding with it, such as pressure plates or farmland).")
                .addVar("block", MinecraftTypes.BLOCK,
                        """
                                if (event.getClickedBlock() != null) {
                                    block = event.getClickedBlock();
                                } else {
                                    block = null;
                                }""")
                .varDescription("The block involved in the interaction, or null if the action targets air")
                .withMeta("nullable", true)
                .addVar("item", MinecraftTypes.ITEMSTACK,
                        """
                                if (event.getItem() != null) {
                                    item = event.getItem();
                                } else {
                                    item = null;
                                }""")
                .varDescription("The item in the player's hand, or null if empty")
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
                .addImport(Material.class.getName())
                .addImport(LumenInventoryHolder.class.getName())
                .addVar("player", MinecraftTypes.PLAYER,
                        """
                                if (event.getWhoClicked() instanceof Player __inv_p) {
                                    player = __inv_p;
                                }""")
                .varDescription("The player who clicked")
                .withMeta("nullable", false)
                .addVar("inventory", MinecraftTypes.INVENTORY, "event.getView().getTopInventory()")
                .varDescription("The top inventory being viewed")
                .addVar("name", PrimitiveType.STRING,
                        """
                                if (event.getView().getTopInventory().getHolder() instanceof LumenInventoryHolder __lh) {
                                    name = __lh.name();
                                } else {
                                    name = "";
                                }""")
                .varDescription("The Lumen inventory name, or empty if not a Lumen inventory")
                .withMeta("nullable", false)
                .addVar("slot", PrimitiveType.INT, "event.getSlot()")
                .varDescription("The slot index that was clicked")
                .addVar("rawSlot", PrimitiveType.INT, "event.getRawSlot()")
                .varDescription("The raw slot index including both top and bottom inventory")
                .addVar("clickType", PrimitiveType.STRING, "event.getClick().name()")
                .varDescription("The click type: LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, MIDDLE, etc.")
                .addVar("action", PrimitiveType.STRING, "event.getAction().name()")
                .varDescription("The inventory action: PICKUP_ALL, PLACE_ALL, SWAP_WITH_CURSOR, etc.")
                .addVar("title", PrimitiveType.STRING, "event.getView().getTitle()")
                .varDescription("The display title of the inventory")
                .addVar("item", MinecraftTypes.ITEMSTACK,
                        """
                                if (event.getCurrentItem() != null) {
                                    item = event.getCurrentItem();
                                } else {
                                    item = null;
                                }""")
                .varDescription("The item in the clicked slot, or null if the slot is empty")
                .withMeta("nullable", true)
                .addVar("cursor", MinecraftTypes.ITEMSTACK,
                        """
                                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                                    cursor = event.getCursor();
                                } else {
                                    cursor = null;
                                }""")
                .varDescription("The item on the cursor, or null if the cursor is empty")
                .withMeta("nullable", true)
                .build());

        api.events().register(api.events().builder("inventory_close").by("Lumen")
                .className(InventoryCloseEvent.class.getName())
                .description("Fires when a player closes an inventory.")
                .example("on inventory_close:")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .cancellable(false)
                .addVar("player", MinecraftTypes.PLAYER,
                        """
                                if (event.getPlayer() instanceof Player __inv_p) {
                                    player = __inv_p;
                                } else {
                                    player = null;
                                }""")
                .varDescription("The player who closed the inventory, or null if the viewer is not a player (unlikely)")
                .withMeta("nullable", true)
                .addImport(LumenInventoryHolder.class.getName())
                .addVar("inventory", MinecraftTypes.INVENTORY, "event.getView().getTopInventory()")
                .varDescription("The top inventory that was closed")
                .addVar("name", PrimitiveType.STRING,
                        """
                                if (event.getView().getTopInventory().getHolder() instanceof LumenInventoryHolder __lh) {
                                    name = __lh.name();
                                } else {
                                    name = null;
                                }""")
                .varDescription("The Lumen inventory name, or null if not a Lumen inventory")
                .withMeta("nullable", true)
                .addVar("title", PrimitiveType.STRING, "event.getView().getTitle()")
                .varDescription("The display title of the inventory")
                .build());

        api.events().register(api.events().builder("inventory_open").by("Lumen")
                .className(InventoryOpenEvent.class.getName())
                .description("Fires when a player opens an inventory.")
                .example("on inventory_open:")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .cancellable(true)
                .addVar("player", MinecraftTypes.PLAYER,
                        """
                                if (event.getPlayer() instanceof Player __inv_p) {
                                    player = __inv_p;
                                } else {
                                    player = null;
                                }""")
                .varDescription("The player who opened the inventory, or null if the viewer is not a player (unlikely)")
                .withMeta("nullable", true)
                .addImport(LumenInventoryHolder.class.getName())
                .addVar("inventory", MinecraftTypes.INVENTORY, "event.getView().getTopInventory()")
                .varDescription("The top inventory that was opened")
                .addVar("name", PrimitiveType.STRING,
                        """
                                if (event.getView().getTopInventory().getHolder() instanceof LumenInventoryHolder __lh) {
                                    name = __lh.name();
                                } else {
                                    name = null;
                                }""")
                .varDescription("The Lumen inventory name, or null if not a Lumen inventory")
                .withMeta("nullable", true)
                .addVar("title", PrimitiveType.STRING, "event.getView().getTitle()")
                .varDescription("The display title of the inventory")
                .build());

        api.events().register(api.events().builder("inventory_drag").by("Lumen")
                .className(InventoryDragEvent.class.getName())
                .description("Fires when a player drags items across slots in an inventory.")
                .example("on inventory_drag:")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .cancellable(true)
                .addVar("player", MinecraftTypes.PLAYER,
                        """
                                if (event.getWhoClicked() instanceof Player __inv_p) {
                                    player = __inv_p;
                                } else {
                                    player = null;
                                }""")
                .varDescription("The player who dragged items, or null if the dragger is not a player (unlikely)")
                .withMeta("nullable", true)
                .addImport(LumenInventoryHolder.class.getName())
                .addVar("inventory", MinecraftTypes.INVENTORY, "event.getView().getTopInventory()")
                .varDescription("The top inventory being viewed")
                .addVar("name", PrimitiveType.STRING,
                        """
                                if (event.getView().getTopInventory().getHolder() instanceof LumenInventoryHolder __lh) {
                                    name = __lh.name();
                                } else {
                                    name = null;
                                }""")
                .varDescription("The Lumen inventory name, or null if not a Lumen inventory")
                .withMeta("nullable", true)
                .addVar("title", PrimitiveType.STRING, "event.getView().getTitle()")
                .varDescription("The display title of the inventory")
                .build());

        api.events().advanced(b -> b
                .name("chat")
                .by("Lumen")
                .description("Fires when a player sends a chat message.")
                .example("on chat:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addImport(AsyncPlayerChatEvent.class.getName())
                .addImport(Lumen.class.getName())
                .addVar("player", MinecraftTypes.PLAYER, "player")
                .withMeta("nullable", false)
                .varDescription("The player who sent the chat message")
                .addVar("text", PrimitiveType.STRING, "text")
                .withMeta("nullable", false)
                .varDescription("The chat message content")
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

        api.events().register(api.events().builder("fish").by("Lumen")
                .className(PlayerFishEvent.class.getName())
                .description("Fires when a player uses a fishing rod.")
                .example("on fish:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .cancellable(true)
                .addVar("player", MinecraftTypes.PLAYER, "event.getPlayer()")
                .varDescription("The player who used the fishing rod")
                .addVar("entity", MinecraftTypes.ENTITY,
                        """
                                if (event.getCaught() != null) {
                                    entity = event.getCaught();
                                } else {
                                    entity = null;
                                }""")
                .varDescription("The caught entity, or null if nothing was caught")
                .withMeta("nullable", true)
                .addVar("hook", MinecraftTypes.ENTITY, "event.getHook()")
                .varDescription("The fishing hook entity")
                .addImport(Material.class.getName())
                .addVar("item", MinecraftTypes.ITEMSTACK,
                        """
                                if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR) {
                                    item = event.getPlayer().getInventory().getItemInMainHand();
                                } else {
                                    item = null;
                                }""")
                .varDescription("The fishing rod in the player's main hand, or null if empty")
                .withMeta("nullable", true)
                .addVar("state", PrimitiveType.STRING, "event.getState().name()")
                .varDescription("The fishing state: FISHING, CAUGHT_FISH, CAUGHT_ENTITY, IN_GROUND, FAILED_ATTEMPT, REEL_IN, BITE, or LURED")
                .build());

        api.events().register(api.events().builder("projectile_launch").by("Lumen")
                .className(ProjectileLaunchEvent.class.getName())
                .description("Fires when a projectile is launched (arrows, snowballs, eggs, etc.).")
                .example("on projectile_launch:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("entity", MinecraftTypes.ENTITY, "event.getEntity()")
                .varDescription("The projectile entity that was launched")
                .addVar("shooter", MinecraftTypes.PLAYER,
                        """
                                if (event.getEntity().getShooter() instanceof Player __sp) {
                                    shooter = __sp;
                                } else {
                                    shooter = null;
                                }""")
                .varDescription("The player who launched the projectile, or null if not launched by a player")
                .withMeta("nullable", true)
                .build());

        api.events().register(api.events().builder("projectile_hit").by("Lumen")
                .className(ProjectileHitEvent.class.getName())
                .description("Fires when a projectile hits a block or entity.")
                .example("on projectile_hit:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .cancellable(true)
                .addVar("entity", MinecraftTypes.ENTITY, "event.getEntity()")
                .varDescription("The projectile entity")
                .addVar("shooter", MinecraftTypes.PLAYER,
                        """
                                if (event.getEntity().getShooter() instanceof Player __sp) {
                                    shooter = __sp;
                                } else {
                                    shooter = null;
                                }""")
                .varDescription("The player who shot the projectile, or null if not shot by a player")
                .withMeta("nullable", true)
                .addVar("hit_entity", MinecraftTypes.ENTITY,
                        """
                                if (event.getHitEntity() != null) {
                                    hit_entity = event.getHitEntity();
                                } else {
                                    hit_entity = null;
                                }""")
                .varDescription("The entity that was hit, or null if a block was hit")
                .withMeta("nullable", true)
                .addVar("hit_block", MinecraftTypes.BLOCK,
                        """
                                if (event.getHitBlock() != null) {
                                    hit_block = event.getHitBlock();
                                } else {
                                    hit_block = null;
                                }""")
                .varDescription("The block that was hit, or null if an entity was hit")
                .withMeta("nullable", true)
                .build());
    }
}
