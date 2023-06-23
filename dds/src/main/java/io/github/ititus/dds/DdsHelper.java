package io.github.ititus.dds;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class DdsHelper {

    private DdsHelper() {
    }

    public static int calculatePitch(int width, PixelFormat format) {
        int horizontalBlocks = maxUnsigned(1, ceilDivUnsigned(width, format.getHorizontalPixelsPerBlock()));
        int bitsPerBlock = format.getBitsPerBlock();
        if (bitsPerBlock % 8 == 0) {
            int bytesPerBlock = bitsPerBlock / 8;
            return horizontalBlocks * bytesPerBlock;
        }

        int bits = Math.multiplyExact(horizontalBlocks, format.getBitsPerBlock());
        return ceilDivUnsigned(bits, 8);
    }

    public static int calculateSurfaceSize(int height, int width, PixelFormat format) {
        int pitch = DdsHelper.calculatePitch(width, format);
        int verticalBlocks = maxUnsigned(1, ceilDivUnsigned(height, format.getVerticalPixelsPerBlock()));
        return Math.multiplyExact(pitch, verticalBlocks);
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
        } else if (divisor == 1) {
            return dividend;
        }

        return 1 + Integer.divideUnsigned(dividend - 1, divisor);
    }

    private static boolean isPrintable(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isISOControl(c)) {
                return false;
            }
        }

        return true;
    }

    public static String guessToString(int n) {
        String fourCC = DdsConstants.getStringFrom4CC(n);
        if (isPrintable(fourCC)) {
            return fourCC;
        }

        return Integer.toUnsignedString(n);
    }
}
