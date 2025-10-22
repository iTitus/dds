package io.github.ititus.dds;

import java.io.IOException;
import java.util.StringJoiner;

import static io.github.ititus.dds.DdsConstants.*;

public record DdsPixelformat(
        int dwSize,
        int dwFlags,
        int dwFourCC,
        int dwRGBBitCount,
        int dwRBitMask,
        int dwGBitMask,
        int dwBBitMask,
        int dwABitMask
) {

    public static final int SIZE = 32;

    public static DdsPixelformat load(DataReader r) throws IOException {
        return new DdsPixelformat(
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword(),
                r.readDword()
        );
    }

    public boolean shouldLoadHeader10() {
        return dwFourCC == DDS_DX10;
    }

    public boolean isValid(boolean strict) {
        if (dwSize != SIZE) {
            return false;
        } else {
            return shouldLoadHeader10() || deriveD3dFormat().getBitsPerPixel() != 0;
        }
    }

    public D3dFormat deriveD3dFormat() {
        return DdsHelper.deriveD3dFormat(this);
    }

    public DxgiFormat deriveDxgiFormat() {
        return DdsHelper.deriveDxgiFormat(this);
    }

    @Override
    public String toString() {
        StringJoiner j = new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]");
        if (dwSize != SIZE) {
            j.add("dwSize=" + Integer.toUnsignedString(dwSize));
        }
        if (dwFlags != 0) {
            StringJoiner j2 = new StringJoiner(",", "dwFlags=[", "]");
            if ((dwFlags & DDPF_ALPHAPIXELS) == DDPF_ALPHAPIXELS) {
                j2.add("alphaPixels");
            }
            if ((dwFlags & DDPF_ALPHA) == DDPF_ALPHA) {
                j2.add("alpha");
            }
            if ((dwFlags & DDPF_FOURCC) == DDPF_FOURCC) {
                j2.add("fourCC");
            }
            if ((dwFlags & DDPF_RGB) == DDPF_RGB) {
                j2.add("rgb");
            }
            if ((dwFlags & DDPF_YUV) == DDPF_YUV) {
                j2.add("yuv");
            }
            if ((dwFlags & DDPF_LUMINANCE) == DDPF_LUMINANCE) {
                j2.add("luminance");
            }
            j.add(j2.toString());
            if ((dwFlags & ~(DDPF_ALPHAPIXELS | DDPF_ALPHA | DDPF_FOURCC | DDPF_RGB | DDPF_YUV | DDPF_LUMINANCE)) != 0) {
                j.add("dwFlags=0x" + Integer.toHexString(dwFlags));
            }
        }
        if (dwFourCC != 0) {
            j.add("dwFourCC=" + getStringFrom4CC(dwFourCC));
        }
        if (dwRGBBitCount != 0) {
            j.add("dwRGBBitCount=" + Integer.toUnsignedString(dwRGBBitCount));
        }
        if (dwRBitMask != 0) {
            j.add("dwRBitMask=0x" + Integer.toHexString(dwRBitMask));
        }
        if (dwGBitMask != 0) {
            j.add("dwGBitMask=0x" + Integer.toHexString(dwGBitMask));
        }
        if (dwBBitMask != 0) {
            j.add("dwBBitMask=0x" + Integer.toHexString(dwBBitMask));
        }
        if (dwABitMask != 0) {
            j.add("dwABitMask=0x" + Integer.toHexString(dwABitMask));
        }
        return j.toString();
    }
}
