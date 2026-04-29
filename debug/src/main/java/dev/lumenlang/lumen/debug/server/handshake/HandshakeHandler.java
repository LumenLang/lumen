package dev.lumenlang.lumen.debug.server.handshake;

import dev.lumenlang.lumen.debug.auth.pairing.AuthDecision;
import dev.lumenlang.lumen.debug.auth.pairing.PairingRequest;
import dev.lumenlang.lumen.debug.auth.policy.AuthManager;
import dev.lumenlang.lumen.debug.auth.session.SessionToken;
import dev.lumenlang.lumen.debug.log.AnsiBanner;
import dev.lumenlang.lumen.debug.protocol.DebugProtocol;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handles the {@code hello} message and sends the appropriate response.
 *
 * <p>The result tells the caller whether the connection should be promoted to active.
 */
public final class HandshakeHandler {

    private static final Logger LOG = Logger.getLogger("LumenDebug-Handshake");

    private final @NotNull AuthManager auth;

    public HandshakeHandler(@NotNull AuthManager auth) {
        this.auth = auth;
    }

    private static String stringField(@NotNull Map<String, Object> msg, @NotNull String key) {
        Object v = msg.get(key);
        return v instanceof String s ? s : null;
    }

    /**
     * Processes a parsed hello message.
     *
     * @param conn the connecting WebSocket
     * @param msg  parsed message map
     */
    public @NotNull Outcome handle(@NotNull WebSocket conn, @NotNull Map<String, Object> msg) {
        String clientId = stringField(msg, "clientId");
        String clientName = stringField(msg, "clientName");
        if (clientId == null || clientName == null) {
            conn.send(DebugProtocol.authFailed("Missing clientId or clientName"));
            return Outcome.REJECTED;
        }
        InetSocketAddress remote = conn.getRemoteSocketAddress();
        if (remote == null) {
            conn.send(DebugProtocol.authFailed("Unable to determine remote address"));
            return Outcome.REJECTED;
        }
        AuthDecision decision = auth.evaluate(clientId, clientName, remote, conn);
        return dispatch(conn, decision);
    }

    private @NotNull Outcome dispatch(@NotNull WebSocket conn, @NotNull AuthDecision decision) {
        return switch (decision.kind()) {
            case AUTHORIZED -> {
                SessionToken token = decision.token();
                if (token == null) yield Outcome.REJECTED;
                int minutes = LumenConfiguration.DEBUG.SERVICE.SESSION_TIMEOUT_MINUTES;
                conn.send(DebugProtocol.authorized(token.token(), token.clientId(), token.expiresAt(), minutes));
                yield Outcome.AUTHORIZED;
            }
            case PAIRING_REQUIRED -> {
                PairingRequest req = decision.pairing();
                if (req == null) yield Outcome.REJECTED;
                long ttl = Math.max(0, (req.expiresAt() - System.currentTimeMillis()) / 1000L);
                conn.send(DebugProtocol.pairingRequired(req.pairingId(), ttl));
                LOG.info(AnsiBanner.pairingPrompt(req.clientName(), req.clientId(), req.remote().toString(), req.code(), req.scope().name(), ttl));
                yield Outcome.AWAITING_PAIRING;
            }
            case REJECTED -> {
                String reason = decision.reason();
                conn.send(DebugProtocol.authFailed(reason != null ? reason : "Rejected"));
                yield Outcome.REJECTED;
            }
        };
    }

    /**
     * Possible results from handling a hello message.
     */
    public enum Outcome {

        /**
         * Auth succeeded and the connection should be promoted to active client.
         */
        AUTHORIZED,

        /**
         * The client must wait for operator approval; do not promote yet.
         */
        AWAITING_PAIRING,

        /**
         * The hello payload was invalid or rejected.
         */
        REJECTED
    }
}
