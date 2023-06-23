package io.github.ititus.dds;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class DdsHelper {

    private DdsHelper() {
    }

    public static int calculatePitch(D3dFormat d3dFormat, int width) {
        return switch (d3dFormat) {
            case DXT1 -> maxUnsigned(1, ceilDivUnsigned(width, 4)) * 8;
            case DXT2, DXT3, DXT4, DXT5 -> maxUnsigned(1, ceilDivUnsigned(width, 4)) * 16;
            case R8G8_B8G8, G8R8_G8B8, UYVY, YUY2 -> ceilDivUnsigned(width, 2) * 4;
            default -> ceilDivUnsigned(width * d3dFormat.getBitsPerPixel(), 8);
        };
    }

    public static int calculateSurfaceSize(int height, int width, D3dFormat d3dFormat) {
        int pitch = DdsHelper.calculatePitch(d3dFormat, width);
        return switch (d3dFormat) {
            case DXT1, DXT2, DXT3, DXT4, DXT5 -> pitch * ceilDivUnsigned(height, 4);
            default -> pitch * height;
        };
    }

    public static int read24(ByteBuffer b) {
        return b.order() == ByteOrder.BIG_ENDIAN ? read24BE(b) : read24LE(b);
    }

    public static int read24BE(ByteBuffer b) {
        int b0 = Byte.toUnsignedInt(b.get());
        int b1 = Byte.toUnsignedInt(b.get());
        int b2 = Byte.toUnsignedInt(b.get());
        return (b0 << 16) | (b1 << 8) | b2;
    }

    public static int read24LE(ByteBuffer b) {
        int b0 = Byte.toUnsignedInt(b.get());
        int b1 = Byte.toUnsignedInt(b.get());
        int b2 = Byte.toUnsignedInt(b.get());
        return b0 | (b1 << 8) | (b2 << 16);
    }

    public static int maxUnsigned(int a, int b) {
        if (Integer.compareUnsigned(a, b) < 0) {
            return b;
        } else {
            return a;
        }
    }

    public static int ceilDivUnsigned(int dividend, int divisor) {
        if (dividend == 0) {
            return 0;
        }

        return 1 + Integer.divideUnsigned(dividend - 1, divisor);
    }
}
