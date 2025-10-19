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
            return imageType(file.header10().dxgiFormat(), file.header10().isAlphaPremultiplied());
        }

        return imageType(file.header());
    }

    /**
     * Get the image type specifier for the resulting BufferedImage after decompression / to-RGB conversion.
     */
    private static ImageTypeSpecifier imageType(DdsHeader header) {
        DdsPixelformat pf = header.ddspf();
        int f = pf.dwFlags();

        // we only look at the pf values when the FOURCC flag is NOT set
        if ((f & DDPF_FOURCC) != DDPF_FOURCC) {
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
            } else if ((f & DDPF_YUV) == DDPF_YUV) {
                throw new UnsupportedOperationException("unsupported YUV format " + header);
            } else if ((f & DDPF_LUMINANCE) == DDPF_LUMINANCE) {
                throw new UnsupportedOperationException("unsupported luminance format " + header);
            }
        }

        if (isBlockCompressed(pf)) {
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
        } else {
            DxgiFormat dxgiFormat = deriveDxgiFormat(header);
            if (dxgiFormat != DxgiFormat.UNKNOWN) {
                try {
                    return imageType(dxgiFormat, false);
                } catch (Exception e) {
                    throw new UnsupportedOperationException("unsupported format " + header, e);
                }
            }
        }

        throw new UnsupportedOperationException("unsupported format " + header);
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

    public static DxgiFormat deriveDxgiFormat(DdsHeader header) {
        DdsPixelformat pf = header.ddspf();
        int fcc = pf.dwFourCC();
        if ((pf.dwFlags() & DdsConstants.DDPF_FOURCC) == DdsConstants.DDPF_FOURCC) {
            if (fcc == D3DFMT_DXT1) {
                return DxgiFormat.BC1_UNORM;
            } else if (fcc == D3DFMT_DXT2 || fcc == D3DFMT_DXT3) {
                return DxgiFormat.BC2_UNORM;
            } else if (fcc == D3DFMT_DXT4 || fcc == D3DFMT_DXT5) {
                return DxgiFormat.BC3_UNORM;
            } else if (fcc == DXGI_FORMAT_BC4_UNORM || fcc == DXGI_FORMAT_BC4_UNORM_ALT) {
                return DxgiFormat.BC4_UNORM;
            } else if (fcc == DXGI_FORMAT_BC4_SNORM) {
                return DxgiFormat.BC4_SNORM;
            } else if (fcc == DXGI_FORMAT_BC5_UNORM) {
                return DxgiFormat.BC5_UNORM;
            } else if (fcc == DXGI_FORMAT_BC5_SNORM) {
                return DxgiFormat.BC5_SNORM;
            } else if (fcc == D3DFMT_R8G8_B8G8) {
                return DxgiFormat.R8G8_B8G8_UNORM;
            } else if (fcc == D3DFMT_G8R8_G8B8) {
                return DxgiFormat.G8R8_G8B8_UNORM;
            } else if (fcc == D3dFormat.A16B16G16R16.value()) {
                return DxgiFormat.R16G16B16A16_UNORM;
            } else if (fcc == D3dFormat.Q16W16V16U16.value()) {
                return DxgiFormat.R16G16B16A16_SNORM;
            } else if (fcc == D3dFormat.R16F.value()) {
                return DxgiFormat.R16_FLOAT;
            } else if (fcc == D3dFormat.G16R16F.value()) {
                return DxgiFormat.R16G16_FLOAT;
            } else if (fcc == D3dFormat.A16B16G16R16F.value()) {
                return DxgiFormat.R16G16B16A16_FLOAT;
            } else if (fcc == D3dFormat.R32F.value()) {
                return DxgiFormat.R32_FLOAT;
            } else if (fcc == D3dFormat.A32B32G32R32F.value()) {
                return DxgiFormat.R32G32B32A32_FLOAT;
            }
        }
        // TODO: more derivations
        return DxgiFormat.UNKNOWN;
    }

    private static boolean isBlockCompressed(DdsPixelformat pf) {
        if ((pf.dwFlags() & DdsConstants.DDPF_FOURCC) == DdsConstants.DDPF_FOURCC) {
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
}
