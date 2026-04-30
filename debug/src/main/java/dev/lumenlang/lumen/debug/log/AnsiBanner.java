package dev.lumenlang.lumen.debug.log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders boxed multi-line ANSI banners used for debug security warnings and pairing prompts.
 */
public final class AnsiBanner {

    private static final String ESC = "\u001B";
    private static final String RESET = ESC + "[0m";
    private static final String BOLD = ESC + "[1m";
    private static final String DIM = ESC + "[2m";

    private static final String WARM_YELLOW = rgb(255, 211, 130);
    private static final String DEEP_AMBER = rgb(255, 168, 76);
    private static final String SOFT_PEACH = rgb(255, 224, 178);
    private static final String ALARM_RED = rgb(255, 92, 92);
    private static final String BLOOD_RED = rgb(220, 38, 38);
    private static final String GHOST_GREY = rgb(140, 140, 150);

    private static final int WIDTH = 78;

    private AnsiBanner() {
    }

    /**
     * Builds a multi-line warning banner shown when the debug service is enabled.
     *
     * @param port                the bound port number
     * @param host                the bound host
     * @param remoteAccessEnabled whether remote (non-loopback) access is allowed
     */
    public static @NotNull String enabledWarning(int port, @NotNull String host, boolean remoteAccessEnabled) {
        List<String> body = new ArrayList<>();
        if (remoteAccessEnabled) {
            body.add(centered(BOLD + ALARM_RED + "!! LUMEN DEBUG SERVICE IS ENABLED WITH REMOTE ACCESS !!" + RESET));
            body.add(blank());
            body.add(centered(BOLD + BLOOD_RED + "ARBITRARY CODE EXECUTION IS NOW POSSIBLE" + RESET));
            body.add(blank());
            body.add(left(WARM_YELLOW + "  Any connected client can:" + RESET));
            body.add(left(SOFT_PEACH + "    • run any Java snippet inside this JVM" + RESET));
            body.add(left(SOFT_PEACH + "    • read and rewrite running scripts on the fly" + RESET));
            body.add(left(SOFT_PEACH + "    • inspect every variable on the server" + RESET));
            body.add(left(SOFT_PEACH + "    • reach anything this server process can reach" + RESET));
            body.add(blank());
            body.add(left(BOLD + DEEP_AMBER + "  Treat this like a remote root shell." + RESET));
            body.add(blank());
            body.add(left(WARM_YELLOW + "  Listening on  " + RESET + BOLD + SOFT_PEACH + host + ":" + port + RESET));
            body.add(left(WARM_YELLOW + "  Disable with  " + RESET + DIM + "debug.service.enabled: false" + RESET));
            body.add(blank());
            body.add(left(GHOST_GREY + "  If you did not enable this on purpose, disable it NOW." + RESET));
            return frame(body, ALARM_RED);
        } else {
            body.add(centered(BOLD + WARM_YELLOW + "Lumen Debug Service Enabled" + RESET));
            body.add(blank());
            body.add(left(SOFT_PEACH + "  Local-only debug access on this machine." + RESET));
            body.add(blank());
            body.add(left(WARM_YELLOW + "  Listening on  " + RESET + BOLD + SOFT_PEACH + host + ":" + port + RESET));
            body.add(left(WARM_YELLOW + "  Disable with  " + RESET + DIM + "debug.service.enabled: false" + RESET));
            return frame(body, WARM_YELLOW);
        }
    }

    /**
     * Builds the banner shown to the operator when a client requests pairing.
     *
     * @param clientName human-readable client name
     * @param clientId   stable client id
     * @param remote     remote address
     * @param code       six-digit pairing code
     * @param scope      trust scope being requested
     * @param ttlSeconds seconds until this pairing request expires
     */
    public static @NotNull String pairingPrompt(@NotNull String clientName, @NotNull String clientId, @NotNull String remote, @NotNull String code, @NotNull String scope, long ttlSeconds) {
        List<String> body = new ArrayList<>();
        body.add(centered(BOLD + WARM_YELLOW + "Lumen Debug pairing requested" + RESET));
        body.add(blank());
        body.add(left(SOFT_PEACH + "  Client    " + RESET + BOLD + clientName + RESET));
        body.add(left(SOFT_PEACH + "  Id        " + RESET + DIM + clientId + RESET));
        body.add(left(SOFT_PEACH + "  Address   " + RESET + remote));
        body.add(left(SOFT_PEACH + "  Scope     " + RESET + scope));
        body.add(blank());
        body.add(centered(BOLD + DEEP_AMBER + "Code: " + code + RESET));
        body.add(blank());
        body.add(left(WARM_YELLOW + "  Approve only if you trust this device." + RESET));
        body.add(left(WARM_YELLOW + "  Run  " + RESET + BOLD + "/lumendebug pair " + code + RESET + WARM_YELLOW + "  to confirm." + RESET));
        body.add(left(GHOST_GREY + "  Expires in " + ttlSeconds + "s. Ignore to deny." + RESET));
        return frame(body, WARM_YELLOW);
    }

    private static @NotNull String frame(@NotNull List<String> body, @NotNull String borderColor) {
        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        sb.append(borderColor).append("╔").append(repeat("═", WIDTH - 2)).append("╗").append(RESET).append('\n');
        for (String line : body) {
            sb.append(borderColor).append("║").append(RESET).append(line).append(borderColor).append("║").append(RESET).append('\n');
        }
        sb.append(borderColor).append("╚").append(repeat("═", WIDTH - 2)).append("╝").append(RESET).append('\n');
        return sb.toString();
    }

    private static @NotNull String blank() {
        return repeat(" ", WIDTH - 2);
    }

    private static @NotNull String left(@NotNull String content) {
        int visible = visibleLength(content);
        int pad = Math.max(0, (WIDTH - 2) - visible);
        return content + repeat(" ", pad);
    }

    private static @NotNull String centered(@NotNull String content) {
        int visible = visibleLength(content);
        int pad = Math.max(0, (WIDTH - 2) - visible);
        int left = pad / 2;
        int right = pad - left;
        return repeat(" ", left) + content + repeat(" ", right);
    }

    private static int visibleLength(@NotNull String s) {
        int len = 0;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == 0x1b && i + 1 < s.length() && s.charAt(i + 1) == '[') {
                int end = s.indexOf('m', i);
                if (end < 0) break;
                i = end + 1;
                continue;
            }
            len++;
            i++;
        }
        return len;
    }

    private static @NotNull String repeat(@NotNull String unit, int count) {
        if (count <= 0) return "";
        return unit.repeat(count);
    }

    private static @NotNull String rgb(int r, int g, int b) {
        return ESC + "[38;2;" + r + ";" + g + ";" + b + "m";
    }
}
