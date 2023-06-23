package io.github.ititus.dds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class DdsResource {

    private final List<DdsSurface> surfaces;

    private DdsResource(List<DdsSurface> surfaces) {
        this.surfaces = surfaces;
    }

    public static DdsResource load(DataReader r, DdsHeader header, DdsHeaderDxt10 header10) throws IOException {
        List<DdsSurface> surfaces = new ArrayList<>();
        if (header10 != null) {
            // TODO: support dxgi format as well
            throw new UnsupportedOperationException("dxt10 header not supported");
        } else {
            D3dFormat d3dFormat = header.d3dFormat();

            int mipMapCount = header.hasMipmaps() ? DdsHelper.maxUnsigned(1, header.dwMipMapCount()) : 1;
            int depth = header.isVolumeTexture() ? DdsHelper.maxUnsigned(1, header.dwDepth()) : 1;
            int faces = header.isCubemap() ? DdsHelper.maxUnsigned(1, header.calculateCubemapFaces()) : 1;

            for (int face = 0; Integer.compareUnsigned(face, faces) < 0; face++) {
                int height = header.dwHeight();
                int width = header.dwWidth();
                int currentDepth = depth;
                for (int mipmap = 0; Integer.compareUnsigned(mipmap, mipMapCount) < 0; mipmap++) {
                    int size = DdsHelper.calculateSurfaceSize(height, width, d3dFormat) * currentDepth;
                    surfaces.add(DdsSurface.load(r, size));

                    if (height == 1 && width == 1 && currentDepth == 1) {
                        break;
                    }

                    height = DdsHelper.ceilDivUnsigned(height, 2);
                    width = DdsHelper.ceilDivUnsigned(width, 2);
                    currentDepth = DdsHelper.ceilDivUnsigned(currentDepth, 2);
                }
            }
        }

        return new DdsResource(List.copyOf(surfaces));
    }

    public List<DdsSurface> getSurfaces() {
        return surfaces;
    }
}
