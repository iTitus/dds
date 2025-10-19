package io.github.ititus.ddsexample;

import io.github.ititus.dds.DdsFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class Example {

    static final Predicate<Path> DDS_FILE =
            file -> Files.isRegularFile(file)
                    && file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dds");

    private Example() {}

    public static void main(String[] args) throws Exception {
        var desktop = Path.of(System.getProperty("user.home"), "Desktop").toRealPath();
        /*var out = desktop.resolve("pdx");
        var steam = Path.of("C:/Program Files (x86)/Steam/steamapps/common").toRealPath();
        var ck3InstallDir = steam.resolve("Crusader Kings III");
        var eu4InstallDir = steam.resolve("Europa Universalis IV");
        var vic3InstallDir = steam.resolve("Victoria III");
        var hoi4InstallDir = steam.resolve("Hearts of Iron IV");
        var stellarisInstallDir = steam.resolve("Stellaris");*/

        // showInfoAndConvertToPng(desktop.resolve("ce_frame_circle.dds"), false);
        // showInfoAndConvertToPng(desktop.resolve("ce_dragon_bhutan.dds"), false);
        // showInfoAndConvertToPng(desktop.resolve("surround_tile.dds"), false);
        // showInfoAndConvertToPng(desktop.resolve("placeholder_activity_background_bg.dds"), false);
        // showInfoAndConvertToPng(desktop.resolve("colony_settlement.dds"), false);
        // convertRecursive(stellarisInstallDir, out.resolve("dds_out"), "png", false);

        // showInfoRecursive(ck3InstallDir, out.resolve("dds_ck3.log"));
        // showInfoRecursive(eu4InstallDir, out.resolve("dds_eu4.log"));
        // showInfoRecursive(stellarisInstallDir, out.resolve("dds_stellaris.log"));

        showInfoRecursive(desktop.resolve("in"));
        convertRecursive(desktop.resolve("in"), desktop.resolve("out"), "png", false);
    }

    static void showInfoAndConvertToPng(Path file, boolean all) throws Exception {
        showInfo(file);
        convertTo(file, null, "png", all);
    }

    private static void doRecursive(Path inDir, Consumer<? super Path> action) throws Exception {
        try (Stream<Path> stream = Files.walk(inDir)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(DDS_FILE)
                    .forEach(action);
        }
    }

    static void showInfoRecursive(Path inDir) throws Exception {
        showInfoRecursive(inDir, System.out);
    }

    static void showInfoRecursive(Path inDir, Path logFile) throws Exception {
        Files.createDirectories(logFile.getParent());
        try (
                var w = Files.newBufferedWriter(logFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                var pw = new PrintWriter(w)
        ) {
            showInfoRecursive(inDir, pw);
        }
    }

    static void showInfoRecursive(Path inDir, PrintWriter w) throws Exception {
        doRecursive(inDir, file -> {
            Path relative = inDir.relativize(file);
            String relativeString = relative.toString().replace('\\', '/');
            try {
                var dds = DdsFile.load(file);
                w.println(relativeString + ": " + dds);
            } catch (Exception e) {
                w.println(relativeString + ": load error | " + e);
            }
        });
    }

    static void showInfoRecursive(Path inDir, PrintStream w) throws Exception {
        doRecursive(inDir, file -> {
            Path relative = inDir.relativize(file);
            String relativeString = relative.toString().replace('\\', '/');
            try {
                var dds = DdsFile.load(file);
                w.println(relativeString + ": " + dds);
            } catch (Exception e) {
                w.println(relativeString + ": load error | " + e);
            }
        });
    }

    static void convertRecursive(Path inDir, Path outDir, String formatName, boolean all) throws Exception {
        long now = System.nanoTime();
        doRecursive(inDir, file -> {
            Path relative = inDir.relativize(file);
            Path relativeDir = relative.getParent();
            Path outParentDir = relativeDir != null ? outDir.resolve(relativeDir) : outDir;

            try {
                convertTo(file, outParentDir, formatName, all);
            } catch (Exception e) {
                String msg;
                try {
                    var dds = DdsFile.load(file);
                    msg = "conversion error: " + dds + " | " + e + " | cause=" + e.getCause();
                } catch (Exception e2) {
                    e2.addSuppressed(e);
                    msg = "load error | " + e2 + " | cause=" + e2.getCause();
                }

                System.out.println(relative.toString().replace('\\', '/') + ": " + msg);
            }
        });
        long elapsed = System.nanoTime() - now;
        System.out.printf("conversion done after %.0f ms%n",  elapsed / 1e6);
    }

    static void convertTo(Path in, Path outDir, String formatName, boolean all) throws IOException {
        convertTo(in, outDir, formatName, formatName.toLowerCase(Locale.ROOT), all);
    }

    static void convertTo(Path in, Path outDir, String formatName, String fileExtension, boolean all) throws IOException {
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
            if (numImages <= 0) {
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

    static void showInfo(Path in) throws Exception {
        var dds = DdsFile.load(in);
        System.out.println(dds);
    }
}
