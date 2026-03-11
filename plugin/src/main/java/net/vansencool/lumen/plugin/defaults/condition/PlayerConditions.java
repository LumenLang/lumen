package net.vansencool.lumen.plugin.defaults.condition;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import org.bukkit.GameMode;
import org.jetbrains.annotations.NotNull;

/**
 * Registers player-related condition patterns.
 */
@Registration
@SuppressWarnings("unused")
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
                .handler((match, env, ctx) -> match.ref("p").java()
                        + ".getHealth() " + match.java("op", ctx, env) + " "
                        + match.java("n", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% exists")
                .description("Checks if a player reference is not null.")
                .example("if player exists:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java() + " != null"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% has permission %perm:STRING%")
                .description("Checks if a player has a specific permission node.")
                .example("if player has permission \"myplugin.admin\":")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java() + ".hasPermission("
                        + match.java("perm", ctx, env) + ")"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% is sneaking")
                .description("Checks if a player is currently sneaking.")
                .example("if player is sneaking:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java() + ".isSneaking()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% is sprinting")
                .description("Checks if a player is currently sprinting.")
                .example("if player is sprinting:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java() + ".isSprinting()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% is op")
                .description("Checks if a player has operator status.")
                .example("if player is op:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java() + ".isOp()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% is flying")
                .description("Checks if a player is currently flying.")
                .example("if player is flying:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java() + ".isFlying()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER_POSSESSIVE% gamemode is %mode:EXPR%")
                .description("Checks if a player's gamemode matches the given mode.")
                .example("if player's gamemode is \"creative\":")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> {
                    String mode = match.java("mode", ctx, env)
                            .replace("\"", "")
                            .toLowerCase();

                    String gm = switch (mode) {
                        case "survival" -> "SURVIVAL";
                        case "creative" -> "CREATIVE";
                        case "adventure" -> "ADVENTURE";
                        case "spectator" -> "SPECTATOR";
                        default -> throw new RuntimeException("Invalid gamemode: " + mode);
                    };

                    ctx.addImport(GameMode.class.getName());
                    return match.ref("p").java() + ".getGameMode() == GameMode." + gm;
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER_POSSESSIVE% y %op:OP% %n:INT%")
                .description("Checks if a player's Y coordinate satisfies a comparison.")
                .example("if player's y >= 64:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java()
                        + ".getLocation().getY() " + match.java("op", ctx, env) + " "
                        + match.java("n", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% is in world %w:STRING%")
                .description("Checks if a player is in a specific world by name.")
                .example("if player is in world \"world_nether\":")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java()
                        + ".getWorld().getName().equals(" + match.java("w", ctx, env) + ")"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("block under %p:PLAYER% is %mat:MATERIAL%")
                .description("Checks if the block directly below a player is a specific material.")
                .example("if block under player is diamond_block:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java()
                        + ".getLocation().subtract(0,1,0).getBlock().getType() == "
                        + match.java("mat", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% has %amt:INT% of %mat:MATERIAL%")
                .description("Checks if a player's inventory contains at least a given amount of a material.")
                .example("if player has 10 of diamond:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java() + ".getInventory().contains("
                        + match.java("mat", ctx, env)
                        + ", " + match.java("amt", ctx, env) + ")"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% is (holding|carrying) %mat:MATERIAL%")
                .description("Checks if a player is holding a specific material in their main hand.")
                .example("if player is holding diamond_sword:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java()
                        + ".getInventory().getItemInMainHand().getType() == "
                        + match.java("mat", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER% is (dead|not alive)")
                .description("Checks if a player is dead.")
                .example("if player is dead:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java() + ".isDead()"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER_POSSESSIVE% food [level] %op:OP% %n:INT%")
                .description("Checks if a player's food level satisfies a comparison.")
                .example("if player's food level >= 10:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java()
                        + ".getFoodLevel() " + match.java("op", ctx, env) + " "
                        + match.java("n", ctx, env)));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%p:PLAYER_POSSESSIVE% (xp level|level) %op:OP% %n:INT%")
                .description("Checks if a player's experience level satisfies a comparison.")
                .example("if player's xp level >= 30:")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .handler((match, env, ctx) -> match.ref("p").java()
                        + ".getLevel() " + match.java("op", ctx, env) + " "
                        + match.java("n", ctx, env)));
    }
}
