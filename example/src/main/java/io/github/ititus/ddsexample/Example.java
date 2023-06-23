package io.github.ititus.ddsexample;

import io.github.ititus.dds.DdsFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class Example {

    private static final Predicate<Path> DDS_FILE =
            file -> Files.isRegularFile(file)
                    && file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dds");

    private Example() {}

    public static void main(String[] args) throws Exception {
        Path desktop = Path.of(System.getProperty("user.home"), "Desktop").toRealPath();
        Path stellarisInstallDir = Path.of("C:/Program Files (x86)/Steam/steamapps/common/Stellaris").toRealPath();

        // showInfoAndConvertToPng(desktop.resolve("ce_frame_circle.dds"));
        showInfoAndConvertToPng(desktop.resolve("ce_dragon_bhutan.dds"), true);
        // convertRecursive(stellarisInstallDir, desktop.resolve("pdx/dds_out"), "png");
    }

    private static void showInfoAndConvertToPng(Path file, boolean all) throws Exception {
        showInfo(file);
        convertTo(file, null, "png", all);
    }

    private static void convertRecursive(Path inDir, Path outDir, String formatName) throws Exception {
        List<Path> files;
        try (Stream<Path> stream = Files.walk(inDir)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(DDS_FILE)
                    .toList();
        }

        for (Path file : files) {
            Path relativeDir = inDir.relativize(file.getParent());
            Path outParentDir = outDir.resolve(relativeDir);
            try {
                convertTo(file, outParentDir, formatName, true);
            } catch (Exception e) {
                Path relative = inDir.relativize(file);

                String msg;
                try {
                    var dds = DdsFile.load(file);
                    msg = "conversion error: " + dds + " | " + e;
                } catch (Exception e2) {
                    msg = "load error | " + e2;
                }

                System.out.println(relative + ": " + msg);
            }
        }
    }

    private static void convertTo(Path in, Path outDir, String formatName, boolean all) throws IOException {
        convertTo(in, outDir, formatName, formatName.toLowerCase(Locale.ROOT), all);
    }

    private static void convertTo(Path in, Path outDir, String formatName, String fileExtension, boolean all) throws IOException {
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

        String outFileName = nameWithoutExtension + '.' + fileExtension;
        Path out;
        if (outDir != null) {
            out = outDir.resolve(outFileName);
        } else {
            out = in.resolveSibling(outFileName);
        }
        out = out.toAbsolutePath().normalize();

        if (Files.exists(out)) {
            if (!Files.isRegularFile(out)) {
                throw new IllegalArgumentException("generated output path already exists and is not a regular file");
            } else if (Files.isSameFile(in, out)) {
                throw new IllegalArgumentException("generated output path gives same file path as input");
            }
        }

        if (all) {
            convertToAllImpl(in, out, formatName);
        } else {
            convertToImpl(in, out, formatName);
        }
    }

    private static void convertToImpl(Path in, Path out, String formatName) throws IOException {
        if (Files.size(in) == 0) {
            return;
        }

        try (var is = Files.newInputStream(in, StandardOpenOption.READ)) {
            var img = ImageIO.read(is);
            Files.createDirectories(out.getParent());
            try (var os = Files.newOutputStream(out, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ImageIO.write(img, formatName, os);
            }
        }
    }

    private static void convertToAllImpl(Path in, Path out, String formatName) throws IOException {
        if (Files.size(in) == 0) {
            return;
        }

        try (var is = Files.newInputStream(in, StandardOpenOption.READ)) {
            var iis = ImageIO.createImageInputStream(is);
            var reader = ImageIO.getImageReaders(iis).next();

            reader.setInput(iis);
            int numImages = reader.getNumImages(false);
            if (numImages == 0) {
                return;
            }

            Files.createDirectories(out.getParent());
            for (int imageIndex = 0; imageIndex < numImages; imageIndex++) {
                var img = reader.read(imageIndex);

                Path newOut;
                if (numImages == 1) {
                    newOut = out;
                } else {
                    String name = out.getFileName().toString();
                    int lastDot = name.lastIndexOf('.');
                    String newName;
                    if (lastDot > 0 && lastDot < name.length() - 1) {
                        newName = name.substring(0, lastDot) + "_" + imageIndex + name.substring(lastDot);
                    } else {
                        newName = name + "_" + imageIndex;
                    }
                    newOut = out.resolveSibling(newName);
                }

                try (var os = Files.newOutputStream(newOut, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    ImageIO.write(img, formatName, os);
                }
            }
        }
    }

    private static void showInfo(Path in) throws Exception {
        var dds = DdsFile.load(in);
        System.out.println(dds);
    }
}
