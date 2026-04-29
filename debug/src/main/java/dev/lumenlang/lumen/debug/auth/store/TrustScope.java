package dev.lumenlang.lumen.debug.auth.store;

/**
 * Marker for whether a stored trust grant covers only loopback peers or any peer.
 */
public enum TrustScope {

    /**
     * Trust valid only for connections originating from the same machine.
     */
    LOCAL,

    /**
     * Trust valid for any peer, including remote network addresses.
     */
    REMOTE
}
