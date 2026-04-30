package dev.lumenlang.lumen.debug.auth.pairing;

import dev.lumenlang.lumen.debug.auth.store.TrustScope;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * Pending pairing handshake awaiting user approval.
 *
 * @param pairingId  unique id sent back to the client so it can poll
 * @param code       six-digit confirmation code shown in the server console
 * @param clientId   stable identifier supplied by the connecting client
 * @param clientName human-readable name supplied by the client
 * @param remote     remote socket address of the connecting client
 * @param connection open WebSocket to notify on approval or rejection
 * @param scope      whether this pairing should grant local-only or remote trust
 * @param expiresAt  epoch milliseconds at which this pairing request expires
 */
public record PairingRequest(@NotNull String pairingId, @NotNull String code, @NotNull String clientId,
                             @NotNull String clientName, @NotNull InetSocketAddress remote,
                             @NotNull WebSocket connection, @NotNull TrustScope scope, long expiresAt) {
}
