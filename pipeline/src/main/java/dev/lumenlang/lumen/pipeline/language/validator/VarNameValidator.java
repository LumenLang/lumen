package dev.lumenlang.lumen.pipeline.language.validator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Validates variable names in Lumen scripts.
 *
 * <p>A valid variable name:
 * <ul>
 *   <li>Must start with a letter (A-Z, a-z) or digit (0-9)</li>
 *   <li>Can contain letters, digits, and underscores after the first character</li>
 *   <li>Must not start with an underscore</li>
 * </ul>
 */
public final class VarNameValidator {

    private VarNameValidator() {
    }

    /**
     * Validates a variable name and returns a human-readable error message
     * if the name is invalid, or {@code null} if the name is valid.
     *
     * @param name the variable name to validate
     * @return an error message describing why the name is invalid, or null if valid
     */
    public static @Nullable String validate(@NotNull String name) {
        if (name.isEmpty()) {
            return "Variable name must not be empty";
        }

        char first = name.charAt(0);
        if (first == '_') {
            return "Variable name '" + name + "' must not start with an underscore. "
                    + "Variable names must begin with a letter or digit";
        }

        if (!Character.isLetterOrDigit(first)) {
            return "Variable name '" + name + "' must start with a letter or digit, "
                    + "but starts with '" + first + "'";
        }

        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return "Variable name '" + name + "' contains invalid character '" + c
                        + "' at position " + (i + 1) + ". "
                        + "Only letters (A-Z, a-z), digits (0-9), and underscores are allowed";
            }
        }

        return null;
    }
}
