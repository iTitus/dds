package io.github.ititus.dds;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public final class DdsResource {

    private final int height;
    private final int width;
    private final int arrayIndex;
    private final int faceIndex;
    private final int mipmapLevel;
    private final int zLevel;
    private final ByteBuffer buffer;

    private DdsResource(int height, int width, int arrayIndex, int faceIndex, int mipmapLevel, int zLevel, ByteBuffer buffer) {
        this.height = height;
        this.width = width;
        this.arrayIndex = arrayIndex;
        this.faceIndex = faceIndex;
        this.mipmapLevel = mipmapLevel;
        this.zLevel = zLevel;
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
                        /*System.out.printf(
                                "loading resource: index=%d/%d, face=%d/%d, mipmap=%d/%d, z=%d/%d | height=%d, " +
                                        "width=%d | size=%d%n",
                                arrayIndex, arraySize, face, faces, mipmap, mipMapCount, z, currentDepth,
                                currentHeight, currentWidth, size
                        );*/
                        resources.add(load(r, currentHeight, currentWidth, arrayIndex, face, mipmap, z, size));
                    }

                    if (Integer.compareUnsigned(currentHeight, 1) > 0) {
                        currentHeight = Integer.divideUnsigned(currentHeight, 2);
                    }

                    if (Integer.compareUnsigned(currentWidth, 1) > 0) {
                        currentWidth = Integer.divideUnsigned(currentWidth, 2);
                    }

                    if (Integer.compareUnsigned(currentDepth, 1) > 0) {
                        currentDepth = Integer.divideUnsigned(currentDepth, 2);
                    }
                }
            }
        }

        return List.copyOf(resources);
    }

    public static DdsResource load(DataReader r, int height, int width, int arrayIndex, int faceIndex, int mipmapLevel, int zLevel, int size) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(size);
        r.read(buf, size);
        buf.flip();
        return new DdsResource(height, width, arrayIndex, faceIndex, mipmapLevel, zLevel, buf.asReadOnlyBuffer());
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getArrayIndex() {
        return arrayIndex;
    }

    public int getFaceIndex() {
        return faceIndex;
    }

    public int getMipmapLevel() {
        return mipmapLevel;
    }

    public int getZLevel() {
        return zLevel;
    }

    /**
     * @return little-endian view of the actual data
     */
    public ByteBuffer getBuffer() {
        return buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>(6);
        list.add("height=" + Integer.toUnsignedString(height));
        list.add("width=" + Integer.toUnsignedString(width));
        list.add("arrayIndex=" + Integer.toUnsignedString(arrayIndex));
        list.add("faceIndex=" + Integer.toUnsignedString(faceIndex));
        list.add("mipmapLevel=" + Integer.toUnsignedString(mipmapLevel));
        list.add("zLevel=" + Integer.toUnsignedString(zLevel));
        return "DdsResource[" + String.join(", ", list) + ']';
    }
}
