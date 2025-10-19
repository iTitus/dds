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
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Iterator;
import java.util.List;

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
            PixelFormat format;
            if (dds.isDxt10()) {
                format = dds.dxgiFormat();
            } else {
                // override format
                DxgiFormat dxgiFormat = Util.deriveDxgiFormat(dds.header());
                if (dxgiFormat != DxgiFormat.UNKNOWN) {
                    format = dxgiFormat;
                } else {
                    format = dds.d3dFormat();
                }
            }

            if (format.isYUVFormat()) {
                throw new UnsupportedOperationException("unsupported YUV format " + format);
            } else if (format.isPacked()) {
                throw new UnsupportedOperationException("unsupported packed format " + format);
            } else if (format.isBlockCompressed()) {
                BC.decode(h, w, raster, b, format);
            } else {
                // assume a simple format that can be copied without decoding it
                int bpp = format.getBitsPerPixel();
                switch (bpp) {
                    case 1, 2, 4, 8 -> {
                        int pixelsPerByte = 8 / bpp;
                        byte[] arr = new byte[DdsHelper.ceilDivUnsigned(w, pixelsPerByte)];
                        for (int y = 0; Integer.compareUnsigned(y, h) < 0; y++) {
                            b.get(arr);
                            raster.setDataElements(0, y, w, 1, arr);
                        }
                    }
                    case 16 -> {
                        short[] arr = new short[w];
                        ShortBuffer sb = b.asShortBuffer();
                        for (int y = 0; Integer.compareUnsigned(y, h) < 0; y++) {
                            sb.get(arr);
                            raster.setDataElements(0, y, w, 1, arr);
                        }
                    }
                    case 32 -> {
                        int[] arr = new int[w];
                        IntBuffer ib = b.asIntBuffer();
                        for (int y = 0; Integer.compareUnsigned(y, h) < 0; y++) {
                            ib.get(arr);
                            raster.setDataElements(0, y, w, 1, arr);
                        }
                    }
                    default -> throw new UnsupportedOperationException("unsupported bpp " + bpp + " for assumed simple format " + format);
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
