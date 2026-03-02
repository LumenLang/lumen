package net.vansencool.lumen.plugin.defaults.entity.specific;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;

/**
 * Registers villager-specific statement and expression patterns.
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers villager patterns: profession, type, level.")
@SuppressWarnings("unused")
public final class VillagerPatterns {

    private static final String FQCN = Villager.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_vl")
                .typedEnumSetter("set %e:ENTITY% profession [to] %prof:VILLAGER_PROFESSION%",
                        "setProfession", "prof",
                        "Sets a villager's profession.",
                        "set mob's profession to farmer")
                .typedEnumSetter("set %e:ENTITY% villager type [to] %type:VILLAGER_TYPE%",
                        "setVillagerType", "type",
                        "Sets a villager's biome type.",
                        "set mob's villager type to plains")
                .intSetter("set %e:ENTITY% villager level [to] %val:INT%",
                        "setVillagerLevel",
                        "Sets a villager's trading level.",
                        "set mob's villager level to 3")
                .stringGetter("[get] %e:ENTITY% profession",
                        "getProfession().name()",
                        "Returns the villager's profession name.",
                        "var prof = mob's profession")
                .intGetter("[get] %e:ENTITY% villager level",
                        "getVillagerLevel()",
                        "Returns the villager's trading level.",
                        "var lvl = mob's villager level")
                .stringGetter("[get] %e:ENTITY% villager type",
                        "getVillagerType().name()",
                        "Returns the villager's biome type name.",
                        "var vt = mob's villager type");
    }
}
