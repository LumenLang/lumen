package dev.lumenlang.lumen.debug.server;

import dev.lumenlang.lumen.debug.auth.pairing.PairingRequest;
import dev.lumenlang.lumen.debug.auth.policy.AuthManager;
import dev.lumenlang.lumen.debug.auth.session.SessionToken;
import dev.lumenlang.lumen.debug.hook.ScriptHooks.ConditionRecord;
import dev.lumenlang.lumen.debug.protocol.DebugProtocol;
import dev.lumenlang.lumen.debug.server.dispatch.DebugMessageRouter;
import dev.lumenlang.lumen.debug.server.handshake.HandshakeHandler;
import dev.lumenlang.lumen.debug.server.override.ScriptOverrideStore;
import dev.lumenlang.lumen.debug.session.DebugListener;
import dev.lumenlang.lumen.debug.session.DebugSession;
import dev.lumenlang.lumen.debug.transform.LineInstrumentTransformer;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket bridge for the debug protocol.
 *
 * <p>Lifecycle-only: accepts a single active client at a time, gates incoming
 * messages through {@link HandshakeHandler} until authenticated, then delegates
 * to {@link DebugMessageRouter}.
 */
public final class DebugServer extends WebSocketServer implements DebugListener {

    private static final Logger LOG = Logger.getLogger("DebugServer");

    private final @NotNull AuthManager auth;
    private final @NotNull HandshakeHandler handshake;
    private final @NotNull DebugMessageRouter router;
    private final @NotNull DebugSession session;
    private final @NotNull Set<WebSocket> awaitingPairing = ConcurrentHashMap.newKeySet();
    private volatile @Nullable WebSocket activeClient;

    /**
     * Creates a new server bound to the configured host and port.
     *
     * @param bindHost          host address to bind on
     * @param port              port to listen on
     * @param auth              authentication manager
     * @param session           session to delegate breakpoint events to
     * @param transformer       transformer used to enable/disable per-script instrumentation
     * @param recompileCallback called with (scriptName, source) when a script needs recompilation, returns a future
     */
    public DebugServer(@NotNull String bindHost, int port, @NotNull AuthManager auth, @NotNull DebugSession session, @NotNull LineInstrumentTransformer transformer, @NotNull BiFunction<String, String, CompletableFuture<?>> recompileCallback) {
        super(new InetSocketAddress(bindHost, port));
        this.auth = auth;
        this.session = session;
        this.handshake = new HandshakeHandler(auth);
        this.router = new DebugMessageRouter(session, transformer, new ScriptOverrideStore(), recompileCallback);
        this.setReuseAddr(true);
    }

    /**
     * Promotes a paired client connection to the active debug session after operator approval.
     *
     * @param req the approved pairing request
     */
    public void onPairingApproved(@NotNull PairingRequest req) {
        WebSocket conn = req.connection();
        awaitingPairing.remove(conn);
        if (!conn.isOpen()) return;
        SessionToken token = auth.issueToken(req.clientId(), conn);
        int minutes = LumenConfiguration.DEBUG.SERVICE.SESSION_TIMEOUT_MINUTES;
        conn.send(DebugProtocol.authorized(token.token(), req.clientId(), token.expiresAt(), minutes));
        adoptActive(conn);
    }

    @Override
    public void onOpen(@NotNull WebSocket conn, @NotNull ClientHandshake handshake) {
        LOG.info("Debug client connecting from " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(@NotNull WebSocket conn, int code, @NotNull String reason, boolean remote) {
        awaitingPairing.remove(conn);
        auth.clear(conn);
        if (conn == activeClient) {
            activeClient = null;
            session.listener(null);
            LOG.info("Debug client disconnected");
        }
    }

    @Override
    public void onMessage(@NotNull WebSocket conn, @NotNull String message) {
        try {
            Map<String, Object> msg = DebugProtocol.parseMessage(message);
            if (auth.authorized(conn)) {
                router.handle(conn, msg);
                return;
            }
            if (!"hello".equals(msg.get("type"))) {
                conn.send(DebugProtocol.authFailed("Send hello first"));
                return;
            }
            HandshakeHandler.Outcome outcome = handshake.handle(conn, msg);
            switch (outcome) {
                case AUTHORIZED -> adoptActive(conn);
                case AWAITING_PAIRING -> awaitingPairing.add(conn);
                case REJECTED -> conn.close(4001, "Auth failed");
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error handling message: " + message, e);
            conn.send(DebugProtocol.error(e.getMessage()));
        }
    }

    @Override
    public void onError(@Nullable WebSocket conn, @NotNull Exception ex) {
        LOG.log(Level.WARNING, "WebSocket error", ex);
    }

    @Override
    public void onStart() {
        LOG.info("Debug server started on " + getAddress());
    }

    @Override
    public void onBreakpointHit(@NotNull String script, int line, @NotNull Map<String, Object> vars, @NotNull List<ConditionRecord> conditionTrace) {
        WebSocket client = activeClient;
        if (client != null && client.isOpen())
            client.send(DebugProtocol.breakpointHit(script, line, vars, router.dumpFields(), conditionTrace));
    }

    private void adoptActive(@NotNull WebSocket conn) {
        WebSocket previous = activeClient;
        if (previous != null && previous != conn && previous.isOpen())
            previous.close(4002, "Replaced by new debug session");
        activeClient = conn;
        session.listener(this);
    }
}
