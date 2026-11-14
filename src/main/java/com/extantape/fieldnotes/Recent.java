package com.extantape.fieldnotes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** A player's last few actions, oldest first. Thread-safe: actions come from the main thread, saves from others. */
final class Recent {
    private final int max;
    private final Deque<String> lines = new ArrayDeque<>();
    private boolean changed;

    Recent(int max) {
        this.max = Math.max(1, max);
    }

    synchronized void add(String line) {
        lines.addLast(line);
        while (lines.size() > max) lines.removeFirst();
        changed = true;
    }

    synchronized List<String> snapshot() {
        return new ArrayList<>(lines);
    }

    /** True once after each change, so unchanged records aren't rewritten. */
    synchronized boolean takeChanged() {
        boolean c = changed;
        changed = false;
        return c;
    }
}
