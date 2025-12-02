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
            file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dds")
                    && Files.isRegularFile(file);

    private Example() {}

    public static void main(String[] args) throws Exception {
        var desktop = Path.of(System.getProperty("user.home"), "Desktop").toRealPath();

        showInfoRecursive(desktop.resolve("in"));
        // convertRecursive(desktop.resolve("in"), desktop.resolve("out"), "png", false);

        // var steamInstallation = SteamInstallation.find();
        // var eu5InstallDir = steamInstallation.getInstallationDir(3450310).orElseThrow().toRealPath();
        // var vic3InstallDir = steamInstallation.getInstallationDir(529340).orElseThrow().toRealPath();
        // var ck3InstallDir = steamInstallation.getInstallationDir(1158310).orElseThrow().toRealPath();
        // var hoi4InstallDir = steamInstallation.getInstallationDir(394360).orElseThrow().toRealPath();
        // var stellarisInstallDir = steamInstallation.getInstallationDir(281990).orElseThrow().toRealPath();
        // var eu4InstallDir = steamInstallation.getInstallationDir(236850).orElseThrow().toRealPath();

        // var out = desktop.resolve("pdx");

        // showInfoRecursive(vic3InstallDir, out.resolve("vic3.log"));
        // showInfoRecursive(stellarisInstallDir, out.resolve("stellaris.log"));

        // convertRecursive(eu5InstallDir, out.resolve("eu5"), "png", false);
        // convertRecursive(vic3InstallDir, out.resolve("vic3"), "png", false);
        // convertRecursive(ck3InstallDir, out.resolve("ck3"), "png", false);
        // convertRecursive(hoi4InstallDir, out.resolve("hoi4"), "png", false);
        // convertRecursive(stellarisInstallDir, out.resolve("stellaris"), "png", false);
        // convertRecursive(eu4InstallDir, out.resolve("eu4"), "png", false);
    }

    static void showInfoAndConvertToPng(Path file, boolean all) throws Exception {
        showInfo(file);
        convertTo(file, null, "png", all);
    }

    private static void doRecursive(Path inDir, Consumer<? super Path> action) throws Exception {
        try (Stream<Path> stream = Files.walk(inDir)) {
            stream
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
                StringBuilder msg = new StringBuilder(relative.toString().replace('\\', '/')).append(": ");
                Exception toPrint;
                try {
                    var dds = DdsFile.load(file);
                    toPrint = e;
                    msg.append("conversion error: ").append(dds);
                } catch (Exception e2) {
                    e2.addSuppressed(e);
                    toPrint = e2;
                    msg.append("load error");
                }

                msg.append(" | ").append(toPrint);
                for (Throwable cause = toPrint.getCause(); cause != null; cause = cause.getCause()) {
                    msg.append(" | cause=").append(cause);
                }

                System.out.println(msg);
            }
        });
        long elapsed = System.nanoTime() - now;
        System.out.printf("conversion done after %.0f ms%n", elapsed / 1e6);
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
