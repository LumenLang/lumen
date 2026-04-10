package dev.lumenlang.lumen.plugin.defaults.condition.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess.VarHandle;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import dev.lumenlang.lumen.plugin.util.EntityValidation;
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
                        "%e:ENTITY% (can|can not|cannot) break doors",
                        0, "can",
                        "canBreakDoors()",
                        "Checks if a zombie can or cannot break doors.",
                        "if mob can break doors:")
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
