package dev.lumenlang.lumen.pipeline.java.version;

import org.jetbrains.annotations.NotNull;

/**
 * An enum representing Java language versions, with utility methods for comparison and retrieval.
 * <p>
 * Useful if you need to conditionally generate code based on the Java version being targeted, to ensure compatibility across different Java versions.
 */
@SuppressWarnings("unused")
public enum JavaVersion {
    JAVA_8(8),
    JAVA_9(9),
    JAVA_10(10),
    JAVA_11(11),
    JAVA_12(12),
    JAVA_13(13),
    JAVA_14(14),
    JAVA_15(15),
    JAVA_16(16),
    JAVA_17(17),
    JAVA_18(18),
    JAVA_19(19),
    JAVA_20(20),
    JAVA_21(21),
    JAVA_22(22),
    JAVA_23(23),
    JAVA_24(24),
    JAVA_25(25),
    JAVA_26(26),
    JAVA_27(27),
    JAVA_28(28),
    JAVA_29(29),
    JAVA_30(30),
    FUTURE(Integer.MAX_VALUE);

    private final int value;

    JavaVersion(int value) {
        this.value = value;
    }

    public static JavaVersion from(int v) {
        JavaVersion last = JAVA_8;
        for (JavaVersion j : values()) {
            if (j.value == v) return j;
            if (j.value < v) last = j;
        }
        return v > JAVA_30.value ? FUTURE : last;
    }

    public int value() {
        return value;
    }

    public boolean isGreaterThan(@NotNull JavaVersion other) {
        return this.value > other.value;
    }

    public boolean isGreaterOrEqual(@NotNull JavaVersion other) {
        return this.value >= other.value;
    }

    public boolean isLessThan(@NotNull JavaVersion other) {
        return this.value < other.value;
    }

    public boolean isLessOrEqual(@NotNull JavaVersion other) {
        return this.value <= other.value;
    }

    public boolean is(@NotNull JavaVersion other) {
        return this.value == other.value;
    }
}
