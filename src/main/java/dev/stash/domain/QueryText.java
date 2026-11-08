package dev.stash.domain;

public final class QueryText {
    private static final char LIKE_ESCAPE = '\\';

    private QueryText() {
    }

    public static String normalize(String raw, int maxLength) {
        String folded = Texts.fold(raw).trim();
        if (folded.length() > maxLength) {
            return folded.substring(0, maxLength);
        }
        return folded;
    }

    public static String displayPattern(String normalized) {
        return "%" + escapeLike(normalized) + "%";
    }

    public static String itemPattern(String normalized) {
        // в id предмета пробел — это подчёркивание, «gold ingot» ищет gold_ingot
        return displayPattern(normalized.replace(' ', '_'));
    }

    static String escapeLike(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == LIKE_ESCAPE || current == '%' || current == '_') {
                builder.append(LIKE_ESCAPE);
            }
            builder.append(current);
        }
        return builder.toString();
    }
}
