package dev.lumenlang.lumen.plugin.defaults.placeholder;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.placeholder.PlaceholderRegistrar;
import dev.lumenlang.lumen.api.placeholder.PlaceholderType;
import dev.lumenlang.lumen.api.type.Types;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public class DefaultItemStackPlaceholders {

    @Call
    public void register(@NotNull LumenAPI api) {
        PlaceholderRegistrar ph = api.placeholders();

        ph.property(Types.ITEMSTACK, "type", "$.getType().name()");
        ph.property(Types.ITEMSTACK, "amount", "$.getAmount()", PlaceholderType.NUMBER);
        ph.property(Types.ITEMSTACK, "max_stack_size", "$.getMaxStackSize()", PlaceholderType.NUMBER);
        ph.property(Types.ITEMSTACK, "display_name",
                "($.hasItemMeta() && $.getItemMeta().hasDisplayName() ? $.getItemMeta().getDisplayName() : $.getType().name())");
        ph.property(Types.ITEMSTACK, "durability",
                "($.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable ? ((org.bukkit.inventory.meta.Damageable) $.getItemMeta()).getDamage() : 0)",
                PlaceholderType.NUMBER);
        ph.property(Types.ITEMSTACK, "max_durability", "$.getType().getMaxDurability()", PlaceholderType.NUMBER);
        ph.property(Types.ITEMSTACK, "has_meta", "$.hasItemMeta()", PlaceholderType.BOOLEAN);
        ph.property(Types.ITEMSTACK, "enchantments", "$.getEnchantments().toString()");
        ph.defaultProperty(Types.ITEMSTACK, "type");
    }
}
