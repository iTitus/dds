package io.github.ititus.dds;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class DdsResource {

    private final ByteBuffer buffer;

    private DdsResource(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public static List<DdsResource> loadAll(DataReader r, DdsHeader header, DdsHeaderDxt10 header10) throws IOException {
        List<DdsResource> resources = new ArrayList<>();

        int height = DdsHelper.maxUnsigned(1, header.dwHeight());
        int width = DdsHelper.maxUnsigned(1, header.dwWidth());
        int depth = header.isVolumeTexture() ? DdsHelper.maxUnsigned(1, header.dwDepth()) : 1;
        int mipMapCount = header.hasMipmaps() ? DdsHelper.maxUnsigned(1, header.dwMipMapCount()) : 1;
        int faces = header.isCubemap() ? DdsHelper.maxUnsigned(1, header.calculateCubemapFaces()) : 1;
        int arraySize = header10 != null ? DdsHelper.maxUnsigned(1, header10.arraySize()) : 1;
        PixelFormat format = header10 != null ? header10.dxgiFormat() : header.d3dFormat();

        for (int arrayIndex = 0; Integer.compareUnsigned(arrayIndex, arraySize) < 0; arrayIndex++) {
            for (int face = 0; Integer.compareUnsigned(face, faces) < 0; face++) {
                int currentHeight = height;
                int currentWidth = width;
                int currentDepth = depth;
                for (int mipmap = 0; Integer.compareUnsigned(mipmap, mipMapCount) < 0; mipmap++) {
                    int size = DdsHelper.calculateSurfaceSize(currentHeight, currentWidth, format);
                    for (int z = 0; Integer.compareUnsigned(z, currentDepth) < 0; z++) {
                        resources.add(load(r, size));
                    }

                    if (currentHeight == 1 && currentWidth == 1 && currentDepth == 1) {
                        break;
                    }

                    currentHeight = DdsHelper.ceilDivUnsigned(currentHeight, 2);
                    currentWidth = DdsHelper.ceilDivUnsigned(currentWidth, 2);
                    currentDepth = DdsHelper.ceilDivUnsigned(currentDepth, 2);
                }
            }
        }

        return List.copyOf(resources);
    }

    public static DdsResource load(DataReader r, int size) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(size);
        r.read(buf, size);
        buf.flip();
        return new DdsResource(buf.asReadOnlyBuffer());
    }

    public ByteBuffer getBuffer() {
        return buffer.duplicate();
    }
}
