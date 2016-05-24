package cauliflower.util;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

public class FileSystem {

    // Borrowed from:
    // Trevor Robinson
    // http://stackoverflow.com/a/8685959/3474
    public static boolean recursiveRemove(Path pat) {
        try {
            Files.walkFileTree(pat, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        throw exc;
                    }
                }
            });
            return true;
        } catch (IOException exc) {
            return false;
        }
    }

    public static Stream<String> getLineStream(InputStream is) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            return buffer.lines();
        }
    }

    public static Stream<String> getLineStream(File f) throws IOException {
        return getLineStream(f.toPath());
    }

    public static Stream<String> getLineStream(Path p) throws IOException {
        return Files.lines(p);
    }

}
