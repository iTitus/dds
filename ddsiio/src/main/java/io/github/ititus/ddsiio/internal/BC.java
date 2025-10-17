package io.github.ititus.ddsiio.internal;

import io.github.ititus.dds.D3dFormat;
import io.github.ititus.dds.DxgiFormat;
import io.github.ititus.dds.PixelFormat;
import io.github.ititus.dds.Rgba;

import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class BC {

    public static void decode(int h, int w, WritableRaster raster, ByteBuffer b, PixelFormat format) {
        BlockDecoder decoder = switch (format) {
            case D3dFormat.DXT1, DxgiFormat.BC1_UNORM, DxgiFormat.BC1_UNORM_SRGB -> BC1.DECODER;
            case D3dFormat.DXT2, D3dFormat.DXT3, DxgiFormat.BC2_UNORM, DxgiFormat.BC2_UNORM_SRGB -> BC2.DECODER;
            case D3dFormat.DXT4, D3dFormat.DXT5, DxgiFormat.BC3_UNORM, DxgiFormat.BC3_UNORM_SRGB -> BC3.DECODER;
            case DxgiFormat.BC7_UNORM, DxgiFormat.BC7_UNORM_SRGB -> BC7.DECODER;
            default -> throw new UnsupportedOperationException("unsupported block compression " + format);
        };

        var decoded = new Rgba[16];
        Arrays.fill(decoded, Rgba.TRANSPARENT);

        for (int y = 0; Integer.compareUnsigned(y, h) < 0; y += 4) {
            int yMax = Math.min(4, h - y);
            for (int x = 0; Integer.compareUnsigned(x, w) < 0; x += 4) {
                decoder.decode(b, decoded);

                int xMax = Math.min(4, w - x);
                for (int y_ = 0; y_ < yMax; y_++) {
                    for (int x_ = 0; x_ < xMax; x_++) {
                        var color = decoded[x_ + y_ * xMax];
                        raster.setDataElements(x + x_, y + y_, new int[] { color.asA8R8G8B8() });
                    }
                }
            }
        }
    }

    @FunctionalInterface
    public interface BlockDecoder {

        void decode(ByteBuffer in, Rgba[] out);
    }
}
