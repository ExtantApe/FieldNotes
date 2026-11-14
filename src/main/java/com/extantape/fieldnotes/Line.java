package com.extantape.fieldnotes;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** One line of plain text for the files: time, then fields separated by " | ", with no line breaks inside. */
final class Line {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private Line() {}

    static String time(long millis) {
        return TIME.format(Instant.ofEpochMilli(millis));
    }

    /** Keeps a field on one line and short enough to read. */
    static String clean(String s, int max) {
        if (s == null) return "";
        String t = s.replaceAll("[\\r\\n\\t\\x00-\\x1f]+", " ").replace(" | ", " / ").strip();
        return t.length() > max ? t.substring(0, max) + "..." : t;
    }

    /** "/home extra words" becomes "/home": what was typed after a command can be private. */
    static String commandName(String message) {
        if (message == null || message.isBlank()) return "/";
        String first = message.strip().split("\\s+", 2)[0];
        return first.startsWith("/") ? first.toLowerCase(Locale.ROOT) : "/" + first.toLowerCase(Locale.ROOT);
    }

    /** The compass direction a yaw faces, as Minecraft counts it (0 is south). */
    static String facing(float yaw) {
        String[] names = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};
        int i = Math.floorMod(Math.round(yaw / 45f), 8);
        return names[i];
    }
}
