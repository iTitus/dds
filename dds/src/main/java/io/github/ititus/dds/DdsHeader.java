package io.github.ititus.dds;

import io.github.ititus.dds.internal.Util;

import java.io.IOException;
import java.util.StringJoiner;

import static io.github.ititus.dds.DdsConstants.*;

public record DdsHeader(
        int dwSize,
        int dwFlags,
        int dwHeight,
        int dwWidth,
        int dwPitchOrLinearSize,
        int dwDepth,
        int dwMipMapCount,
        int dwReserved1_0,
        int dwReserved1_1,
        int dwReserved1_2,
        int dwReserved1_3,
        int dwReserved1_4,
        int dwReserved1_5,
        int dwReserved1_6,
        int dwReserved1_7,
        int dwReserved1_8,
        int dwReserved1_9,
        int dwReserved1_10,
        DdsPixelformat ddspf,
        int dwCaps,
        int dwCaps2,
        int dwCaps3,
        int dwCaps4,
        int dwReserved2
) {

    public static final int SIZE = 124;

    public static DdsHeader load(DataReader r) throws IOException {
        return new DdsHeader(
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                DdsPixelformat.load(r),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword()
        );
    }

    public boolean isValid() {
        return isValid(false);
    }

    public boolean isValid(boolean strict) {
        if (dwSize != SIZE) {
            return false;
        } else if (strict && (dwFlags & DDS_HEADER_FLAGS_TEXTURE) != DDS_HEADER_FLAGS_TEXTURE) {
            return false;
        } else if (strict && (dwCaps & DDSCAPS_TEXTURE) != DDSCAPS_TEXTURE) {
            return false;
        } else if (isUncompressed() && isCompressed()) {
            return false;
        } else if (isCubemap() && isVolumeTexture()) {
            return false;
        } else if (hasDepth() != isVolumeTexture()) {
            return false;
        } else if (strict && isBlockCompressed() && (dwHeight % 4 != 0 || dwWidth % 4 != 0)) {
            return false;
        }

        return ddspf.isValid(strict);
    }

    public boolean shouldLoadHeader10() {
        return ddspf.shouldLoadHeader10();
    }

    public boolean isUncompressed() {
        return (dwFlags & DDS_HEADER_FLAGS_PITCH) == DDS_HEADER_FLAGS_PITCH;
    }

    public boolean isCompressed() {
        return (dwFlags & DDS_HEADER_FLAGS_LINEARSIZE) == DDS_HEADER_FLAGS_LINEARSIZE;
    }

    public boolean isBlockCompressed() {
        return d3dFormat().isBlockCompressed();
    }

    public boolean hasDepth() {
        return (dwFlags & DDS_HEADER_FLAGS_VOLUME) == DDS_HEADER_FLAGS_VOLUME;
    }

    public boolean hasMipmaps() {
        return (dwFlags & DDSD_MIPMAPCOUNT) == DDSD_MIPMAPCOUNT && (dwCaps & DDSCAPS_MIPMAP) == DDSCAPS_MIPMAP;
    }

    public boolean isFlatTexture() {
        return !isCubemap() && !isVolumeTexture();
    }

    public boolean isCubemap() {
        return (dwCaps2 & DDSCAPS2_CUBEMAP) == DDSCAPS2_CUBEMAP;
    }

    public boolean isVolumeTexture() {
        return (dwCaps2 & DDSCAPS2_VOLUME) == DDSCAPS2_VOLUME;
    }

    public D3dFormat d3dFormat() {
        return ddspf.deriveD3dFormat();
    }

    public int countCubemapFaces() {
        int faces = 0;
        if ((dwCaps2 & DDSCAPS2_CUBEMAP_POSITIVEX) == DDSCAPS2_CUBEMAP_POSITIVEX) {
            faces++;
        }
        if ((dwCaps2 & DDSCAPS2_CUBEMAP_NEGATIVEX) == DDSCAPS2_CUBEMAP_NEGATIVEX) {
            faces++;
        }
        if ((dwCaps2 & DDSCAPS2_CUBEMAP_POSITIVEY) == DDSCAPS2_CUBEMAP_POSITIVEY) {
            faces++;
        }
        if ((dwCaps2 & DDSCAPS2_CUBEMAP_NEGATIVEY) == DDSCAPS2_CUBEMAP_NEGATIVEY) {
            faces++;
        }
        if ((dwCaps2 & DDSCAPS2_CUBEMAP_POSITIVEZ) == DDSCAPS2_CUBEMAP_POSITIVEZ) {
            faces++;
        }
        if ((dwCaps2 & DDSCAPS2_CUBEMAP_NEGATIVEZ) == DDSCAPS2_CUBEMAP_NEGATIVEZ) {
            faces++;
        }
        return faces;
    }

    @Override
    public String toString() {
        StringJoiner j = new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]");
        if (dwSize != SIZE) {
            j.add("dwSize=" + Integer.toUnsignedString(dwSize));
        }
        if (dwFlags != 0) {
            StringJoiner j2 = new StringJoiner(",", "dwFlags=[", "]");
            if ((dwFlags & DDSD_CAPS) == DDSD_CAPS) {
                j2.add("caps");
            }
            if ((dwFlags & DDSD_HEIGHT) == DDSD_HEIGHT) {
                j2.add("height");
            }
            if ((dwFlags & DDSD_WIDTH) == DDSD_WIDTH) {
                j2.add("width");
            }
            if ((dwFlags & DDSD_PITCH) == DDSD_PITCH) {
                j2.add("pitch");
            }
            if ((dwFlags & DDSD_PIXELFORMAT) == DDSD_PIXELFORMAT) {
                j2.add("pixelformat");
            }
            if ((dwFlags & DDSD_MIPMAPCOUNT) == DDSD_MIPMAPCOUNT) {
                j2.add("mipmapcount");
            }
            if ((dwFlags & DDSD_LINEARSIZE) == DDSD_LINEARSIZE) {
                j2.add("linearsize");
            }
            if ((dwFlags & DDSD_DEPTH) == DDSD_DEPTH) {
                j2.add("depth");
            }
            j.add(j2.toString());
            if ((dwFlags & ~(DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH | DDSD_PITCH | DDSD_PIXELFORMAT | DDSD_MIPMAPCOUNT | DDSD_LINEARSIZE | DDSD_DEPTH)) != 0) {
                j.add("dwFlags=0x" + Integer.toHexString(dwFlags));
            }
        }
        j.add("dwHeight=" + dwHeight);
        j.add("dwWidth=" + dwWidth);
        if (dwPitchOrLinearSize != 0) {
            j.add((isUncompressed() ? "dwPitch=" : isCompressed() ? "dwLinearSize=" : "dwPitchOrLinearSize=") + Integer.toUnsignedString(dwPitchOrLinearSize));
        }
        if (dwDepth != 1) {
            j.add("dwDepth=" + Integer.toUnsignedString(dwDepth));
        }
        if (dwMipMapCount != 0) {
            j.add("dwMipMapCount=" + Integer.toUnsignedString(dwMipMapCount));
        }
        if (dwReserved1_0 != 0) {
            j.add("dwReserved[0]=" + Util.guessToString(dwReserved1_0));
        }
        if (dwReserved1_1 != 0) {
            j.add("dwReserved[1]=" + Util.guessToString(dwReserved1_1));
        }
        if (dwReserved1_2 != 0) {
            j.add("dwReserved[2]=" + Util.guessToString(dwReserved1_2));
        }
        if (dwReserved1_3 != 0) {
            j.add("dwReserved[3]=" + Util.guessToString(dwReserved1_3));
        }
        if (dwReserved1_4 != 0) {
            j.add("dwReserved[4]=" + Util.guessToString(dwReserved1_4));
        }
        if (dwReserved1_5 != 0) {
            j.add("dwReserved[5]=" + Util.guessToString(dwReserved1_5));
        }
        if (dwReserved1_6 != 0) {
            j.add("dwReserved[6]=" + Util.guessToString(dwReserved1_6));
        }
        if (dwReserved1_7 != 0) {
            j.add("dwReserved[7]=" + Util.guessToString(dwReserved1_7));
        }
        if (dwReserved1_8 != 0) {
            j.add("dwReserved[8]=" + Util.guessToString(dwReserved1_8));
        }
        if (dwReserved1_9 != 0) {
            j.add("dwReserved[9]=" + Util.guessToString(dwReserved1_9));
        }
        if (dwReserved1_10 != 0) {
            j.add("dwReserved[10]=" + Util.guessToString(dwReserved1_10));
        }
        if (dwCaps != 0) {
            StringJoiner j2 = new StringJoiner(",", "dwCaps=[", "]");
            if ((dwCaps & DDSCAPS_COMPLEX) == DDSCAPS_COMPLEX) {
                j2.add("complex");
            }
            if ((dwCaps & DDSCAPS_MIPMAP) == DDSCAPS_MIPMAP) {
                j2.add("mipmap");
            }
            if ((dwCaps & DDSCAPS_TEXTURE) == DDSCAPS_TEXTURE) {
                j2.add("texture");
            }
            j.add(j2.toString());
            if ((dwCaps & ~(DDSCAPS_COMPLEX | DDSCAPS_MIPMAP | DDSCAPS_TEXTURE)) != 0) {
                j.add("dwCaps=0x" + Integer.toHexString(dwCaps));
            }
        }
        if (dwCaps2 != 0) {
            StringJoiner j2 = new StringJoiner(",", "dwCaps2=[", "]");
            if ((dwCaps2 & DDSCAPS2_CUBEMAP) == DDSCAPS2_CUBEMAP) {
                j2.add("cubemap");
            }
            if ((dwCaps2 & DDSCAPS2_CUBEMAP_POSITIVEX) == DDSCAPS2_CUBEMAP_POSITIVEX) {
                j2.add("cubemapPositiveX");
            }
            if ((dwCaps2 & DDSCAPS2_CUBEMAP_NEGATIVEX) == DDSCAPS2_CUBEMAP_NEGATIVEX) {
                j2.add("cubemapNegativeX");
            }
            if ((dwCaps2 & DDSCAPS2_CUBEMAP_POSITIVEY) == DDSCAPS2_CUBEMAP_POSITIVEY) {
                j2.add("cubemapPositiveY");
            }
            if ((dwCaps2 & DDSCAPS2_CUBEMAP_NEGATIVEY) == DDSCAPS2_CUBEMAP_NEGATIVEY) {
                j2.add("cubemapNegativeY");
            }
            if ((dwCaps2 & DDSCAPS2_CUBEMAP_POSITIVEZ) == DDSCAPS2_CUBEMAP_POSITIVEZ) {
                j2.add("cubemapPositiveZ");
            }
            if ((dwCaps2 & DDSCAPS2_CUBEMAP_NEGATIVEZ) == DDSCAPS2_CUBEMAP_NEGATIVEZ) {
                j2.add("cubemapNegativeZ");
            }
            if ((dwCaps2 & DDSCAPS2_VOLUME) == DDSCAPS2_VOLUME) {
                j2.add("volume");
            }
            j.add(j2.toString());
            if ((dwCaps2 & ~(DDSCAPS2_CUBEMAP | DDSCAPS2_CUBEMAP_POSITIVEX | DDSCAPS2_CUBEMAP_NEGATIVEX | DDSCAPS2_CUBEMAP_POSITIVEY | DDSCAPS2_CUBEMAP_NEGATIVEY | DDSCAPS2_CUBEMAP_POSITIVEZ | DDSCAPS2_CUBEMAP_NEGATIVEZ | DDSCAPS2_VOLUME)) != 0) {
                j.add("dwCaps2=0x" + Integer.toHexString(dwCaps2));
            }
        }
        if (dwCaps3 != 0) {
            j.add("dwCaps3=0x" + Integer.toHexString(dwCaps3));
        }
        if (dwCaps4 != 0) {
            j.add("dwCaps4=0x" + Integer.toHexString(dwCaps4));
        }
        if (dwReserved2 != 0) {
            j.add("dwReserved2=" + Util.guessToString(dwReserved2));
        }
        j.add("ddspf=" + ddspf);
        return j.toString();
    }
}
