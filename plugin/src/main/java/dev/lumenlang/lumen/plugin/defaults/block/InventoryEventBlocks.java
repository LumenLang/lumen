package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.plugin.util.LumenInventoryHolder;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static dev.lumenlang.lumen.api.pattern.LumaExample.of;
import static dev.lumenlang.lumen.api.pattern.LumaExample.secondly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.top;

/**
 * Registers inventory event sugar blocks that simplify handling GUI interactions.
 *
 * <p>These blocks must be placed at the root level with a quoted string inventory name.
 * A dedicated Bukkit event listener method is generated for each.
 *
 * <p>Click events are automatically cancelled by default to prevent item movement, but can be uncancelled with "uncancel event".
 */
@Registration
@SuppressWarnings("unused")
public final class InventoryEventBlocks {

    private static final String INVENTORY = Inventory.class.getName();
    private static final String ITEM_STACK = ItemStack.class.getName();
    private static final String WORLD = World.class.getName();

    private static @NotNull String sanitize(@NotNull String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("^\"|\"$", "");
    }

    private static void clickImports(@NotNull CodegenAccess jctx) {
        jctx.addImport(InventoryClickEvent.class.getName());
        jctx.addImport(Material.class.getName());
        jctx.addImport(INVENTORY);
        jctx.addImport(ITEM_STACK);
        jctx.addImport(WORLD);
        jctx.addImport(LumenInventoryHolder.class.getName());
    }

    private static void closeOpenImports(@NotNull CodegenAccess jctx, @NotNull Class<?> eventClass) {
        jctx.addImport(eventClass.getName());
        jctx.addImport(INVENTORY);
        jctx.addImport(WORLD);
        jctx.addImport(LumenInventoryHolder.class.getName());
    }

    /**
     * Emits a guard that returns early if the inventory is not a Lumen inventory
     * with the expected name.
     */
    private static void emitNameGuard(@NotNull HandlerContext ctx, @NotNull String name) {
        ctx.out().line("if (!(event.getView().getTopInventory().getHolder() instanceof LumenInventoryHolder __lh)"
                + " || !__lh.name().equals(" + name + ")) return;");
    }

    /**
     * Emits all standard click event variables and defines them in the environment.
     */
    private static void emitClickVars(@NotNull HandlerContext ctx) {
        ctx.out().line("if (!(event.getWhoClicked() instanceof Player player)) return;");
        ctx.out().line("World world = player.getWorld();");
        ctx.out().line("Inventory inventory = event.getView().getTopInventory();");
        ctx.out().line("int slot = event.getRawSlot();");
        ctx.out().line("String clickType = event.getClick().name();");
        ctx.out().line("String action = event.getAction().name();");
        ctx.out().line("String title = event.getView().getTitle();");
        ctx.out().line("ItemStack item = event.getCurrentItem();");
        ctx.out().line("ItemStack cursor = (event.getCursor() != null"
                + " && event.getCursor().getType() != Material.AIR)"
                + " ? event.getCursor() : null;");

        ctx.env().defineVar("player", MinecraftTypes.PLAYER, "player");
        ctx.env().defineVar("world", MinecraftTypes.WORLD, "world");
        ctx.env().defineVar("inventory", MinecraftTypes.INVENTORY, "inventory");
        ctx.env().defineVar("slot", PrimitiveType.INT, "slot");
        ctx.env().defineVar("clickType", PrimitiveType.STRING, "clickType");
        ctx.env().defineVar("action", PrimitiveType.STRING, "action");
        ctx.env().defineVar("title", PrimitiveType.STRING, "title");
        ctx.env().defineVar("item", MinecraftTypes.ITEMSTACK, "item");
        ctx.env().defineVar("cursor", MinecraftTypes.ITEMSTACK, "cursor");
    }

    /**
     * Emits close/open event variables and defines them in the environment.
     */
    private static void emitCloseOpenVars(@NotNull HandlerContext ctx) {
        ctx.out().line("if (!(event.getPlayer() instanceof Player player)) return;");
        ctx.out().line("World world = player.getWorld();");
        ctx.out().line("Inventory inventory = event.getView().getTopInventory();");
        ctx.out().line("String title = event.getView().getTitle();");

        ctx.env().defineVar("player", MinecraftTypes.PLAYER, "player");
        ctx.env().defineVar("world", MinecraftTypes.WORLD, "world");
        ctx.env().defineVar("inventory", MinecraftTypes.INVENTORY, "inventory");
        ctx.env().defineVar("title", PrimitiveType.STRING, "title");
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        registerSlotClick(api);
        registerClick(api);
        registerClose(api);
        registerOpen(api);
    }

    private void registerSlotClick(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("slot %slot:INT% click [in|of] %inv:QSTRING%")
                .description("Handles a click on a specific slot in a Lumen inventory. "
                        + "The event is automatically cancelled. "
                        + "Provides player, inventory, slot, item, cursor, clickType, action, "
                        + "and title variables.")
                .example(of(
                        top("slot 11 click in \"main_menu\":"),
                        secondly("message player \"You clicked the info button!\"")))
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .supportsRootLevel(true)
                .supportsBlock(false)
                .addVar("player", MinecraftTypes.PLAYER)
                    .varDescription("The player who clicked")
                .addVar("world", MinecraftTypes.WORLD)
                    .varDescription("The world the player is in")
                .addVar("inventory", MinecraftTypes.INVENTORY)
                    .varDescription("The top inventory being interacted with")
                .addVar("slot", PrimitiveType.INT)
                    .varDescription("The raw slot index that was clicked")
                .addVar("clickType", PrimitiveType.STRING)
                    .varDescription("The type of click performed (e.g. LEFT, RIGHT, SHIFT_LEFT)")
                .addVar("action", PrimitiveType.STRING)
                    .varDescription("The inventory action triggered by the click")
                .addVar("title", PrimitiveType.STRING)
                    .varDescription("The title of the inventory view")
                .addVar("item", MinecraftTypes.ITEMSTACK)
                    .withMeta("nullable", true)
                    .varDescription("The item in the clicked slot, or null if empty")
                .addVar("cursor", MinecraftTypes.ITEMSTACK)
                    .withMeta("nullable", true)
                    .varDescription("The item on the cursor, or null if empty or air")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        CodegenAccess jctx = ctx.codegen();
                        String inv = ctx.java("inv");
                        String slotJava = ctx.java("slot");

                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "Inventory event blocks must be top-level (not nested inside other blocks)");
                        }
                        ctx.block().putEnv("__event_block", true);
                        ctx.block().putEnv("__event_cancellable", true);
                        String safeName = sanitize(inv);
                        String method = "__lumen_slot_click_" + safeName + "_" + ctx.codegen().nextMethodId();
                        clickImports(jctx);
                        ctx.out().line("@LumenEvent(InventoryClickEvent.class)");
                        ctx.out().line("public void " + method + "(InventoryClickEvent event) {");
                        emitNameGuard(ctx, inv);
                        ctx.out().line("if (event.getRawSlot() != " + slotJava + ") return;");
                        ctx.out().line("event.setCancelled(true);");
                        emitClickVars(ctx);
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        ctx.out().line("}");
                    }
                }));
    }

    private void registerClick(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("click [in|of] %inv:QSTRING%")
                .description("Handles any click in a Lumen inventory. "
                        + "The event is automatically cancelled. "
                        + "Provides player, inventory, slot, item, cursor, clickType, action, "
                        + "and title variables.")
                .example(of(
                        top("click in \"main_menu\":"),
                        secondly("cancel event")))
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .supportsRootLevel(true)
                .supportsBlock(false)
                .addVar("player", MinecraftTypes.PLAYER)
                    .varDescription("The player who clicked")
                .addVar("world", MinecraftTypes.WORLD)
                    .varDescription("The world the player is in")
                .addVar("inventory", MinecraftTypes.INVENTORY)
                    .varDescription("The top inventory being interacted with")
                .addVar("slot", PrimitiveType.INT)
                    .varDescription("The raw slot index that was clicked")
                .addVar("clickType", PrimitiveType.STRING)
                    .varDescription("The type of click performed (e.g. LEFT, RIGHT, SHIFT_LEFT)")
                .addVar("action", PrimitiveType.STRING)
                    .varDescription("The inventory action triggered by the click")
                .addVar("title", PrimitiveType.STRING)
                    .varDescription("The title of the inventory view")
                .addVar("item", MinecraftTypes.ITEMSTACK)
                    .withMeta("nullable", true)
                    .varDescription("The item in the clicked slot, or null if empty")
                .addVar("cursor", MinecraftTypes.ITEMSTACK)
                    .withMeta("nullable", true)
                    .varDescription("The item on the cursor, or null if empty or air")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        CodegenAccess jctx = ctx.codegen();
                        String inv = ctx.java("inv");

                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "Inventory event blocks must be top-level (not nested inside other blocks)");
                        }
                        ctx.block().putEnv("__event_block", true);
                        ctx.block().putEnv("__event_cancellable", true);
                        String safeName = sanitize(inv);
                        String method = "__lumen_click_" + safeName + "_" + ctx.codegen().nextMethodId();
                        clickImports(jctx);
                        ctx.out().line("@LumenEvent(InventoryClickEvent.class)");
                        ctx.out().line("public void " + method + "(InventoryClickEvent event) {");
                        emitNameGuard(ctx, inv);
                        ctx.out().line("event.setCancelled(true);");
                        emitClickVars(ctx);
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        ctx.out().line("}");
                    }
                }));
    }

    private void registerClose(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("close [of] %inv:QSTRING%")
                .description("Handles the close event of a Lumen inventory. "
                        + "Provides player, inventory, and title variables.")
                .example(of(
                        top("close of \"main_menu\":"),
                        secondly("message player \"Menu closed!\"")))
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .supportsRootLevel(true)
                .supportsBlock(false)
                .addVar("player", MinecraftTypes.PLAYER)
                    .varDescription("The player who closed the inventory")
                .addVar("world", MinecraftTypes.WORLD)
                    .varDescription("The world the player is in")
                .addVar("inventory", MinecraftTypes.INVENTORY)
                    .varDescription("The top inventory that was closed")
                .addVar("title", PrimitiveType.STRING)
                    .varDescription("The title of the inventory view")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        CodegenAccess jctx = ctx.codegen();
                        String inv = ctx.java("inv");

                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "Inventory event blocks must be top-level (not nested inside other blocks)");
                        }
                        ctx.block().putEnv("__event_block", true);
                        ctx.block().putEnv("__event_cancellable", false);
                        String safeName = sanitize(inv);
                        String method = "__lumen_inv_close_" + safeName + "_" + ctx.codegen().nextMethodId();
                        closeOpenImports(jctx, InventoryCloseEvent.class);
                        ctx.out().line("@LumenEvent(InventoryCloseEvent.class)");
                        ctx.out().line("public void " + method + "(InventoryCloseEvent event) {");
                        emitNameGuard(ctx, inv);
                        emitCloseOpenVars(ctx);
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        ctx.out().line("}");
                    }
                }));
    }

    private void registerOpen(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("open [of] %inv:QSTRING%")
                .description("Handles the open event of a Lumen inventory. "
                        + "Provides player, inventory, and title variables.")
                .example(of(
                        top("open of \"main_menu\":"),
                        secondly("message player \"Menu opened!\"")))
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .supportsRootLevel(true)
                .supportsBlock(false)
                .addVar("player", MinecraftTypes.PLAYER)
                    .varDescription("The player who opened the inventory")
                .addVar("world", MinecraftTypes.WORLD)
                    .varDescription("The world the player is in")
                .addVar("inventory", MinecraftTypes.INVENTORY)
                    .varDescription("The top inventory that was opened")
                .addVar("title", PrimitiveType.STRING)
                    .varDescription("The title of the inventory view")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        CodegenAccess jctx = ctx.codegen();
                        String inv = ctx.java("inv");

                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "Inventory event blocks must be top-level (not nested inside other blocks)");
                        }
                        ctx.block().putEnv("__event_block", true);
                        ctx.block().putEnv("__event_cancellable", true);
                        String safeName = sanitize(inv);
                        String method = "__lumen_inv_open_" + safeName + "_" + ctx.codegen().nextMethodId();
                        closeOpenImports(jctx, InventoryOpenEvent.class);
                        ctx.out().line("@LumenEvent(InventoryOpenEvent.class)");
                        ctx.out().line("public void " + method + "(InventoryOpenEvent event) {");
                        emitNameGuard(ctx, inv);
                        emitCloseOpenVars(ctx);
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        ctx.out().line("}");
                    }
                }));
    }
}
