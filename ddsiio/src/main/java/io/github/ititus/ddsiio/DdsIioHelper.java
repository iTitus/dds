package io.github.ititus.ddsiio;

import io.github.ititus.dds.*;

import javax.imageio.ImageTypeSpecifier;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;

import static io.github.ititus.dds.DdsConstants.*;

public final class DdsIioHelper {

    private DdsIioHelper() {}

    public static final int RGB_COLORSPACE = ColorSpace.CS_sRGB;

    public static ImageTypeSpecifier imageType(DdsFile file) {
        if (file.isDx10()) {
            return imageType(file.header10());
        }

        return imageType(file.header());
    }

    public static ImageTypeSpecifier imageType(DdsHeader header) {
        return imageType(header.ddspf());
    }

    public static ImageTypeSpecifier imageType(DdsPixelformat pixelformat) {
        if ((pixelformat.dwFlags() & DDS_RGBA) == DDS_RGBA) {
            ColorModel cm = new DirectColorModel(
                    ColorSpace.getInstance(RGB_COLORSPACE),
                    pixelformat.dwRGBBitCount(),
                    pixelformat.dwRBitMask(),
                    pixelformat.dwGBitMask(),
                    pixelformat.dwBBitMask(),
                    pixelformat.dwABitMask(),
                    false,
                    findBestTransferType(pixelformat)
            );
            return new ImageTypeSpecifier(
                    cm,
                    cm.createCompatibleSampleModel(1, 1)
            );
        } else if ((pixelformat.dwFlags() & DDPF_RGB) == DDPF_RGB) {
            ColorModel cm = new DirectColorModel(
                    ColorSpace.getInstance(RGB_COLORSPACE),
                    pixelformat.dwRGBBitCount(),
                    pixelformat.dwRBitMask(),
                    pixelformat.dwGBitMask(),
                    pixelformat.dwBBitMask(),
                    0,
                    false,
                    findBestTransferType(pixelformat)
            );
            return new ImageTypeSpecifier(
                    cm,
                    cm.createCompatibleSampleModel(1, 1)
            );
        } else if ((pixelformat.dwFlags() & DdsConstants.DDPF_FOURCC) == DdsConstants.DDPF_FOURCC) {
            if (pixelformat.dwFourCC() == D3DFMT_DXT1) {
                ColorModel cm = new DirectColorModel(
                        ColorSpace.getInstance(RGB_COLORSPACE),
                        17,
                        0xf800,
                        0x7e0,
                        0x1f,
                        0x10000,
                        false,
                        DataBuffer.TYPE_INT
                );
                return new ImageTypeSpecifier(
                        cm,
                        cm.createCompatibleSampleModel(1, 1)
                );
            } else if (pixelformat.dwFourCC() == D3DFMT_DXT2 || pixelformat.dwFourCC() == D3DFMT_DXT3) {
                ColorModel cm = new DirectColorModel(
                        ColorSpace.getInstance(RGB_COLORSPACE),
                        20,
                        0xf800,
                        0x7e0,
                        0x1f,
                        0xf0000,
                        pixelformat.dwFourCC() == D3DFMT_DXT2,
                        DataBuffer.TYPE_INT
                );
                return new ImageTypeSpecifier(
                        cm,
                        cm.createCompatibleSampleModel(1, 1)
                );
            } else if (pixelformat.dwFourCC() == D3DFMT_DXT4 || pixelformat.dwFourCC() == D3DFMT_DXT5) {
                ColorModel cm = new DirectColorModel(
                        ColorSpace.getInstance(RGB_COLORSPACE),
                        24,
                        0xf800,
                        0x7e0,
                        0x1f,
                        0xff0000,
                        pixelformat.dwFourCC() == D3DFMT_DXT4,
                        DataBuffer.TYPE_INT
                );
                return new ImageTypeSpecifier(
                        cm,
                        cm.createCompatibleSampleModel(1, 1)
                );
            }
        }

        throw new UnsupportedOperationException();
    }

    public static ImageTypeSpecifier imageType(DdsHeaderDxt10 header10) {
        throw new UnsupportedOperationException("cannot get ImageTypeSpecifier from DX10 header");
    }

    public static int findBestTransferType(DdsPixelformat pixelformat) {
        if (pixelformat.dwRGBBitCount() <= 8) {
            return DataBuffer.TYPE_BYTE;
        } else if (pixelformat.dwRGBBitCount() <= 16) {
            return DataBuffer.TYPE_USHORT;
        } else if (pixelformat.dwRGBBitCount() <= 32) {
            return DataBuffer.TYPE_INT;
        }

        return DataBuffer.TYPE_UNDEFINED;
    }
}
