package net.vansencool.lumen.plugin.defaults.condition.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.plugin.util.EntityHelper;
import net.vansencool.lumen.plugin.util.EntityValidation;
import org.bukkit.entity.Zombie;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class ZombieConditions {

    private static final String FQCN = Zombie.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_zb")
                .conditionPair(
                        "%e:ENTITY% can break doors",
                        "%e:ENTITY% (can not|cannot) break doors",
                        "canBreakDoors()",
                        "Checks if a zombie can break doors.",
                        "Checks if a zombie cannot break doors.",
                        "if mob can break doors:",
                        "if mob cannot break doors:")
                .condition(
                        "%e:ENTITY% is converting [to drowned]",
                        "Checks if a zombie is converting to a drowned.",
                        "if mob is converting to drowned:",
                        (match, env, ctx) -> {
                            VarHandle h = match.ref("e");
                            EntityValidation.requireSubtype(h, FQCN, "is converting");
                            ctx.addImport(FQCN);
                            return "(" + h.java()
                                    + " instanceof Zombie _zb && _zb.isConverting())";
                        });
    }
}
