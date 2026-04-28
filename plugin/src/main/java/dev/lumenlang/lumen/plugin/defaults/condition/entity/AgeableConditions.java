package dev.lumenlang.lumen.plugin.defaults.condition.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import dev.lumenlang.lumen.plugin.util.EntityValidation;
import org.bukkit.entity.Ageable;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class AgeableConditions {

    private static final String FQCN = Ageable.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ag")
                .condition(
                        "%e:ENTITY% is (adult|grown|baby|young)",
                        "Checks if an ageable entity is an adult or a baby.",
                        "if mob is adult:",
                        (ctx) -> {
                            EntityValidation.requireSubtype(ctx.requireVarHandle("e"), FQCN, "is (adult|grown|baby|young)");
                            ctx.codegen().addImport(FQCN);
                            String choice = ctx.choice(0);
                            boolean adult = choice.equals("adult") || choice.equals("grown");
                            return "(" + ctx.requireVarHandle("e").java() + " instanceof Ageable _ag && " + (adult ? "" : "!") + "_ag.isAdult())";
                        });
    }
}
