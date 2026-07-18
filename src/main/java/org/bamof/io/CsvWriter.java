package org.bamof.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Function;

public final class CsvWriter {
    private CsvWriter() {
    }

    public static <T> void write(Path path, String header, Collection<T> rows, Function<T, String> mapper)
            throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(header);
            writer.newLine();
            for (T row : rows) {
                writer.write(mapper.apply(row));
                writer.newLine();
            }
        }
    }
}
