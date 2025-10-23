package io.github.ititus.dds;

import io.github.ititus.dds.exception.DdsLoadException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.github.ititus.dds.DdsConstants.DDS_MAGIC;

public record DdsFile(
        DdsHeader header,
        DdsHeaderDxt10 header10,
        List<DdsResource> resources
) {

    public static DdsFile load(Path path) throws DdsLoadException {
        try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
            return load(is);
        } catch (Exception e) {
            throw new DdsLoadException("could not load dds file " + path, e);
        }
    }

    public static DdsFile load(InputStream is) throws DdsLoadException {
        return load(new DataReader() {
            @Override
            public byte readByte() throws IOException {
                int n = is.read();
                if (n == -1) {
                    throw new EOFException();
                }

                return (byte) n;
            }

            @Override
            public void read(ByteBuffer target, int size) throws IOException {
                if (target.hasArray()) {
                    int read = is.readNBytes(target.array(), target.arrayOffset() + target.position(), size);
                    if (read != size) {
                        throw new EOFException("expected=" + size + " actual=" + read);
                    }
                    target.position(target.position() + size);
                } else {
                    byte[] arr = new byte[size];
                    int read = is.readNBytes(arr, 0, size);
                    if (read != size) {
                        throw new EOFException("expected=" + size + " actual=" + read);
                    }
                    target.put(arr, 0, size);
                }
            }
        });
    }

    public static DdsFile load(DataInput di) throws DdsLoadException {
        return load(new DataReader() {
            @Override
            public byte readByte() throws IOException {
                return di.readByte();
            }

            @Override
            public void read(ByteBuffer target, int size) throws IOException {
                if (target.hasArray()) {
                    di.readFully(target.array(), target.arrayOffset() + target.position(), size);
                    target.position(target.position() + size);
                } else {
                    byte[] arr = new byte[size];
                    di.readFully(arr, 0, size);
                    target.put(arr, 0, size);
                }
            }
        });
    }

    public static DdsFile load(DataReader r) throws DdsLoadException {
        int dwMagic;
        try {
            dwMagic = r.readDword();
        } catch (EOFException e) {
            throw new DdsLoadException("empty dds file", e);
        } catch (Exception e) {
            throw new DdsLoadException("could not read magic bytes", e);
        }

        if (dwMagic != DDS_MAGIC) {
            throw new DdsLoadException("invalid dds magic");
        }

        DdsHeader header;
        try {
            header = DdsHeader.load(r);
        } catch (Exception e) {
            throw new DdsLoadException("could not load dds header", e);
        }
        if (!header.isValid()) {
            throw new DdsLoadException("invalid dds header");
        }

        DdsHeaderDxt10 header10;
        if (header.shouldLoadHeader10()) {
            try {
                header10 = DdsHeaderDxt10.load(r);
            } catch (Exception e) {
                throw new DdsLoadException("could not load dds dxt10 header", e);
            }
            if (!header10.isValid(header)) {
                throw new DdsLoadException("invalid dds dxt10 header");
            }
        } else {
            header10 = null;
        }

        List<DdsResource> resources;
        try {
            resources = DdsResource.loadAll(r, header, header10);
        } catch (Exception e) {
            throw new DdsLoadException("could not load dds dxt10 header", e);
        }

        try {
            r.readByte();
            throw new DdsLoadException("unconsumed bytes");
        } catch (Exception ignored) {
        }

        return new DdsFile(header, header10, resources);
    }

    public int height() {
        return header.dwHeight();
    }

    public int width() {
        return header.dwWidth();
    }

    public boolean hasMipmaps() {
        return header.hasMipmaps();
    }

    public boolean isFlatTexture() {
        return header.isFlatTexture();
    }

    public boolean isCubemap() {
        return header.isCubemap();
    }

    public boolean isVolumeTexture() {
        return header.isVolumeTexture();
    }

    public boolean isDxt10() {
        return header10 != null;
    }

    public int resourceCount() {
        return resources.size();
    }

    public PixelFormat derivePixelFormat() {
        return DdsHelper.derivePixelFormat(this);
    }

    @Override
    public String toString() {
        PixelFormat pf = this.derivePixelFormat();
        return "DdsFile[" +
                "header=" + header +
                (header10 == null ? "" : ", header10=" + header10) +
                ", pixelFormat=" + pf.getClass().getSimpleName() + "." + pf +
                ", resourceCount=" + this.resourceCount() +
                ']';
    }
}
