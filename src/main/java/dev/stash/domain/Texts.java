package dev.stash.domain;

import java.util.Locale;

public final class Texts {
    public static final int MAX_LABEL_LENGTH = 64;
    public static final int MAX_DISPLAY_NAME_LENGTH = 128;

    private Texts() {
    }

    public static String oneLine(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String flat = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (flat.length() > maxLength) {
            return flat.substring(0, maxLength);
        }
        return flat;
    }

    // SQLite lower() кириллицу не трогает, «ё» люди пишут и как «е».
    public static String fold(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace('ё', 'е');
    }
}
