package io.github.ititus.ddsiio.internal;

import java.nio.ByteBuffer;

public final class BC3 {

    public static final BC.BlockDecoder DECODER = BC3::decode;

    public static void decode(ByteBuffer in, int[] out) {
        var alphas = new byte[8];
        var alphaIndices = new int[2];
        var colors = new int[4];
        var colorIndices = new byte[4];
        loadAlpha(in, alphas, alphaIndices);
        BC1.loadColors(in, colors, colorIndices, false);

        for (int y_ = 0; y_ < 4; y_++) {
            for (int x_ = 0; x_ < 4; x_++) {
                var alpha = lookupAlpha(alphas, alphaIndices, y_, x_);
                out[x_ + y_ * 4] = (BC1.lookupColor(colors, colorIndices, y_, x_) & 0xFFFFFF) | ((alpha & 0xFF) << 24);
            }
        }
    }

    private static void loadAlpha(ByteBuffer b, byte[] alphas, int[] alphaIndices) {
        byte rawA0 = b.get();
        byte rawA1 = b.get();
        alphaIndices[0] = Util.read24(b);
        alphaIndices[1] = Util.read24(b);

        int a0 = Byte.toUnsignedInt(rawA0);
        int a1 = Byte.toUnsignedInt(rawA1);

        alphas[0] = rawA0;
        alphas[1] = rawA1;
        if (Byte.compareUnsigned(rawA0, rawA1) > 0) {
            for (int i = 1; i <= 6; i++) {
                alphas[i + 1] = (byte) (((7 - i) * a0 + i * a1) / 7);
            }
        } else {
            for (int i = 1; i <= 4; i++) {
                alphas[i + 1] = (byte) (((5 - i) * a0 + i * a1) / 5);
            }
            alphas[6] = 0;
            alphas[7] = (byte) 255;
        }
    }

    private static byte lookupAlpha(byte[] alphas, int[] alphaIndices, int y, int x) {
        int alphaIndex = (alphaIndices[y >>> 1] >>> (3 * (((y & 0x1) << 2) | x))) & 0x7;
        return alphas[alphaIndex];
    }
}
