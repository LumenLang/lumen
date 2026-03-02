package net.vansencool.lumen.plugin.defaults.entity.specific;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Creeper;
import org.jetbrains.annotations.NotNull;

/**
 * Registers creeper-specific statement, condition, and expression patterns.
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers creeper patterns: powered, explosion radius, fuse ticks, ignite, explode.")
@SuppressWarnings("unused")
public final class CreeperPatterns {

    private static final String FQCN = Creeper.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_cr")
                .boolSetter("set %e:ENTITY% (powered|charged) [to] %val:BOOLEAN%",
                        "setPowered",
                        "Sets whether a creeper is powered (charged).",
                        "set mob's powered to true")
                .intSetter("set %e:ENTITY% explosion radius [to] %val:INT%",
                        "setExplosionRadius",
                        "Sets the explosion radius of a creeper.",
                        "set mob's explosion radius to 5")
                .intSetter("set %e:ENTITY% max fuse ticks [to] %val:INT%",
                        "setMaxFuseTicks",
                        "Sets a creeper's maximum fuse ticks before detonation.",
                        "set mob's max fuse ticks to 40")
                .action("ignite %e:ENTITY%",
                        "ignite()",
                        "Ignites a creeper, starting its fuse countdown.",
                        "ignite mob")
                .action("explode %e:ENTITY%",
                        "explode()",
                        "Immediately detonates a creeper.",
                        "explode mob")
                .conditionPair(
                        "%e:ENTITY% is (powered|charged)",
                        "%e:ENTITY% is not (powered|charged)",
                        "isPowered()",
                        "Checks if a creeper is powered (charged).",
                        "Checks if a creeper is not powered (charged).",
                        "if mob is powered:",
                        "if mob is not powered:")
                .conditionPair(
                        "%e:ENTITY% is ignited",
                        "%e:ENTITY% is not ignited",
                        "isIgnited()",
                        "Checks if a creeper has been ignited.",
                        "Checks if a creeper has not been ignited.",
                        "if mob is ignited:",
                        "if mob is not ignited:")
                .intGetter("[get] %e:ENTITY% explosion radius",
                        "getExplosionRadius()",
                        "Returns the creeper's explosion radius.",
                        "var r = mob's explosion radius")
                .intGetter("[get] %e:ENTITY% max fuse ticks",
                        "getMaxFuseTicks()",
                        "Returns the creeper's max fuse ticks before detonation.",
                        "var fuse = mob's max fuse ticks");
    }
}
