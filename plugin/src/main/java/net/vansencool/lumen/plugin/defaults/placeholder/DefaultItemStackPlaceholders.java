package net.vansencool.lumen.plugin.defaults.placeholder;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.placeholder.PlaceholderRegistrar;
import net.vansencool.lumen.api.placeholder.PlaceholderType;
import net.vansencool.lumen.api.type.RefTypes;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public class DefaultItemStackPlaceholders {

    @Call
    public void register(@NotNull LumenAPI api) {
        PlaceholderRegistrar ph = api.placeholders();

        ph.property(RefTypes.ITEMSTACK, "type", "$.getType().name()");
        ph.property(RefTypes.ITEMSTACK, "amount", "$.getAmount()", PlaceholderType.NUMBER);
        ph.property(RefTypes.ITEMSTACK, "max_stack_size", "$.getMaxStackSize()", PlaceholderType.NUMBER);
        ph.property(RefTypes.ITEMSTACK, "display_name",
                "($.hasItemMeta() && $.getItemMeta().hasDisplayName() ? $.getItemMeta().getDisplayName() : $.getType().name())");
        ph.property(RefTypes.ITEMSTACK, "durability",
                "($.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable ? ((org.bukkit.inventory.meta.Damageable) $.getItemMeta()).getDamage() : 0)",
                PlaceholderType.NUMBER);
        ph.property(RefTypes.ITEMSTACK, "max_durability", "$.getType().getMaxDurability()", PlaceholderType.NUMBER);
        ph.property(RefTypes.ITEMSTACK, "has_meta", "$.hasItemMeta()", PlaceholderType.BOOLEAN);
        ph.property(RefTypes.ITEMSTACK, "enchantments", "$.getEnchantments().toString()");
        ph.defaultProperty(RefTypes.ITEMSTACK, "type");
    }
}
