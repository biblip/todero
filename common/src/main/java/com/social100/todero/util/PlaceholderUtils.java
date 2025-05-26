package com.social100.todero.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderUtils {

    private PlaceholderUtils(){
    }

    // Extracts the first value inside ${...}
    public static String extractPlaceholder(String text) {
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1); // The content inside ${...}
        }
        return null;
    }

    // Replaces the first ${...} with new_value (without ${})
    public static String replacePlaceholder(String text, String newValue) {
        Pattern pattern = Pattern.compile("\\$\\{[^}]+}");
        Matcher matcher = pattern.matcher(text);
        return matcher.find()
                ? matcher.replaceFirst(Matcher.quoteReplacement(newValue))
                : text;
    }
}