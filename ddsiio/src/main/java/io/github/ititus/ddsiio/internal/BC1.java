package io.github.ititus.ddsiio.internal;

import java.nio.ByteBuffer;

public final class BC1 {

    public static final BC.BlockDecoder DECODER = BC1::decode;

    public static void decode(ByteBuffer in, int[] out) {
        var colors = new int[4];
        var colorIndices = new byte[4];
        loadColors(in, colors, colorIndices, true);

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                out[x + y * 4] = lookupColor(colors, colorIndices, y, x);
            }
        }
    }

    static void loadColors(ByteBuffer b, int[] colors, byte[] colorIndices, boolean oneBitAlpha) {
        short rawC0 = b.getShort();
        short rawC1 = b.getShort();
        b.get(colorIndices, 0, 4);

        int c0 = fromR5G6B5(rawC0);
        int c1 = fromR5G6B5(rawC1);
        colors[0] = c0;
        colors[1] = c1;
        if (!oneBitAlpha || Short.compareUnsigned(rawC0, rawC1) > 0) {
            colors[2] = lerp(c0, c1, 2, 1);
            colors[3] = lerp(c0, c1, 1, 2);
        } else {
            colors[2] = lerp(c0, c1, 1, 1);
            colors[3] = 0;
        }
    }

    static int lookupColor(int[] colors, byte[] colorIndices, int y, int x) {
        int colorIndex = (colorIndices[y] >>> (2 * x)) & 0x3;
        return colors[colorIndex];
    }

    static int fromR5G6B5(short color) {
        int r = (color >>> 11) & 0x1f;
        int g = (color >>> 5) & 0x3f;
        int b = color & 0x1f;
        // exact multiplications would be 255/31 ≈ 8.2258 and 255/63 ≈ 4.0476 respectively
        r = ((r << 3) | (r >>> 2)); // equivalent to r * 8.25
        g = ((g << 2) | (g >>> 4)); // equivalent to g * 4.0625
        b = ((b << 3) | (b >>> 2)); // equivalent to b * 8.25
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    static int lerp(int c0, int c1, int w0, int w1) {
        int w = w0 + w1;
        int a = (((c0 >>> 24) & 0xFF) * w0 + ((c1 >>> 24) & 0xFF) * w1 + w / 2) / w;
        int r = (((c0 >>> 16) & 0xFF) * w0 + ((c1 >>> 16) & 0xFF) * w1 + w / 2) / w;
        int g = (((c0 >>> 8) & 0xFF) * w0 + ((c1 >>> 8) & 0xFF) * w1 + w / 2) / w;
        int b = ((c0 & 0xFF) * w0 + (c1 & 0xFF) * w1 + w / 2) / w;
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
