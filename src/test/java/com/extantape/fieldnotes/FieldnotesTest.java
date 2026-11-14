package com.extantape.fieldnotes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FieldnotesTest {
    @TempDir
    Path dir;

    @Test void commandsAreRecordedByNameOnlySoPrivateWordsNeverReachTheFile() {
        assertEquals("/msg", Line.commandName("/msg Friend my secret"));
        assertEquals("/login", Line.commandName("/LOGIN hunter2"));
        assertEquals("/home", Line.commandName("home"));
    }

    @Test void fieldsStayOnOneLine() {
        assertEquals("a b / c", Line.clean("a\nb | c", 100));
        assertEquals("abc...", Line.clean("abcdef", 3));
    }

    @Test void facingFollowsMinecraftsYaw() {
        assertEquals("south", Line.facing(0));
        assertEquals("west", Line.facing(90));
        assertEquals("north", Line.facing(180));
        assertEquals("east", Line.facing(-90));
    }

    @Test void theRecordKeepsOnlyTheLastFewAndSaysWhenItChanged() {
        Recent r = new Recent(3);
        assertFalse(r.takeChanged());
        for (int i = 1; i <= 5; i++) r.add("a" + i);
        assertEquals(List.of("a3", "a4", "a5"), r.snapshot());
        assertTrue(r.takeChanged());
        assertFalse(r.takeChanged(), "unchanged records aren't rewritten");
    }

    @Test void notesAppendAndRecordsReplaceWhole() throws Exception {
        Path notes = dir.resolve("notes.log");
        Files.append(notes, List.of("one"));
        Files.append(notes, List.of("two"));
        assertEquals(List.of("one", "two"), java.nio.file.Files.readAllLines(notes));
        Path rec = dir.resolve("recent").resolve("Tester.log");
        Files.replace(rec, List.of("x", "y"));
        Files.replace(rec, List.of("z"));
        assertEquals(List.of("z"), java.nio.file.Files.readAllLines(rec));
        assertFalse(java.nio.file.Files.exists(rec.resolveSibling("Tester.log.tmp")));
    }
}
