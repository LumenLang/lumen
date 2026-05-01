package dev.lumenlang.console.element;

/**
 * Axis-agnostic alignment value used by layout containers and styled text.
 */
public enum Align {

    /**
     * Pin to the start (left for rows, top for columns).
     */
    START,

    /**
     * Center within the available space.
     */
    CENTER,

    /**
     * Pin to the end (right for rows, bottom for columns).
     */
    END
}
