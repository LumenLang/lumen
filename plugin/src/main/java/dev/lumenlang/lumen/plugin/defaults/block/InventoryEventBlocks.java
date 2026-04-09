package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
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
    private static void emitNameGuard(@NotNull JavaOutput out, @NotNull String name) {
        out.line("if (!(event.getView().getTopInventory().getHolder() instanceof LumenInventoryHolder __lh)"
                + " || !__lh.name().equals(" + name + ")) return;");
    }

    /**
     * Emits all standard click event variables and defines them in the environment.
     */
    private static void emitClickVars(
            @NotNull EnvironmentAccess env,
            @NotNull BindingAccess ctx,
            @NotNull JavaOutput out) {
        out.line("if (!(event.getWhoClicked() instanceof Player player)) return;");
        out.line("World world = player.getWorld();");
        out.line("Inventory inventory = event.getView().getTopInventory();");
        out.line("int slot = event.getRawSlot();");
        out.line("String clickType = event.getClick().name();");
        out.line("String action = event.getAction().name();");
        out.line("String title = event.getView().getTitle();");
        out.line("ItemStack item = event.getCurrentItem();");
        out.line("ItemStack cursor = (event.getCursor() != null"
                + " && event.getCursor().getType() != Material.AIR)"
                + " ? event.getCursor() : null;");

        env.defineVar("player", MinecraftTypes.PLAYER, "player");
        env.defineVar("world", MinecraftTypes.WORLD, "world");
        env.defineVar("inventory", MinecraftTypes.INVENTORY, "inventory");
        env.defineVar("slot", null, "slot");
        env.defineVar("clickType", null, "clickType");
        env.defineVar("action", null, "action");
        env.defineVar("title", null, "title");
        env.defineVar("item", MinecraftTypes.ITEMSTACK, "item");
        env.defineVar("cursor", MinecraftTypes.ITEMSTACK, "cursor");
    }

    /**
     * Emits close/open event variables and defines them in the environment.
     */
    private static void emitCloseOpenVars(
            @NotNull EnvironmentAccess env,
            @NotNull BindingAccess ctx,
            @NotNull JavaOutput out) {
        out.line("if (!(event.getPlayer() instanceof Player player)) return;");
        out.line("World world = player.getWorld();");
        out.line("Inventory inventory = event.getView().getTopInventory();");
        out.line("String title = event.getView().getTitle();");

        env.defineVar("player", MinecraftTypes.PLAYER, "player");
        env.defineVar("world", MinecraftTypes.WORLD, "world");
        env.defineVar("inventory", MinecraftTypes.INVENTORY, "inventory");
        env.defineVar("title", null, "title");
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
                .pattern("slot %slot:INT% click [in|of] %inv:EXPR%")
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
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        EnvironmentAccess env = ctx.env();
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
                        String method = "__lumen_slot_click_" + safeName + "_" + out.lineNum();
                        clickImports(jctx);
                        out.line("@LumenEvent(InventoryClickEvent.class)");
                        out.line("public void " + method + "(InventoryClickEvent event) {");
                        emitNameGuard(out, inv);
                        out.line("if (event.getRawSlot() != " + slotJava + ") return;");
                        out.line("event.setCancelled(true);");
                        emitClickVars(env, ctx, out);
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }

    private void registerClick(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("click [in|of] %inv:EXPR%")
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
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        EnvironmentAccess env = ctx.env();
                        CodegenAccess jctx = ctx.codegen();
                        String inv = ctx.java("inv");

                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "Inventory event blocks must be top-level (not nested inside other blocks)");
                        }
                        ctx.block().putEnv("__event_block", true);
                        ctx.block().putEnv("__event_cancellable", true);
                        String safeName = sanitize(inv);
                        String method = "__lumen_click_" + safeName + "_" + out.lineNum();
                        clickImports(jctx);
                        out.line("@LumenEvent(InventoryClickEvent.class)");
                        out.line("public void " + method + "(InventoryClickEvent event) {");
                        emitNameGuard(out, inv);
                        out.line("event.setCancelled(true);");
                        emitClickVars(env, ctx, out);
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }

    private void registerClose(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("close [of] %inv:EXPR%")
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
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        EnvironmentAccess env = ctx.env();
                        CodegenAccess jctx = ctx.codegen();
                        String inv = ctx.java("inv");

                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "Inventory event blocks must be top-level (not nested inside other blocks)");
                        }
                        ctx.block().putEnv("__event_block", true);
                        ctx.block().putEnv("__event_cancellable", false);
                        String safeName = sanitize(inv);
                        String method = "__lumen_inv_close_" + safeName + "_" + out.lineNum();
                        closeOpenImports(jctx, InventoryCloseEvent.class);
                        out.line("@LumenEvent(InventoryCloseEvent.class)");
                        out.line("public void " + method + "(InventoryCloseEvent event) {");
                        emitNameGuard(out, inv);
                        emitCloseOpenVars(env, ctx, out);
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }

    private void registerOpen(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("open [of] %inv:EXPR%")
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
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        EnvironmentAccess env = ctx.env();
                        CodegenAccess jctx = ctx.codegen();
                        String inv = ctx.java("inv");

                        if (!ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "Inventory event blocks must be top-level (not nested inside other blocks)");
                        }
                        ctx.block().putEnv("__event_block", true);
                        ctx.block().putEnv("__event_cancellable", true);
                        String safeName = sanitize(inv);
                        String method = "__lumen_inv_open_" + safeName + "_" + out.lineNum();
                        closeOpenImports(jctx, InventoryOpenEvent.class);
                        out.line("@LumenEvent(InventoryOpenEvent.class)");
                        out.line("public void " + method + "(InventoryOpenEvent event) {");
                        emitNameGuard(out, inv);
                        emitCloseOpenVars(env, ctx, out);
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }
}
