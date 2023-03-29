package io.github.ititus.ddsexample;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public final class Example {

    private Example() {}

    public static void main(String[] args) throws Exception {
        Path desktop = Path.of(System.getProperty("user.home"), "Desktop").toRealPath();

        convertTo(desktop.resolve("ce_eagle_doubleheaded_base.dds"), "png");
    }

    private static void convertTo(Path in, String formatName) throws IOException {
        convertTo(in, formatName, formatName.toLowerCase(Locale.ROOT));
    }

    private static void convertTo(Path in, String formatName, String fileExtension) throws IOException {
        in = in.toRealPath();
        if (!Files.isRegularFile(in)) {
            throw new IllegalArgumentException("invalid input path");
        }

        String name = in.getFileName().toString();
        if (name.isEmpty()) {
            throw new IllegalArgumentException();
        }

        int lastDot = name.lastIndexOf('.');
        String nameWithoutExtension;
        if (lastDot > 0 && lastDot < name.length() - 1) {
            nameWithoutExtension = name.substring(0, lastDot);
        } else {
            nameWithoutExtension = name;
        }

        Path out = in.resolveSibling(nameWithoutExtension + '.' + fileExtension).toRealPath();
        if (Files.isSameFile(in, out)) {
            throw new IllegalArgumentException("generated output format gives same file path");
        }

        convertTo(in, out, formatName);
    }

    private static void convertTo(Path in, Path out, String formatName) throws IOException {
        try (var is = Files.newInputStream(in, StandardOpenOption.READ)) {
            var img = ImageIO.read(is);
            Files.createDirectories(out.getParent());
            try (var os = Files.newOutputStream(out, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ImageIO.write(img, formatName, os);
            }
        }
    }
}
