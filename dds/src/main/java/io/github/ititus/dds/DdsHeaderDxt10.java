package io.github.ititus.dds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        return (miscFlags2 & DdsConstants.DDS_ALPHA_MODE_PREMULTIPLIED) == DdsConstants.DDS_ALPHA_MODE_PREMULTIPLIED;
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>(5);
        list.add("dxgiFormat=" + dxgiFormat);
        list.add("resourceDimension=" + resourceDimension);
        if (miscFlag != 0) {
            list.add("miscFlag=0x" + Integer.toHexString(miscFlag));
        }
        if (arraySize != 0) {
            list.add("arraySize=" + Integer.toUnsignedString(arraySize));
        }
        if (miscFlags2 != 0) {
            list.add("miscFlags2=0x" + Integer.toHexString(miscFlags2));
        }
        return "DdsHeaderDxt10[" + String.join(", ", list) + ']';
    }
}
