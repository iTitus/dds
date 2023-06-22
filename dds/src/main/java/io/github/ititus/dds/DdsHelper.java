package io.github.ititus.dds;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class DdsHelper {

    private DdsHelper() {
    }

    public static int calculatePitch(D3dFormat d3dFormat, int width) {
        return switch (d3dFormat) {
            case DXT1 -> Math.max(1, ((width + 3) / 4)) * 8;
            case DXT2, DXT3, DXT4, DXT5 -> Math.max(1, ((width + 3) / 4)) * 16;
            case R8G8_B8G8, G8R8_G8B8, UYVY, YUY2 -> ((width + 1) >> 1) * 4;
            default -> (width * d3dFormat.getBitsPerPixel() + 7) / 8;
        };
    }

    public static int calculateSurfaceSize(int height, int width, D3dFormat d3dFormat) {
        int pitch = DdsHelper.calculatePitch(d3dFormat, width);
        return switch (d3dFormat) {
            case DXT1, DXT2, DXT3, DXT4, DXT5 -> pitch * Math.max(1, (height + 3) / 4);
            default -> pitch * height;
        };
    }

    public static int read24(ByteBuffer b) {
        return b.order() == ByteOrder.BIG_ENDIAN ? read24BE(b) : read24LE(b);
    }

    public static int read24BE(ByteBuffer b) {
        byte[] bytes = new byte[3];
        b.get(bytes);
        return (Byte.toUnsignedInt(bytes[0]) << 16) | (Byte.toUnsignedInt(bytes[1]) << 8) | Byte.toUnsignedInt(bytes[2]);
    }

    public static int read24LE(ByteBuffer b) {
        byte[] bytes = new byte[3];
        b.get(bytes);
        return Byte.toUnsignedInt(bytes[0]) | (Byte.toUnsignedInt(bytes[1]) << 8) | (Byte.toUnsignedInt(bytes[2]) << 16);
    }

    public static int maxUnsigned(int a, int b) {
        if (Integer.compareUnsigned(a, b) < 0) {
            return b;
        } else {
            return a;
        }
    }
}
