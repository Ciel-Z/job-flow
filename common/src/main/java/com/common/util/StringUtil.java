package com.common.util;

public class StringUtil {

    /**
     * Replace placeholders {} with the provided arguments in the template string.
     *
     * @param template The template string containing {} as placeholders.
     * @param args     The arguments to replace the placeholders with.
     * @return The formatted string with placeholders replaced by the provided arguments.
     */
    public static String formatWithPlaceholders(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        String[] parts = template.split("\\{\\}", -1);
        StringBuilder result = new StringBuilder(parts[0]);
        int i = 0;
        for (; i < args.length && i < parts.length - 1; i++) {
            result.append(args[i]).append(parts[i + 1]);
        }
        // Append the remaining parts if there are more placeholders than arguments
        for (int j = i + 1; j < parts.length; j++) {
            result.append(parts[j]);
        }
        return result.toString();
    }

}
