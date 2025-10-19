package io.github.ititus.ddsiio.internal;

import io.github.ititus.dds.*;

import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static io.github.ititus.dds.DdsConstants.*;

public final class BC {

    public static void decode(int h, int w, WritableRaster raster, ByteBuffer b, PixelFormat format) {
        BlockDecoder decoder = switch (format) {
            case D3dFormat.DXT1, DxgiFormat.BC1_UNORM, DxgiFormat.BC1_UNORM_SRGB -> BC1.DECODER;
            case D3dFormat.DXT2, D3dFormat.DXT3, DxgiFormat.BC2_UNORM, DxgiFormat.BC2_UNORM_SRGB -> BC2.DECODER;
            case D3dFormat.DXT4, D3dFormat.DXT5, DxgiFormat.BC3_UNORM, DxgiFormat.BC3_UNORM_SRGB -> BC3.DECODER;
            case DxgiFormat.BC7_UNORM, DxgiFormat.BC7_UNORM_SRGB -> BC7.DECODER;
            default -> throw new UnsupportedOperationException("unsupported block compression " + format);
        };

        int[] decoded = new int[16];
        for (int y = 0; Integer.compareUnsigned(y, h) < 0; y += 4) {
            int yMax = Math.min(4, h - y);
            for (int x = 0; Integer.compareUnsigned(x, w) < 0; x += 4) {
                decoder.decode(b, decoded);

                int xMax = Math.min(4, w - x);
                if (xMax == 4) {
                    raster.setDataElements(x, y, xMax, yMax, decoded);
                } else {
                    for (int i = 0; i < yMax; i++) {
                        raster.setDataElements(x, y, xMax, yMax, Arrays.copyOfRange(decoded, 4 * i, 4 * i + xMax));
                    }
                }
            }
        }
    }

    public static boolean isBlockCompressed(DdsPixelformat pf) {
        if ((pf.dwFlags() & DdsConstants.DDPF_FOURCC) == DdsConstants.DDPF_FOURCC) {
            int fcc = pf.dwFourCC();
            if (fcc == D3DFMT_DXT1 || fcc == D3DFMT_DXT2 || fcc == D3DFMT_DXT3 || fcc == D3DFMT_DXT4 || fcc == D3DFMT_DXT5
                    || fcc == DXGI_FORMAT_BC4_UNORM || fcc == DXGI_FORMAT_BC4_UNORM_ALT || fcc == DXGI_FORMAT_BC4_SNORM
                    || fcc == DXGI_FORMAT_BC5_UNORM || fcc == DXGI_FORMAT_BC5_SNORM) {
                return true;
            }
        }

        return false;
    }

    @FunctionalInterface
    public interface BlockDecoder {

        void decode(ByteBuffer in, int[] out);
    }
}
