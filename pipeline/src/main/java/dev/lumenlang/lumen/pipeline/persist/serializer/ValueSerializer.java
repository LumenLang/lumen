package dev.lumenlang.lumen.pipeline.persist.serializer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts arbitrary values to and from byte arrays for persistent storage.
 *
 * <p>Implementations wrap values into a tagged binary format so that backends only
 * need to store {@code byte[]} and can delegate all encoding/decoding to this layer.
 *
 * @see LumenSerializer
 */
public interface ValueSerializer {

    /**
     * Serializes a value into a byte array.
     *
     * @param value the value to serialize
     * @return the serialized bytes
     * @throws IllegalArgumentException if the value type is not supported
     */
    byte @NotNull [] serialize(@NotNull Object value);

    /**
     * Deserializes a byte array back into the original value.
     *
     * @param data the serialized bytes
     * @return the deserialized value, or {@code null} if the data is malformed
     */
    @Nullable Object deserialize(byte @NotNull [] data);

    /**
     * Returns whether this serializer can handle the given value type.
     *
     * @param value the value to check
     * @return {@code true} if the value can be serialized
     */
    boolean supports(@NotNull Object value);

    /**
     * Returns whether the given byte array was produced by this serializer,
     * based on its tag prefix.
     *
     * @param data the byte array to check
     * @return {@code true} if this serializer can deserialize the data
     */
    boolean canDeserialize(byte @NotNull [] data);
}
