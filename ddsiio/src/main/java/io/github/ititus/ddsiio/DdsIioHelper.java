package io.github.ititus.ddsiio;

import io.github.ititus.dds.*;

import javax.imageio.ImageTypeSpecifier;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;

import static io.github.ititus.dds.DdsConstants.*;

public final class DdsIioHelper {

    public static final int RGB_COLORSPACE = ColorSpace.CS_sRGB;

    private DdsIioHelper() {}

    public static ImageTypeSpecifier imageType(DdsFile file) {
        if (file.isDxt10()) {
            return imageType(file.header(), file.header10());
        }

        return imageType(file.header());
    }

    public static ImageTypeSpecifier imageType(DdsHeader header) {
        D3dFormat format = header.d3dFormat();
        DdsPixelformat pf = header.ddspf();
        if ((pf.dwFlags() & DDS_RGBA) == DDS_RGBA) {
            ColorModel cm = new DirectColorModel(
                    ColorSpace.getInstance(RGB_COLORSPACE),
                    pf.dwRGBBitCount(),
                    pf.dwRBitMask(),
                    pf.dwGBitMask(),
                    pf.dwBBitMask(),
                    pf.dwABitMask(),
                    false,
                    findBestTransferType(format)
            );
            return new ImageTypeSpecifier(
                    cm,
                    cm.createCompatibleSampleModel(1, 1)
            );
        } else if ((pf.dwFlags() & DDPF_RGB) == DDPF_RGB) {
            ColorModel cm = new DirectColorModel(
                    ColorSpace.getInstance(RGB_COLORSPACE),
                    pf.dwRGBBitCount(),
                    pf.dwRBitMask(),
                    pf.dwGBitMask(),
                    pf.dwBBitMask(),
                    0,
                    false,
                    findBestTransferType(format)
            );
            return new ImageTypeSpecifier(
                    cm,
                    cm.createCompatibleSampleModel(1, 1)
            );
        } else if ((pf.dwFlags() & DdsConstants.DDPF_FOURCC) == DdsConstants.DDPF_FOURCC) {
            if (pf.dwFourCC() == D3DFMT_DXT1) {
                ColorModel cm = new DirectColorModel(
                        ColorSpace.getInstance(RGB_COLORSPACE),
                        32,
                        0x00ff0000,
                        0x0000ff00,
                        0x000000ff,
                        0xff000000,
                        false,
                        DataBuffer.TYPE_INT
                );
                return new ImageTypeSpecifier(
                        cm,
                        cm.createCompatibleSampleModel(1, 1)
                );
            } else if (pf.dwFourCC() == D3DFMT_DXT2 || pf.dwFourCC() == D3DFMT_DXT3) {
                ColorModel cm = new DirectColorModel(
                        ColorSpace.getInstance(RGB_COLORSPACE),
                        32,
                        0x00ff0000,
                        0x0000ff00,
                        0x000000ff,
                        0xff000000,
                        pf.dwFourCC() == D3DFMT_DXT2,
                        DataBuffer.TYPE_INT
                );
                return new ImageTypeSpecifier(
                        cm,
                        cm.createCompatibleSampleModel(1, 1)
                );
            } else if (pf.dwFourCC() == D3DFMT_DXT4 || pf.dwFourCC() == D3DFMT_DXT5) {
                ColorModel cm = new DirectColorModel(
                        ColorSpace.getInstance(RGB_COLORSPACE),
                        32,
                        0x00ff0000,
                        0x0000ff00,
                        0x000000ff,
                        0xff000000,
                        pf.dwFourCC() == D3DFMT_DXT4,
                        DataBuffer.TYPE_INT
                );
                return new ImageTypeSpecifier(
                        cm,
                        cm.createCompatibleSampleModel(1, 1)
                );
            }
        }

        throw new UnsupportedOperationException("unsupported format: " + header);
    }

    public static ImageTypeSpecifier imageType(DdsHeader header, DdsHeaderDxt10 header10) {
        // TODO: support dxgi format as well
        throw new UnsupportedOperationException("unsupported format: " + header + " " + header10);
    }

    public static int findBestTransferType(PixelFormat format) {
        return findBestTransferType(format.getBitsPerPixel());
    }

    public static int findBestTransferType(int bpp) {
        if (bpp <= 8) {
            return DataBuffer.TYPE_BYTE;
        } else if (bpp <= 16) {
            return DataBuffer.TYPE_USHORT;
        } else if (bpp <= 32) {
            return DataBuffer.TYPE_INT;
        }

        return DataBuffer.TYPE_UNDEFINED;
    }
}
