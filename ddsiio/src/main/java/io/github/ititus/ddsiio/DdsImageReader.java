package io.github.ititus.ddsiio;

import io.github.ititus.dds.*;
import io.github.ititus.ddsiio.internal.BC;
import io.github.ititus.ddsiio.internal.Util;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.List;

import static io.github.ititus.dds.DdsConstants.*;

public class DdsImageReader extends ImageReader {

    private ImageInputStream stream;
    private DdsFile dds;

    public DdsImageReader(DdsImageReaderSpi originator) {
        super(originator);
    }


    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        if (seekForwardOnly && allowSearch) {
            throw new IllegalStateException("seekForwardOnly and allowSearch cannot both be true!");
        }

        load();
        return dds.resourceCount();
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        loadAndCheckIndex(imageIndex);
        return dds.resources().get(imageIndex).getWidth();
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        loadAndCheckIndex(imageIndex);
        return dds.resources().get(imageIndex).getHeight();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        loadAndCheckIndex(imageIndex);
        return List.of(Util.imageType(dds)).iterator();
    }

    @Override
    public IIOMetadata getStreamMetadata() {
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        loadAndCheckIndex(imageIndex);
        return null;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        loadAndCheckIndex(imageIndex);
        clearAbortRequest();
        processImageStarted(imageIndex);
        if (param == null) {
            param = getDefaultReadParam();
        }

        DdsResource resource = dds.resources().get(imageIndex);
        ByteBuffer b = resource.getBuffer();
        b.order(ByteOrder.LITTLE_ENDIAN);

        int h = getHeight(imageIndex);
        int w = getWidth(imageIndex);
        Iterator<ImageTypeSpecifier> imageTypes;
        try {
            imageTypes = getImageTypes(imageIndex);
        } catch (UnsupportedOperationException e) {
            throw new IIOException("error while getting image type from " + dds, e);
        }
        BufferedImage img = getDestination(param, imageTypes, w, h);
        WritableRaster raster = img.getRaster();

        try {
            PixelFormat format = dds.isDxt10() ? dds.dxgiFormat() : dds.d3dFormat();
            if (format.isPacked()) {
                throw new UnsupportedOperationException("unsupported packed format " + format);
            } else if (format.isPlanar()) {
                throw new UnsupportedOperationException("unsupported planar format " + format);
            } else if (format.isBlockCompressed()) {
                BC.decode(h, w, raster, b, format);
            } else if (BC.isBlockCompressed(dds.header().ddspf())) {
                DxgiFormat dxgiFormat;
                int fcc = dds.header().ddspf().dwFourCC();
                if (fcc == D3DFMT_DXT1) {
                    dxgiFormat = DxgiFormat.BC1_UNORM;
                } else if (fcc == D3DFMT_DXT2) {
                    dxgiFormat = DxgiFormat.BC2_UNORM;
                } else if (fcc == D3DFMT_DXT3) {
                    dxgiFormat = DxgiFormat.BC2_UNORM;
                } else if (fcc == D3DFMT_DXT4) {
                    dxgiFormat = DxgiFormat.BC3_UNORM;
                } else if (fcc == D3DFMT_DXT5) {
                    dxgiFormat = DxgiFormat.BC3_UNORM;
                } else if (fcc == DXGI_FORMAT_BC4_UNORM || fcc == DXGI_FORMAT_BC4_UNORM_ALT) {
                    dxgiFormat = DxgiFormat.BC4_UNORM;
                } else if (fcc == DXGI_FORMAT_BC4_SNORM) {
                    dxgiFormat = DxgiFormat.BC4_SNORM;
                } else if (fcc == DXGI_FORMAT_BC5_UNORM) {
                    dxgiFormat = DxgiFormat.BC5_UNORM;
                } else if (fcc == DXGI_FORMAT_BC5_SNORM) {
                    dxgiFormat = DxgiFormat.BC5_SNORM;
                } else {
                    throw new AssertionError();
                }
                BC.decode(h, w, raster, b, dxgiFormat);
            } else {
                // assume RGB(A) packed format
                int bpp = format.getBitsPerPixel();
                for (int y = 0; Integer.compareUnsigned(y, h) < 0; y++) {
                    for (int x = 0; Integer.compareUnsigned(x, w) < 0; x++) {
                        Object arr = switch (bpp) {
                            case 8 -> new byte[] { b.get() };
                            case 16 -> new short[] { b.getShort() };
                            case 24 -> new int[] { DdsHelper.read24(b) };
                            case 32 -> new int[] { b.getInt() };
                            default -> throw new UnsupportedOperationException("unsupported bpp " + bpp + " for assumed packed format " + format);
                        };
                        raster.setDataElements(x, y, arr);
                    }
                }
            }
        } catch (Exception e) {
            throw new IIOException("error while processing image " + dds, e);
        }

        processImageComplete();
        return img;
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);
        this.stream = (ImageInputStream) input;
        _reset();
    }

    @Override
    public void reset() {
        super.reset();
        _reset();
    }

    private void checkSource() {
        if (stream == null) {
            throw new IllegalStateException("No input source set!");
        }
    }

    private void loadAndCheckIndex(int imageIndex) throws IOException {
        load();
        if (imageIndex < 0 || imageIndex >= dds.resourceCount()) {
            throw new IndexOutOfBoundsException("imageIndex " + imageIndex + " out of bounds: only " + dds.resourceCount() + "image(s) available!");
        }
    }

    private void _reset() {
        dds = null;
    }

    private void load() throws IOException {
        checkSource();
        if (dds != null) {
            return;
        }

        try {
            dds = DdsFile.load(stream);
        } catch (Exception e) {
            throw new IIOException("error while loading dds file", e);
        }
    }
}
