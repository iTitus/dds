package io.github.ititus.ddsiio.internal;

import io.github.ititus.dds.Rgba;

import java.nio.ByteBuffer;

public final class BC1 {

    public static final BC.BlockDecoder DECODER = BC1::decode;

    public static void decode(ByteBuffer in, Rgba[] out) {
        var colors = new Rgba[4];
        var colorIndices = new byte[4];
        loadColors(in, colors, colorIndices, true);

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                out[x + y * 4] = lookupColor(colors, colorIndices, y, x);
            }
        }
    }

    static void loadColors(ByteBuffer b, Rgba[] colors, byte[] colorIndices, boolean oneBitAlpha) {
        short rawC0 = b.getShort();
        short rawC1 = b.getShort();
        b.get(colorIndices, 0, 4);

        var c0 = Rgba.fromR5G6B5(rawC0);
        var c1 = Rgba.fromR5G6B5(rawC1);
        colors[0] = c0;
        colors[1] = c1;
        if (!oneBitAlpha || Short.compareUnsigned(rawC0, rawC1) > 0) {
            colors[2] = c0.lerp(c1, 2, 1);
            colors[3] = c0.lerp(c1, 1, 2);
        } else {
            colors[2] = c0.lerp(c1, 1, 1);
            colors[3] = Rgba.TRANSPARENT;
        }
    }

    static Rgba lookupColor(Rgba[] colors, byte[] colorIndices, int y, int x) {
        int colorIndex = (Byte.toUnsignedInt(colorIndices[y]) >>> (2 * x)) & 0x3;
        return colors[colorIndex];
    }
}
