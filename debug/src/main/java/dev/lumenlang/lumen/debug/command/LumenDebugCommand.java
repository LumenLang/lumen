package dev.lumenlang.lumen.debug.command;

import dev.lumenlang.lumen.debug.auth.pairing.PairingRequest;
import dev.lumenlang.lumen.debug.auth.policy.AuthManager;
import dev.lumenlang.lumen.debug.auth.store.TrustedClient;
import dev.lumenlang.lumen.plugin.commands.CommandRegistry;
import dev.lumenlang.lumen.plugin.text.LumenText;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Operator-facing command for the debug service: pair, list, and revoke.
 */
public final class LumenDebugCommand implements CommandExecutor, TabCompleter {

    private static final String RED = "<#ff4a7a>";
    private static final String OK = "<#a3bdff>";
    private static final String DIM = "<#7a8aa6>";
    private static final String ACCENT = "<#ffd382>";

    private final @NotNull AuthManager auth;
    private final @NotNull Consumer<PairingRequest> onApproved;

    public LumenDebugCommand(@NotNull AuthManager auth, @NotNull Consumer<PairingRequest> onApproved) {
        this.auth = auth;
        this.onApproved = onApproved;
    }

    /**
     * Registers the command with the plugin command map.
     *
     * @param auth       active auth manager
     * @param onApproved callback fired when a pairing is approved, used to issue a token to the waiting client
     */
    public static void register(@NotNull AuthManager auth, @NotNull Consumer<PairingRequest> onApproved) {
        LumenDebugCommand cmd = new LumenDebugCommand(auth, onApproved);
        CommandRegistry.registerPluginCommand("lumendebug", List.of("ldebug"), cmd, cmd);
    }

    /**
     * Removes the command registration.
     */
    public static void unregister() {
        CommandRegistry.unregisterPluginCommand("lumendebug");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            usage(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "pair" -> pair(sender, args);
            case "list" -> list(sender);
            case "revoke" -> revoke(sender, args);
            case "pending" -> pending(sender);
            default -> {
                usage(sender);
                yield true;
            }
        };
    }

    private boolean pair(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            LumenText.send(sender, RED + "Usage: /lumendebug pair <code>");
            return true;
        }
        String code = args[1].trim();
        PairingRequest req = auth.approve(code);
        if (req == null) {
            LumenText.send(sender, RED + "No pending pairing matches that code (expired or already used)");
            return true;
        }
        onApproved.accept(req);
        LumenText.send(sender, OK + "Approved " + ACCENT + req.clientName() + OK + " (" + DIM + req.clientId() + OK + ")");
        return true;
    }

    private boolean list(@NotNull CommandSender sender) {
        List<TrustedClient> all = auth.trustedClients();
        if (all.isEmpty()) {
            LumenText.send(sender, OK + "No trusted clients");
            return true;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        LumenText.send(sender, OK + "Trusted clients (" + all.size() + "):");
        for (TrustedClient c : all) {
            String when = fmt.format(Instant.ofEpochMilli(c.approvedAt()));
            LumenText.send(sender, OK + "  " + ACCENT + c.clientName() + OK + " " + DIM + c.clientId() + OK + " " + DIM + "[" + c.scope() + "] " + when);
        }
        return true;
    }

    private boolean pending(@NotNull CommandSender sender) {
        List<PairingRequest> all = auth.pending();
        if (all.isEmpty()) {
            LumenText.send(sender, OK + "No pending pairings");
            return true;
        }
        LumenText.send(sender, OK + "Pending pairings (" + all.size() + "):");
        long now = System.currentTimeMillis();
        for (PairingRequest r : all) {
            long secs = Math.max(0, (r.expiresAt() - now) / 1000L);
            LumenText.send(sender, OK + "  " + ACCENT + r.code() + OK + " " + r.clientName() + " " + DIM + "(" + r.clientId() + ") expires in " + secs + "s");
        }
        return true;
    }

    private boolean revoke(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            LumenText.send(sender, RED + "Usage: /lumendebug revoke <clientId>");
            return true;
        }
        boolean removed = auth.revoke(args[1]);
        LumenText.send(sender, (removed ? OK + "Revoked " : RED + "No such client ") + args[1]);
        return true;
    }

    private void usage(@NotNull CommandSender sender) {
        LumenText.send(sender, RED + "Usage: /lumendebug pair <code> | list | pending | revoke <clientId>");
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        if (args.length <= 1) {
            for (String s : List.of("pair", "list", "pending", "revoke")) if (s.startsWith(last)) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("revoke")) {
            for (TrustedClient c : auth.trustedClients())
                if (c.clientId().toLowerCase().startsWith(last)) out.add(c.clientId());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("pair")) {
            for (PairingRequest r : auth.pending()) if (r.code().startsWith(last)) out.add(r.code());
        }
        Collections.sort(out);
        return out;
    }
}
