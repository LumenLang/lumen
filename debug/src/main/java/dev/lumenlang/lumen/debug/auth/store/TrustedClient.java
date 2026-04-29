package dev.lumenlang.lumen.debug.auth.store;

import org.jetbrains.annotations.NotNull;

/**
 * Persistent record describing a previously approved debug client.
 *
 * @param clientId   stable identifier supplied by the client
 * @param clientName human-readable name shown during pairing
 * @param approvedAt epoch milliseconds when the user approved this client
 * @param scope      whether this trust applies to local-only or also remote sessions
 */
public record TrustedClient(@NotNull String clientId, @NotNull String clientName, long approvedAt,
                            @NotNull TrustScope scope) {
}
