package io.github.ititus.dds;

import java.io.IOException;
import java.util.StringJoiner;

import static io.github.ititus.dds.DdsConstants.*;

public record DdsHeaderDxt10(
        DxgiFormat dxgiFormat,
        D3d10ResourceDimension resourceDimension,
        int miscFlag,
        int arraySize,
        int miscFlags2
) {

    public static final int SIZE = 20;

    public static DdsHeaderDxt10 load(DataReader r) throws IOException {
        return new DdsHeaderDxt10(
                DxgiFormat.load(r),
                D3d10ResourceDimension.load(r),
                r.readUInt(),
                r.readUInt(),
                r.readUInt()
        );
    }

    public boolean isValid(DdsHeader header) {
        if (arraySize == 0) {
            return false;
        } else if (dxgiFormat.getBitsPerPixel() == 0) {
            return false;
        } else if (header.isVolumeTexture() && arraySize != 1) {
            return false;
        }

        switch (resourceDimension) {
            case D3D10_RESOURCE_DIMENSION_TEXTURE1D -> {
                if ((header.dwFlags() & DdsConstants.DDSD_HEIGHT) != DdsConstants.DDSD_HEIGHT || header.dwHeight() != 1) {
                    return false;
                }
            }
            case D3D10_RESOURCE_DIMENSION_TEXTURE2D -> {}
            case D3D10_RESOURCE_DIMENSION_TEXTURE3D -> {
                if ((header.dwFlags() & DdsConstants.DDSD_DEPTH) != DdsConstants.DDSD_DEPTH) {
                    return false;
                } else if (arraySize != 1) {
                    return false;
                }
            }
            default -> {
                return false;
            }
        }

        return true;
    }

    public boolean isAlphaPremultiplied() {
        return (miscFlags2 & DDS_ALPHA_MODE_PREMULTIPLIED) == DDS_ALPHA_MODE_PREMULTIPLIED;
    }

    @Override
    public String toString() {
        StringJoiner j = new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]");
        if (dxgiFormat != DxgiFormat.UNKNOWN) {
            j.add("dxgiFormat=" + dxgiFormat);
        }
        if (resourceDimension != D3d10ResourceDimension.D3D10_RESOURCE_DIMENSION_UNKNOWN) {
            j.add("resourceDimension=" + resourceDimension);
        }
        if (miscFlag != 0) {
            StringJoiner j2 = new StringJoiner(",", "miscFlag=[", "]");
            if ((miscFlag & DDS_RESOURCE_MISC_TEXTURECUBE) == DDS_RESOURCE_MISC_TEXTURECUBE) {
                j2.add("texturecube");
            }
            j.add(j2.toString());
            if ((miscFlag & ~DDS_RESOURCE_MISC_TEXTURECUBE) != 0) {
                j.add("miscFlag=0x" + Integer.toHexString(miscFlag));
            }
        }
        if (arraySize != 1) {
            j.add("arraySize=" + Integer.toUnsignedString(arraySize));
        }
        if (miscFlags2 != 0) {
            StringJoiner j2 = new StringJoiner(",", "miscFlags2=[", "]");
            if ((miscFlags2 & DDS_MISC_FLAGS2_ALPHA_MODE_MASK) <= DDS_ALPHA_MODE_CUSTOM) {
                j2.add("alphaMode=" + switch (miscFlags2 & DDS_MISC_FLAGS2_ALPHA_MODE_MASK) {
                    case DDS_ALPHA_MODE_UNKNOWN -> "unknown";
                    case DDS_ALPHA_MODE_STRAIGHT -> "straight";
                    case DDS_ALPHA_MODE_PREMULTIPLIED -> "premultiplied";
                    case DDS_ALPHA_MODE_OPAQUE -> "opaque";
                    case DDS_ALPHA_MODE_CUSTOM -> "custom";
                    default -> throw new AssertionError();
                });
            }
            j.add(j2.toString());
            if ((miscFlags2 & ~DDS_MISC_FLAGS2_ALPHA_MODE_MASK) != 0 || (miscFlags2 & DDS_MISC_FLAGS2_ALPHA_MODE_MASK) > DDS_ALPHA_MODE_CUSTOM) {
                j.add("miscFlags2=0x" + Integer.toHexString(miscFlags2));
            }
        }
        return j.toString();
    }
}
