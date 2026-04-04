package dev.lumenlang.lumen.plugin.defaults;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.inject.Fakes;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.Types;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Registration
public class MyStatements {
    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().injectable("greet %who:PLAYER%", () -> {
            Player player = Fakes.fake("who");
            player.sendMessage("Hello " + player.getName() + "!");
        });

        api.patterns().injectable("teleport %who:PLAYER% to %target:PLAYER%", () -> {
            Player who = Fakes.fake("who");
            Player target = Fakes.fake("target");
            who.teleport(target.getLocation());
            who.sendMessage("Teleported to " + target.getName());
        });

        api.patterns().injectable("announce %msg:STRING%", () -> {
            String msg = Fakes.fakeString("msg");
            Bukkit.broadcastMessage("[Announcement] " + msg.toUpperCase());
        });

        api.patterns().injectable("heal %who:PLAYER% by %amount:INT%", () -> {
            Player player = Fakes.fake("who");
            int amount = Fakes.fakeInt("amount");
            player.setHealth(Math.min(player.getHealth() + amount, 20.0));
            player.sendMessage("Healed by " + amount + "!");
        });

        api.patterns().injectable("build wall at %loc:LOCATION%", () -> {
            Location loc = Fakes.fake("loc");
            for (int y = 0; y < 5; y++) {
                for (int x = -2; x <= 2; x++) {
                    loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ()).setType(Material.STONE);
                }
            }
        });

        api.patterns().injectable("ignite %who:PLAYER% for %seconds:INT%", () -> {
            Player player = Fakes.fake("who");
            int seconds = Fakes.fakeInt("seconds");
            player.setFireTicks(seconds * 20);
            player.sendMessage("You are on fire for " + seconds + " seconds!");
        });

        api.patterns().injectable("igniteeee %who:PLAYER% for %seconds:INT%", () -> {
            Player player = Fakes.fake("who");
            int seconds = Fakes.fakeInt("seconds");
            player.setFireTicks(seconds * 20);
            player.sendMessage("You are on fire for " + seconds + " seconds!");
            try {
                player.sendMessage("Hii");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            });
        api.patterns().statement(b -> b
            .by("Lumen")
            .pattern("boost %who:PLAYER% by %amount:DOUBLE%")
            .description("Boosts a player upward by the given velocity.")
            .example("boost player by 2.0")
            .since("1.0.0")
            .category(Categories.PLAYER)
            .injectableHandler(() -> {
                Player player = Fakes.fake("who");
                double amount = Fakes.fakeDouble("amount");
                player.setVelocity(player.getVelocity().setY(amount));
                player.sendMessage("Boosted!");
            })
        );

        api.patterns().expression(b -> b
            .by("Lumen")
            .pattern("location of %who:PLAYER%")
            .description("Returns the current location of a player.")
            .example("var loc = location of player")
            .since("1.0.0")
            .category(Categories.PLAYER)
            .returnRefTypeId(Types.LOCATION.id())
            .injectableHandler(() -> {
                Player player = Fakes.fake("who");
                player.sendMessage("...");
                return player.getLocation();
            })
        );

        api.patterns().expression(b -> b
            .by("Lumen")
            .pattern("healthss of %who:PLAYER%")
            .description("Returns the health of a player as a double.")
            .example("var hp = health of player")
            .since("1.0.0")
            .category(Categories.PLAYER)
            .returnJavaType(Types.DOUBLE)
            .injectableHandler(() -> {
                Player player = Fakes.fake("who");

                player.sendMessage("...");
                return player.getHealth();
            })
        );

        api.patterns().condition(b -> b
            .by("Lumen")
            .pattern("%p:PLAYER% is swimming")
            .description("Checks whether a player is currently swimming.")
            .example("if player is swimming:")
            .since("1.0.0")
            .category(Categories.PLAYER)
            .injectableHandler(() -> {
                Player player = Fakes.fake("p");

                player.sendMessage("...");
                return player.isSwimming();
            })
        );

        api.patterns().condition(b -> b
            .by("Lumen")
            .pattern("%p:PLAYER% has more than %hp:INT% healthss")
            .description("Checks whether a player has more than the given health.")
            .example("if player has more than 10 health:")
            .since("1.0.0")
            .category(Categories.PLAYER)
            .injectableHandler(() -> {
                Player player = Fakes.fake("p");
                int hp = Fakes.fakeInt("hp");
                return player.getHealth() > hp;
            })
        );

        api.patterns().injectableExpression("world of %who:PLAYER%", Types.WORLD.id(), null, () -> {
            Player player = Fakes.fake("who");
            return player.getWorld();
        });

        api.patterns().injectableCondition("%p:PLAYER% is on fires", () -> {
            Player player = Fakes.fake("p");
            player.sendMessage("...");
            for (int i = 2;i < 2002;i++) {
                player.sendMessage("Tick " + i + " :)");
            }
            return player.getFireTicks() > 0;
        });

        api.patterns().statement(b -> b
            .by("Lumen")
            .pattern("launch %who:PLAYER% with message %msg:STRING%")
            .description("Launches a player upward and sends a formatted message.")
            .example("launch player with message \"Up you go!\"")
            .since("1.0.0")
            .category(Categories.PLAYER)
            .injectableHandler(MyStatements.class, "launchWithMessage")
        );

        api.patterns().expression(b -> b
            .by("Lumen")
            .pattern("display name of %who:PLAYER%")
            .description("Returns the display name of a player.")
            .example("var name = display name of player")
            .since("1.0.0")
            .category(Categories.PLAYER)
            .returnJavaType(Types.STRING)
            .injectableHandler(MyStatements.class, "displayName")
        );
    }

    private static void launchWithMessage() {
        Player player = Fakes.fake("who");
        String msg = Fakes.fakeString("msg");
        player.setVelocity(player.getVelocity().setY(1.5));
        player.sendMessage("[Launch] " + msg);
    }

    private static Object displayName() {
        Player player = Fakes.fake("who");
        return player.getDisplayName();
    }
}