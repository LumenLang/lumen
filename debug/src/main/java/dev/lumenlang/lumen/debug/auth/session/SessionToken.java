package dev.lumenlang.lumen.debug.auth.session;

import org.jetbrains.annotations.NotNull;

/**
 * Short-lived credential issued after a successful handshake.
 *
 * @param token     opaque random string
 * @param clientId  client this token belongs to
 * @param expiresAt epoch millis when this token stops being valid
 */
public record SessionToken(@NotNull String token, @NotNull String clientId, long expiresAt) {

    /**
     * Returns whether this token has already expired against the given clock value.
     *
     * @param nowMillis current time in epoch milliseconds
     */
    public boolean expired(long nowMillis) {
        return nowMillis >= expiresAt;
    }
}
