package io.github.ititus.ddsiio.internal;

import io.github.ititus.dds.*;

import javax.imageio.ImageTypeSpecifier;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static io.github.ititus.dds.DdsConstants.*;

public final class Util {

    private Util() {}

    public static ImageTypeSpecifier imageType(DdsFile file) {
        return imageType(file.header(), file.header10());
    }

    public static ImageTypeSpecifier imageType(DdsHeader header, DdsHeaderDxt10 header10) {
        if (header10 != null) {
            return imageType(header10.dxgiFormat(), header10.isAlphaPremultiplied());
        }

        return imageType(header.ddspf());
    }

    /**
     * Get the image type specifier for the resulting BufferedImage after decompression / to-RGB conversion.
     */
    private static ImageTypeSpecifier imageType(DdsPixelformat pf) {
        // shortcuts to not have to derive the dxgi/d3d format first
        int f = pf.dwFlags();
        if ((f & DDPF_RGB) == DDPF_RGB) {
            return packedRGB(
                    false,
                    pf.dwRGBBitCount(),
                    pf.dwRBitMask(),
                    pf.dwGBitMask(),
                    pf.dwBBitMask(),
                    (f & DDPF_ALPHAPIXELS) == DDPF_ALPHAPIXELS ? pf.dwABitMask() : 0,
                    false
            );
        } else if ((f & DDPF_ALPHA) == DDPF_ALPHA) {
            return packedRGB(
                    false,
                    pf.dwRGBBitCount(),
                    0,
                    0,
                    0,
                    pf.dwABitMask(),
                    false
            );
        } else if (isBlockCompressed(pf)) {
            // special case for block compressed formats derived from the fourCC code
            // because of the alphaPremultiplied check
            return packedRGB(
                    false,
                    32,
                    0x00ff0000,
                    0x0000ff00,
                    0x000000ff,
                    0xff000000,
                    pf.dwFourCC() == D3DFMT_DXT2 || pf.dwFourCC() == D3DFMT_DXT4
            );
        }

        DxgiFormat dxgiFormat = pf.deriveDxgiFormat();
        if (dxgiFormat != DxgiFormat.UNKNOWN) {
            try {
                return imageType(dxgiFormat, false);
            } catch (Exception e) {
                throw new UnsupportedOperationException("unsupported format " + pf, e);
            }
        } else {
            D3dFormat d3dFormat = pf.deriveD3dFormat();
            if (d3dFormat != D3dFormat.UNKNOWN) {
                try {
                    return imageType(d3dFormat);
                } catch (Exception e) {
                    throw new UnsupportedOperationException("unsupported format " + pf, e);
                }
            }
        }

        throw new UnsupportedOperationException("unsupported format " + pf);
    }

    /**
     * Get the image type specifier for the resulting BufferedImage after decompression / to-RGB conversion.
     */
    private static ImageTypeSpecifier imageType(D3dFormat format) {
        // TODO: implement some fallbacks
        throw new UnsupportedOperationException("unsupported format " + format);
    }

    /**
     * Get the image type specifier for the resulting BufferedImage after decompression / to-RGB conversion.
     */
    private static ImageTypeSpecifier imageType(DxgiFormat format, boolean alphaPremultiplied) {
        return switch (format) {
            case R10G10B10A2_UNORM, R10G10B10A2_UINT -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0x000003ff,
                    0x000ffc00,
                    0x3ff00000,
                    0xc0000000,
                    alphaPremultiplied
            );
            case R8G8B8A8_UNORM, R8G8B8A8_UNORM_SRGB, R8G8B8A8_UINT -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0x000000ff,
                    0x0000ff00,
                    0x00ff0000,
                    0xff000000,
                    alphaPremultiplied
            );
            case R16G16_UNORM, R16G16_UINT -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0x0000ffff,
                    0xffff0000,
                    0,
                    0,
                    alphaPremultiplied
            );
            case R32_UINT -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0xffffffff,
                    0,
                    0,
                    0,
                    alphaPremultiplied
            );
            case R8G8_UNORM, R8G8_UINT -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0x00ff,
                    0xff00,
                    0,
                    0,
                    alphaPremultiplied
            );
            case R16_UNORM, R16_UINT -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0xffff,
                    0,
                    0,
                    0,
                    alphaPremultiplied
            );
            case R8_UNORM, R8_UINT -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0xff,
                    0,
                    0,
                    0,
                    alphaPremultiplied
            );
            case A8_UNORM -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0,
                    0,
                    0,
                    0xff,
                    alphaPremultiplied
            );
            case B5G6R5_UNORM -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0xf800,
                    0x07e0,
                    0x001f,
                    0,
                    alphaPremultiplied
            );
            case B5G5R5A1_UNORM -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0x7c00,
                    0x03e0,
                    0x001f,
                    0x8000,
                    alphaPremultiplied
            );
            case B8G8R8A8_UNORM, B8G8R8X8_UNORM, B8G8R8A8_UNORM_SRGB, B8G8R8X8_UNORM_SRGB -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0x00ff0000,
                    0x0000ff00,
                    0x000000ff,
                    switch (format) {
                        case B8G8R8X8_UNORM, B8G8R8X8_UNORM_SRGB -> 0;
                        default -> 0xff000000;
                    },
                    alphaPremultiplied
            );
            case B4G4R4A4_UNORM -> packedRGB(
                    format.isSRGB(),
                    format.getBitsPerPixel(),
                    0x0f00,
                    0x00f0,
                    0x000f,
                    0xf000,
                    alphaPremultiplied
            );
            case BC1_UNORM, BC1_UNORM_SRGB,
                 BC2_UNORM, BC2_UNORM_SRGB,
                 BC3_UNORM, BC3_UNORM_SRGB,
                 BC7_UNORM, BC7_UNORM_SRGB -> packedRGB(
                    format.isSRGB(),
                    32,
                    0x00ff0000,
                    0x0000ff00,
                    0x000000ff,
                    // technically BC1 and BC7 can have no alpha per compression block, but we ignore that here
                    0xff000000,
                    alphaPremultiplied
            );
            default -> throw new UnsupportedOperationException("unsupported format " + format);
        };
    }

    private static boolean isBlockCompressed(DdsPixelformat pf) {
        if ((pf.dwFlags() & DDPF_FOURCC) == DDPF_FOURCC) {
            int fcc = pf.dwFourCC();
            return fcc == D3DFMT_DXT1 || fcc == D3DFMT_DXT2 || fcc == D3DFMT_DXT3 || fcc == D3DFMT_DXT4 || fcc == D3DFMT_DXT5
                    || fcc == DXGI_FORMAT_BC4_UNORM || fcc == DXGI_FORMAT_BC4_UNORM_ALT || fcc == DXGI_FORMAT_BC4_SNORM
                    || fcc == DXGI_FORMAT_BC5_UNORM || fcc == DXGI_FORMAT_BC5_SNORM;
        }

        return false;
    }

    private static int findExactIntegerTransferType(int bpp) {
        if (bpp == 8) {
            return DataBuffer.TYPE_BYTE;
        } else if (bpp == 16) {
            return DataBuffer.TYPE_USHORT;
        } else if (bpp == 32) {
            return DataBuffer.TYPE_INT;
        } else {
            throw new UnsupportedOperationException("cannot find exact transfer type for bpp " + bpp);
        }
    }

    private static ImageTypeSpecifier packedRGB(boolean sRGB, int bpp, int rmask, int gmask, int bmask, int amask, boolean isAlphaPremultiplied) {
        ColorModel cm = new DirectColorModel(
                ColorSpace.getInstance(sRGB ? ColorSpace.CS_sRGB : ColorSpace.CS_LINEAR_RGB),
                bpp,
                rmask,
                gmask,
                bmask,
                amask,
                isAlphaPremultiplied,
                findExactIntegerTransferType(bpp)
        );
        return new ImageTypeSpecifier(
                cm,
                cm.createCompatibleSampleModel(1, 1)
        );
    }

    public static int ceilDivUnsigned(int dividend, int divisor) {
        if (dividend == 0) {
            return 0;
        } else if (divisor == 1) {
            return dividend;
        }

        return 1 + Integer.divideUnsigned(dividend - 1, divisor);
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
}
