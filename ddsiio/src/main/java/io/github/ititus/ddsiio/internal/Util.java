package io.github.ititus.ddsiio.internal;

import io.github.ititus.dds.*;

import javax.imageio.ImageTypeSpecifier;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;

import static io.github.ititus.dds.DdsConstants.*;

public final class Util {

    private Util() {}

    public static ImageTypeSpecifier imageType(DdsFile file) {
        if (file.isDxt10()) {
            return imageType(file.header(), file.header10());
        }

        return imageType(file.header());
    }

    /**
     * Get the image type specifier for the resulting BufferedImage after decompression / to-RGB conversion.
     */
    public static ImageTypeSpecifier imageType(DdsHeader header) {
        DdsPixelformat pf = header.ddspf();
        if ((pf.dwFlags() & DDPF_RGB) == DDPF_RGB) {
            return packed(
                    false,
                    pf.dwRGBBitCount(),
                    pf.dwRBitMask(),
                    pf.dwGBitMask(),
                    pf.dwBBitMask(),
                    (pf.dwFlags() & DDPF_ALPHAPIXELS) == DDPF_ALPHAPIXELS ? pf.dwABitMask() : 0,
                    false
            );
        } else if (BC.isBlockCompressed(pf)) {
            return packed(
                    false,
                    32,
                    0x00ff0000,
                    0x0000ff00,
                    0x000000ff,
                    0xff000000,
                    pf.dwFourCC() == D3DFMT_DXT2 || pf.dwFourCC() == D3DFMT_DXT4
            );
        }

        throw new UnsupportedOperationException("unsupported format: " + header);
    }

    /**
     * Get the image type specifier for the resulting BufferedImage after decompression / to-RGB conversion.
     */
    public static ImageTypeSpecifier imageType(DdsHeader header, DdsHeaderDxt10 header10) {
        DxgiFormat format = header10.dxgiFormat();
        switch (format) {
            case R10G10B10A2_UNORM, R10G10B10A2_UINT -> {
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0x000003ff,
                        0x000ffc00,
                        0x3ff00000,
                        0xc0000000,
                        header10.isAlphaPremultiplied()
                );
            }
            case R8G8B8A8_UNORM, R8G8B8A8_UNORM_SRGB, R8G8B8A8_UINT -> {
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0x000000ff,
                        0x0000ff00,
                        0x00ff0000,
                        0xff000000,
                        header10.isAlphaPremultiplied()
                );
            }
            case R16G16_UNORM, R16G16_UINT -> {
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0x0000ffff,
                        0xffff0000,
                        0,
                        0,
                        header10.isAlphaPremultiplied()
                );
            }
            case R32_UINT -> {
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0xffffffff,
                        0,
                        0,
                        0,
                        header10.isAlphaPremultiplied()
                );
            }
            case R8G8_UNORM, R8G8_UINT -> {
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0x00ff,
                        0xff00,
                        0,
                        0,
                        header10.isAlphaPremultiplied()
                );
            }
            case R16_UNORM, R16_UINT -> {
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0xffff,
                        0,
                        0,
                        0,
                        header10.isAlphaPremultiplied()
                );
            }
            case R8_UNORM, R8_UINT -> {
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0xff,
                        0,
                        0,
                        0,
                        header10.isAlphaPremultiplied()
                );
            }
            case A8_UNORM -> {
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0,
                        0,
                        0,
                        0xff,
                        header10.isAlphaPremultiplied()
                );
            }
            case B5G6R5_UNORM -> {
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0xf800,
                        0x07e0,
                        0x001f,
                        0,
                        header10.isAlphaPremultiplied()
                );
            }
            case B5G5R5A1_UNORM -> {
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0x7c00,
                        0x03e0,
                        0x001f,
                        0x8000,
                        header10.isAlphaPremultiplied()
                );
            }
            case B8G8R8A8_UNORM, B8G8R8X8_UNORM, B8G8R8A8_UNORM_SRGB, B8G8R8X8_UNORM_SRGB -> {
                int amask = switch (format) {
                    case B8G8R8X8_UNORM, B8G8R8X8_UNORM_SRGB -> 0;
                    default -> 0xff000000;
                };
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0x00ff0000,
                        0x0000ff00,
                        0x000000ff,
                        amask,
                        header10.isAlphaPremultiplied()
                );
            }
            case B4G4R4A4_UNORM -> {
                return packed(
                        format.isSRGB(),
                        format.getBitsPerPixel(),
                        0x0f00,
                        0x00f0,
                        0x000f,
                        0xf000,
                        header10.isAlphaPremultiplied()
                );
            }
            case BC1_UNORM, BC1_UNORM_SRGB,
                 BC2_UNORM, BC2_UNORM_SRGB,
                 BC3_UNORM, BC3_UNORM_SRGB,
                 BC7_UNORM, BC7_UNORM_SRGB -> {
                // technically BC1 and BC7 can have no alpha per compression block, but we ignore that here
                return packed(
                        format.isSRGB(),
                        32,
                        0x00ff0000,
                        0x0000ff00,
                        0x000000ff,
                        0xff000000,
                        header10.isAlphaPremultiplied()
                );
            }
        }

        throw new UnsupportedOperationException("unsupported format: " + header + " " + header10);
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

    private static ImageTypeSpecifier packed(boolean sRGB, int bpp, int rmask, int gmask, int bmask, int amask, boolean isAlphaPremultiplied) {
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
}
