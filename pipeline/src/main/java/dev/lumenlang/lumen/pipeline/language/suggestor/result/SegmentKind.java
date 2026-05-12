package dev.lumenlang.lumen.pipeline.language.suggestor.result;

/**
 * Display state of a {@link ShapeSegment}.
 */
public enum SegmentKind {

    /**
     * Literal already satisfied by an input token.
     */
    LITERAL_MATCHED,

    /**
     * Literal not yet typed.
     */
    LITERAL_PENDING,

    /**
     * Placeholder already filled by input tokens.
     */
    PLACEHOLDER_FILLED,

    /**
     * Placeholder not yet filled.
     */
    PLACEHOLDER_PENDING
}
