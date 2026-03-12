package dev.lumenlang.lumen.pipeline.persist.serializer;

import dev.lumenlang.lumen.pipeline.java.compiled.DataInstance;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link ValueSerializer} that handles all value types
 * using a tagged binary format.
 *
 * <p>Each serialized value starts with a single-byte type tag, followed by the
 * type-specific payload. The format is self-describing and supports nested
 * structures (e.g. a List of Maps). The recursive design makes it reusable for
 * any storage backend that works with {@code byte[]}.
 */
// TODO: Better approach for handling Bukkit Locations and other Bukkit types
public final class LumenSerializer implements ValueSerializer {

    public static final byte TAG_UUID = 0x01;
    public static final byte TAG_LIST = 0x02;
    public static final byte TAG_MAP = 0x03;
    public static final byte TAG_DATA_INSTANCE = 0x04;
    public static final byte TAG_LOCATION = 0x05;
    public static final byte TAG_STRING = 0x10;
    public static final byte TAG_INT = 0x11;
    public static final byte TAG_DOUBLE = 0x12;
    public static final byte TAG_BOOLEAN = 0x13;
    public static final byte TAG_LONG = 0x14;
    public static final byte TAG_FLOAT = 0x15;
    public static final byte TAG_BYTES = 0x16;

    private static final String BUKKIT_LOCATION_CLASS = "org.bukkit.Location";

    private static final LumenSerializer INSTANCE = new LumenSerializer();

    private LumenSerializer() {
    }

    /**
     * Returns the singleton instance.
     *
     * @return the shared serializer instance
     */
    public static @NotNull LumenSerializer instance() {
        return INSTANCE;
    }

    private static boolean isBukkitLocation(@NotNull Object value) {
        return value.getClass().getName().equals(BUKKIT_LOCATION_CLASS);
    }

    private static void writeLocation(@NotNull DataOutputStream dos,
                                      @NotNull String worldName,
                                      double x, double y, double z) throws IOException {
        dos.writeByte(TAG_LOCATION);
        dos.writeUTF(worldName);
        dos.writeDouble(x);
        dos.writeDouble(y);
        dos.writeDouble(z);
    }

    private static void writeLocationViaReflection(@NotNull DataOutputStream dos,
                                                   @NotNull Object location) throws IOException {
        try {
            Object world = location.getClass().getMethod("getWorld").invoke(location);
            String worldName = (String) world.getClass().getMethod("getName").invoke(world);
            double x = (double) location.getClass().getMethod("getX").invoke(location);
            double y = (double) location.getClass().getMethod("getY").invoke(location);
            double z = (double) location.getClass().getMethod("getZ").invoke(location);
            writeLocation(dos, worldName, x, y, z);
        } catch (ReflectiveOperationException e) {
            dos.writeByte(TAG_STRING);
            dos.writeUTF(location.toString());
        }
    }

    @Override
    public byte @NotNull [] serialize(@NotNull Object value) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            writeTagged(dos, value);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to serialize value of type " + value.getClass().getName(), e);
        }
    }

    @Override
    public @Nullable Object deserialize(byte @NotNull [] data) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            return readTagged(dis);
        } catch (IOException e) {
            LumenLogger.severe("Failed to deserialize value", e);
            return null;
        }
    }

    @Override
    public boolean supports(@NotNull Object value) {
        return value instanceof UUID
                || value instanceof List<?>
                || value instanceof Map<?, ?>
                || value instanceof DataInstance
                || value instanceof SerializedLocation
                || value instanceof String
                || value instanceof Integer
                || value instanceof Double
                || value instanceof Boolean
                || value instanceof Long
                || value instanceof Float
                || value instanceof byte[]
                || isBukkitLocation(value);
    }

    @Override
    public boolean canDeserialize(byte @NotNull [] data) {
        if (data.length == 0) return false;
        byte tag = data[0];
        return tag == TAG_UUID || tag == TAG_LIST || tag == TAG_MAP
                || tag == TAG_STRING || tag == TAG_INT || tag == TAG_DOUBLE
                || tag == TAG_BOOLEAN || tag == TAG_LONG || tag == TAG_FLOAT
                || tag == TAG_BYTES || tag == TAG_DATA_INSTANCE
                || tag == TAG_LOCATION;
    }

    /**
     * Writes a value with its type tag to the output stream.
     *
     * @param dos   the data output stream
     * @param value the value to write
     * @throws IOException if an I/O error occurs
     */
    public void writeTagged(@NotNull DataOutputStream dos, @NotNull Object value) throws IOException {
        if (value instanceof DataInstance data) {
            dos.writeByte(TAG_DATA_INSTANCE);
            dos.writeUTF(data.type());
            Map<String, Object> fields = data.fields();
            dos.writeInt(fields.size());
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                dos.writeUTF(entry.getKey());
                byte[] valBytes = serialize(entry.getValue());
                dos.writeInt(valBytes.length);
                dos.write(valBytes);
            }
        } else if (value instanceof UUID uuid) {
            dos.writeByte(TAG_UUID);
            dos.writeLong(uuid.getMostSignificantBits());
            dos.writeLong(uuid.getLeastSignificantBits());
        } else if (value instanceof List<?> list) {
            dos.writeByte(TAG_LIST);
            dos.writeInt(list.size());
            for (Object element : list) {
                byte[] elementBytes = serialize(element);
                dos.writeInt(elementBytes.length);
                dos.write(elementBytes);
            }
        } else if (value instanceof Map<?, ?> map) {
            dos.writeByte(TAG_MAP);
            dos.writeInt(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                byte[] keyBytes = serialize(entry.getKey());
                dos.writeInt(keyBytes.length);
                dos.write(keyBytes);
                byte[] valBytes = serialize(entry.getValue());
                dos.writeInt(valBytes.length);
                dos.write(valBytes);
            }
        } else if (value instanceof String s) {
            dos.writeByte(TAG_STRING);
            dos.writeUTF(s);
        } else if (value instanceof Integer n) {
            dos.writeByte(TAG_INT);
            dos.writeInt(n);
        } else if (value instanceof Double d) {
            dos.writeByte(TAG_DOUBLE);
            dos.writeDouble(d);
        } else if (value instanceof Boolean b) {
            dos.writeByte(TAG_BOOLEAN);
            dos.writeBoolean(b);
        } else if (value instanceof Long l) {
            dos.writeByte(TAG_LONG);
            dos.writeLong(l);
        } else if (value instanceof Float f) {
            dos.writeByte(TAG_FLOAT);
            dos.writeFloat(f);
        } else if (value instanceof byte[] bytes) {
            dos.writeByte(TAG_BYTES);
            dos.writeInt(bytes.length);
            dos.write(bytes);
        } else if (value instanceof SerializedLocation loc) {
            writeLocation(dos, loc.worldName(), loc.x(), loc.y(), loc.z());
        } else if (isBukkitLocation(value)) {
            writeLocationViaReflection(dos, value);
        } else {
            dos.writeByte(TAG_STRING);
            dos.writeUTF(value.toString());
        }
    }

    /**
     * Reads a tagged value from the input stream.
     *
     * @param dis the data input stream
     * @return the deserialized value
     * @throws IOException if an I/O error occurs or the tag is unknown
     */
    public @Nullable Object readTagged(@NotNull DataInputStream dis) throws IOException {
        byte tag = dis.readByte();
        return switch (tag) {
            case TAG_DATA_INSTANCE -> {
                String typeName = dis.readUTF();
                int fieldCount = dis.readInt();
                Map<String, Object> fields = new LinkedHashMap<>(fieldCount);
                for (int i = 0; i < fieldCount; i++) {
                    String fieldName = dis.readUTF();
                    int valLen = dis.readInt();
                    byte[] valBytes = new byte[valLen];
                    dis.readFully(valBytes);
                    fields.put(fieldName, deserialize(valBytes));
                }
                yield new DataInstance(typeName, fields);
            }
            case TAG_UUID -> new UUID(dis.readLong(), dis.readLong());
            case TAG_LIST -> {
                int size = dis.readInt();
                List<Object> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    int len = dis.readInt();
                    byte[] elementBytes = new byte[len];
                    dis.readFully(elementBytes);
                    Object element = deserialize(elementBytes);
                    if (element != null) list.add(element);
                }
                yield list;
            }
            case TAG_MAP -> {
                int size = dis.readInt();
                Map<Object, Object> map = new LinkedHashMap<>(size);
                for (int i = 0; i < size; i++) {
                    int keyLen = dis.readInt();
                    byte[] keyBytes = new byte[keyLen];
                    dis.readFully(keyBytes);
                    Object key = deserialize(keyBytes);

                    int valLen = dis.readInt();
                    byte[] valBytes = new byte[valLen];
                    dis.readFully(valBytes);
                    Object val = deserialize(valBytes);

                    if (key != null) map.put(key, val);
                }
                yield map;
            }
            case TAG_STRING -> dis.readUTF();
            case TAG_INT -> dis.readInt();
            case TAG_DOUBLE -> dis.readDouble();
            case TAG_BOOLEAN -> dis.readBoolean();
            case TAG_LONG -> dis.readLong();
            case TAG_FLOAT -> dis.readFloat();
            case TAG_BYTES -> {
                int len = dis.readInt();
                byte[] bytes = new byte[len];
                dis.readFully(bytes);
                yield bytes;
            }
            case TAG_LOCATION -> {
                String worldName = dis.readUTF();
                double x = dis.readDouble();
                double y = dis.readDouble();
                double z = dis.readDouble();
                yield new SerializedLocation(worldName, x, y, z);
            }
            default -> null;
        };
    }
}
