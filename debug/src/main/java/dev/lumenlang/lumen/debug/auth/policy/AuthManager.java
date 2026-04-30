package dev.lumenlang.lumen.debug.auth.policy;

import dev.lumenlang.lumen.debug.auth.pairing.AuthDecision;
import dev.lumenlang.lumen.debug.auth.pairing.PairingRequest;
import dev.lumenlang.lumen.debug.auth.session.SessionToken;
import dev.lumenlang.lumen.debug.auth.store.TrustScope;
import dev.lumenlang.lumen.debug.auth.store.TrustStore;
import dev.lumenlang.lumen.debug.auth.store.TrustedClient;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central policy engine for the debug service.
 *
 * <p>Decides whether a connecting client is auto-authorized, must pair, or is
 * rejected. Owns pairing state, session tokens, and per-IP rate limiting.
 */
public final class AuthManager {

    private static final long PAIRING_TTL_MILLIS = 60_000L;
    private static final long RATE_WINDOW_MILLIS = 60_000L;
    private static final int RATE_LIMIT_PER_WINDOW = 5;

    private final @NotNull SecureRandom random = new SecureRandom();
    private final @NotNull TrustStore trustStore;
    private final @NotNull Map<String, PairingRequest> pendingByCode = new ConcurrentHashMap<>();
    private final @NotNull Map<String, SessionToken> tokensByConn = new ConcurrentHashMap<>();
    private final @NotNull Map<String, Deque<Long>> attemptsByIp = new ConcurrentHashMap<>();

    public AuthManager(@NotNull TrustStore trustStore) {
        this.trustStore = trustStore;
    }

    private static boolean isLoopback(@Nullable InetAddress addr) {
        return addr != null && addr.isLoopbackAddress();
    }

    private static @NotNull String connKey(@NotNull WebSocket conn) {
        return Integer.toHexString(System.identityHashCode(conn));
    }

    /**
     * Evaluates a hello message under the current policy.
     *
     * @param clientId   stable client identifier
     * @param clientName human-readable name shown during pairing
     * @param remote     address of the connecting peer
     * @param connection open WebSocket awaiting a verdict
     */
    public synchronized @NotNull AuthDecision evaluate(@NotNull String clientId, @NotNull String clientName, @NotNull InetSocketAddress remote, @NotNull WebSocket connection) {
        String ip = remote.getAddress() != null ? remote.getAddress().getHostAddress() : "unknown";
        if (rateLimited(ip)) return AuthDecision.rejected("Too many attempts; try again shortly");

        boolean local = isLoopback(remote.getAddress());
        LumenConfiguration.Debug.Service cfg = LumenConfiguration.DEBUG.SERVICE;

        if (!local && !cfg.REMOTE.ALLOW_REMOTE_ACCESS) return AuthDecision.rejected("Remote debug access disabled");
        if (local && !cfg.SAME_OS.ALLOW_SAME_OS_ACCESS) return AuthDecision.rejected("Local debug access disabled");

        TrustScope requiredScope = local ? TrustScope.LOCAL : TrustScope.REMOTE;
        TrustedClient existing = trustStore.byId(clientId);
        if (existing != null && trustGrantsAccess(existing, local))
            return AuthDecision.authorized(issueToken(clientId, connection));

        boolean pairingRequired = local ? cfg.SAME_OS.REQUIRE_PAIRING : cfg.REMOTE.REQUIRE_PAIRING;
        if (!pairingRequired) {
            trustStore.put(new TrustedClient(clientId, clientName, System.currentTimeMillis(), requiredScope));
            return AuthDecision.authorized(issueToken(clientId, connection));
        }

        PairingRequest req = newPairing(clientId, clientName, remote, connection, requiredScope);
        return AuthDecision.pairingRequired(req);
    }

    private boolean trustGrantsAccess(@NotNull TrustedClient client, boolean local) {
        if (client.scope() == TrustScope.REMOTE) return true;
        return local;
    }

    /**
     * Approves a pending pairing by its six-digit code.
     *
     * @param code six-digit confirmation code shown in console
     */
    public synchronized @Nullable PairingRequest approve(@NotNull String code) {
        purgeExpired();
        PairingRequest req = pendingByCode.remove(code);
        if (req == null) return null;
        trustStore.put(new TrustedClient(req.clientId(), req.clientName(), System.currentTimeMillis(), req.scope()));
        return req;
    }

    /**
     * Returns a snapshot of currently outstanding pairing requests.
     */
    public synchronized @NotNull List<PairingRequest> pending() {
        purgeExpired();
        return List.copyOf(pendingByCode.values());
    }

    /**
     * Issues a new session token bound to a clientId and to the given WebSocket.
     *
     * @param clientId stable client identifier
     * @param conn     the WebSocket the token applies to
     */
    public @NotNull SessionToken issueToken(@NotNull String clientId, @NotNull WebSocket conn) {
        long ttlMs = Math.max(1, LumenConfiguration.DEBUG.SERVICE.SESSION_TIMEOUT_MINUTES) * 60_000L;
        byte[] buf = new byte[32];
        random.nextBytes(buf);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        SessionToken session = new SessionToken(token, clientId, System.currentTimeMillis() + ttlMs);
        tokensByConn.put(connKey(conn), session);
        return session;
    }

    /**
     * Returns whether a WebSocket currently has a valid session token.
     *
     * @param conn the WebSocket to check
     */
    public boolean authorized(@NotNull WebSocket conn) {
        SessionToken token = tokensByConn.get(connKey(conn));
        if (token == null) return false;
        if (token.expired(System.currentTimeMillis())) {
            tokensByConn.remove(connKey(conn));
            return false;
        }
        return true;
    }

    /**
     * Drops the session token for the given WebSocket, if any.
     *
     * @param conn the WebSocket whose token should be dropped
     */
    public void clear(@NotNull WebSocket conn) {
        tokensByConn.remove(connKey(conn));
    }

    /**
     * Removes a stored trust grant.
     *
     * @param clientId client to revoke
     */
    public boolean revoke(@NotNull String clientId) {
        return trustStore.revoke(clientId);
    }

    /**
     * Returns all currently trusted clients.
     */
    public @NotNull List<TrustedClient> trustedClients() {
        return trustStore.all();
    }

    private @NotNull PairingRequest newPairing(@NotNull String clientId, @NotNull String clientName, @NotNull InetSocketAddress remote, @NotNull WebSocket connection, @NotNull TrustScope scope) {
        purgeExpired();
        String pairingId = UUID.randomUUID().toString();
        String code = generateCode();
        long expiresAt = System.currentTimeMillis() + PAIRING_TTL_MILLIS;
        PairingRequest req = new PairingRequest(pairingId, code, clientId, clientName, remote, connection, scope, expiresAt);
        pendingByCode.put(code, req);
        return req;
    }

    private @NotNull String generateCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            int n = 100000 + random.nextInt(900000);
            String code = Integer.toString(n);
            if (!pendingByCode.containsKey(code)) return code;
        }
        return Integer.toString(100000 + random.nextInt(900000));
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        pendingByCode.entrySet().removeIf(e -> now >= e.getValue().expiresAt());
        tokensByConn.entrySet().removeIf(e -> e.getValue().expired(now));
    }

    private boolean rateLimited(@NotNull String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> q = attemptsByIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > RATE_WINDOW_MILLIS) q.pollFirst();
            if (q.size() >= RATE_LIMIT_PER_WINDOW) return true;
            q.addLast(now);
            return false;
        }
    }
}
