package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.util.BlockFillHelper;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Registers statement patterns for block manipulation: setting type,
 * breaking blocks, setting block data, and filling cuboid regions.
 */
@Registration
@SuppressWarnings("unused")
public final class BlockStatements {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %b:BLOCK% type [to] %mat:MATERIAL%")
                .description("Sets the type (material) of a block.")
                .example("set block type to stone").since("1.0.0").category(Categories.BLOCK)
                .handler((line, ctx, out) -> out
                        .line(ctx.java("b") + ".setType(" + ctx.java("mat") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("break %b:BLOCK% naturally")
                .description("Breaks a block naturally, dropping its items as if mined by a player.")
                .example("break block naturally").since("1.0.0").category(Categories.BLOCK)
                .handler((line, ctx, out) -> out.line(ctx.java("b") + ".breakNaturally();")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %b:BLOCK% data [to] %data:EXPR%")
                .description("Sets raw block data on a block from a string representation.")
                .example("set block data to \"facing=north\"").since("1.0.0").category(Categories.BLOCK)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(Bukkit.class.getName());
                    out.line(ctx.java("b") + ".setBlockData(Bukkit.createBlockData("
                            + ctx.java("b") + ".getType(), " + ctx.java("data") + "));");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("fill (from|between) %a:LOCATION% (to|and) %b:LOCATION% with %mat:MATERIAL%")
                .description("Fills all blocks in a cuboid region between two locations with the given material.")
                .example("fill from loc1 to loc2 with stone").since("1.0.0").category(Categories.BLOCK)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(BlockFillHelper.class.getName());
                    out.line("BlockFillHelper.fill(" + ctx.java("a") + ", " + ctx.java("b")
                            + ", " + ctx.java("mat") + ");");
                }));
    }
}
