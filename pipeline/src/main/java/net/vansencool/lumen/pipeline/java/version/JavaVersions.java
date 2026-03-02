package net.vansencool.lumen.pipeline.java.version;

public final class JavaVersions {
    private static final JavaVersion CURRENT = detect();

    private static JavaVersion detect() {
        String v = System.getProperty("java.specification.version");
        if (v.indexOf('.') != -1)
            return JavaVersion.from(Integer.parseInt(v.substring(v.indexOf('.') + 1)));
        return JavaVersion.from(Integer.parseInt(v));
    }

    /**
     * Detects the current Java version at runtime and returns it as a {@link JavaVersion} enum constant.
     *
     * @return the detected current Java version
     */
    public static JavaVersion current() {
        return CURRENT;
    }
}
