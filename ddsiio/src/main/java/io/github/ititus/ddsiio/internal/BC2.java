package io.github.ititus.ddsiio.internal;

import io.github.ititus.dds.Rgba;

import java.nio.ByteBuffer;

public final class BC2 {

    public static final BC.BlockDecoder DECODER = BC2::decode;

    public static void decode(ByteBuffer in, Rgba[] out) {
        var alphas = new byte[8];
        var colors = new Rgba[4];
        var colorIndices = new byte[4];
        in.get(alphas, 0, 8);
        BC1.loadColors(in, colors, colorIndices, false);

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                var alpha = lookupAlpha(alphas, y, x);
                out[x + y * 4] = BC1.lookupColor(colors, colorIndices, y, x).withAlpha(alpha);
            }
        }
    }

    private static byte lookupAlpha(byte[] alphas, int y, int x) {
        int alpha = (alphas[(y << 1) | (x >>> 1)] >>> (4 * (x & 0x1))) & 0xf;
        return (byte) (alpha * 17);
    }
}
