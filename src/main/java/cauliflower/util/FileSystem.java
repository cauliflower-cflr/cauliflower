package cauliflower.util;

import java.io.*;
import java.nio.file.*;
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

    public static String stripExtension(Path file){
        String ret = file.getFileName().toString();
        if(ret.contains(".")) ret = ret.substring(0, ret.lastIndexOf("."));
        return ret;
    }

    public static Path constructPath(Path dir, String name){
        return Paths.get(dir.toString(), name);
    }

    public static Path constructPath(Path dir, String name, String ext){
        return constructPath(dir, name + "." + ext);
    }

    public static void mkdirFor(Path p) throws IOException {
        Files.createDirectories(p.getParent());
    }

    public static PrintStream getOutputStream(Path p) throws FileNotFoundException {
        return new PrintStream(new FileOutputStream(p.toFile()));
    }

}
