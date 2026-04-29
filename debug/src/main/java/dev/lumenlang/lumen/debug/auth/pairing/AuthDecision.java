package dev.lumenlang.lumen.debug.auth.pairing;

import dev.lumenlang.lumen.debug.auth.session.SessionToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outcome of evaluating a hello message against the current debug policy.
 */
public final class AuthDecision {

    private final @NotNull Kind kind;
    private final @Nullable SessionToken token;
    private final @Nullable PairingRequest pairing;
    private final @Nullable String reason;

    private AuthDecision(@NotNull Kind kind, @Nullable SessionToken token, @Nullable PairingRequest pairing, @Nullable String reason) {
        this.kind = kind;
        this.token = token;
        this.pairing = pairing;
        this.reason = reason;
    }

    /**
     * Builds a successful authorization carrying the issued token.
     *
     * @param token the freshly issued session token
     */
    public static @NotNull AuthDecision authorized(@NotNull SessionToken token) {
        return new AuthDecision(Kind.AUTHORIZED, token, null, null);
    }

    /**
     * Builds a pairing-required decision carrying the pending request.
     *
     * @param req the pending pairing request
     */
    public static @NotNull AuthDecision pairingRequired(@NotNull PairingRequest req) {
        return new AuthDecision(Kind.PAIRING_REQUIRED, null, req, null);
    }

    /**
     * Builds an outright rejection.
     *
     * @param reason short description shown to the client
     */
    public static @NotNull AuthDecision rejected(@NotNull String reason) {
        return new AuthDecision(Kind.REJECTED, null, null, reason);
    }

    public @NotNull Kind kind() {
        return kind;
    }

    public @Nullable SessionToken token() {
        return token;
    }

    public @Nullable PairingRequest pairing() {
        return pairing;
    }

    public @Nullable String reason() {
        return reason;
    }

    /**
     * Possible outcomes of policy evaluation.
     */
    public enum Kind {

        /**
         * Client is trusted, a session token is attached.
         */
        AUTHORIZED,

        /**
         * Client must complete pairing before access is granted.
         */
        PAIRING_REQUIRED,

        /**
         * Connection is denied outright; reason is attached.
         */
        REJECTED
    }
}
