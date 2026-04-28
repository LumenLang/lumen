package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

/**
 * Registers player-related condition patterns.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class PlayerConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER_POSSESSIVE% health %op:OP% %n:INT%")
                .description("Checks if a player's health satisfies a comparison.")
                .example("if player's health >= 10:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.requireVarHandle("p").java()
                        + ".getHealth() " + ctx.java("op") + " "
                        + ctx.java("n")));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% exists")
                .description("Checks if a player reference is not null.")
                .example("if player exists:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.requireVarHandle("p").java() + " != null"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% has permission %perm:STRING%")
                .description("Checks if a player has a specific permission node.")
                .example("if player has permission \"myplugin.admin\":")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.requireVarHandle("p").java() + ".hasPermission("
                        + ctx.java("perm") + ")"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% (is|is not) sneaking")
                .description("Checks if a player is or is not currently sneaking.")
                .examples("if player is sneaking:", "if player is not sneaking:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + ctx.requireVarHandle("p").java() + ".isSneaking()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% (is|is not) sprinting")
                .description("Checks if a player is or is not currently sprinting.")
                .examples("if player is sprinting:", "if player is not sprinting:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + ctx.requireVarHandle("p").java() + ".isSprinting()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% (is|is not) [a] op")
                .description("Checks if a player has or does not have operator status.")
                .examples("if player is a op:", "if player is not op:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + ctx.requireVarHandle("p").java() + ".isOp()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% (is|is not) flying")
                .description("Checks if a player is or is not currently flying.")
                .examples("if player is flying:", "if player is not flying:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + ctx.requireVarHandle("p").java() + ".isFlying()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER_POSSESSIVE% gamemode (is|is not) %mode:GAME_MODE%")
                .description("Checks if a player's gamemode matches or does not match the given mode.")
                .examples("if player's gamemode is \"creative\":", "if player's gamemode is not \"creative\":")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return ctx.requireVarHandle("p").java() + ".getGameMode() " + (negated ? "!= " : "== ") + ctx.java("mode");
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER_POSSESSIVE% y %op:OP% %n:INT%")
                .description("Checks if a player's Y coordinate satisfies a comparison.")
                .example("if player's y >= 64:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.requireVarHandle("p").java()
                        + ".getLocation().getY() " + ctx.java("op") + " "
                        + ctx.java("n")));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% (is|is not) in world %w:STRING%")
                .description("Checks if a player is or is not in a specific world by name.")
                .examples("if player is in world \"world_nether\":", "if player is not in world \"world_nether\":")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + ctx.requireVarHandle("p").java() + ".getWorld().getName().equals(" + ctx.java("w") + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("block under %p:PLAYER% (is|is not) %mat:MATERIAL%")
                .description("Checks if the block directly below a player is or is not a specific material.")
                .examples("if block under player is diamond_block:", "if block under player is not diamond_block:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return ctx.requireVarHandle("p").java() + ".getLocation().subtract(0,1,0).getBlock().getType() " + (negated ? "!= " : "== ") + ctx.java("mat");
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% has %amt:INT% of %mat:MATERIAL%")
                .description("Checks if a player's inventory contains at least a given amount of a material.")
                .example("if player has 10 of diamond:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.requireVarHandle("p").java() + ".getInventory().contains("
                        + ctx.java("mat")
                        + ", " + ctx.java("amt") + ")"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% is (holding|carrying) %mat:MATERIAL%")
                .description("Checks if a player is holding a specific material in their main hand.")
                .example("if player is holding diamond_sword:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.requireVarHandle("p").java()
                        + ".getInventory().getItemInMainHand().getType() == "
                        + ctx.java("mat")));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% is (dead|not alive)")
                .description("Checks if a player is dead.")
                .example("if player is dead:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.requireVarHandle("p").java() + ".isDead()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER_POSSESSIVE% food [level] %op:OP% %n:INT%")
                .description("Checks if a player's food level satisfies a comparison.")
                .example("if player's food level >= 10:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.requireVarHandle("p").java()
                        + ".getFoodLevel() " + ctx.java("op") + " "
                        + ctx.java("n")));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER_POSSESSIVE% (xp level|level) %op:OP% %n:INT%")
                .description("Checks if a player's experience level satisfies a comparison.")
                .example("if player's xp level >= 30:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler(ctx -> ctx.requireVarHandle("p").java()
                        + ".getLevel() " + ctx.java("op") + " "
                        + ctx.java("n")));
    }
}
