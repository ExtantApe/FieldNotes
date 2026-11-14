package com.extantape.fieldnotes;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

/** Writes that reach the disk before they return, so a crash right after loses nothing already written. */
final class Files {
    private Files() {}

    /** Appends lines to a file and forces them to disk. */
    static synchronized void append(Path file, List<String> lines) throws IOException {
        java.nio.file.Files.createDirectories(file.getParent());
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            ch.write(java.nio.ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.UTF_8)));
            ch.force(true);
        }
    }

    /** Replaces a file's contents in one step: a crash leaves the old version or the new one, never half. */
    static void replace(Path file, List<String> lines) throws IOException {
        java.nio.file.Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ch.write(java.nio.ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.UTF_8)));
            ch.force(true);
        }
        java.nio.file.Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
