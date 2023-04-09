package org.sobiemir.codegen;

import org.openapitools.codegen.utils.StringUtils;

public class StringExtensions {
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static boolean isNullOrWhiteSpace(String value) {
        if (value == null) {
            return true;
        }

        for (int index = 0; index < value.length(); ++index) {
            if (Character.isWhitespace(value.charAt(index))) {
                return false;
            }
        }

        return true;
    }

    public static String[] lines(String value) {
        return value.split("\r\n|\r|\n");
    }

    public static String camelize(String value) {
        return StringUtils.camelize(value);
    }
}
