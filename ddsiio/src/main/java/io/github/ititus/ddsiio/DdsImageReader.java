package io.github.ititus.ddsiio;

import io.github.ititus.dds.*;

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

public class DdsImageReader extends ImageReader {

    private ImageInputStream stream;
    private DdsFile dds;

    public DdsImageReader(DdsImageReaderSpi originator) {
        super(originator);
    }

    private static void loadColorsBc1(ByteBuffer b, Rgba[] colors, byte[] colorIndices, boolean oneBitAlpha) {
        short rawC0 = b.getShort();
        short rawC1 = b.getShort();
        b.get(colorIndices, 0, 4);

        var c0 = Rgba.fromR5G6B5(rawC0);
        var c1 = Rgba.fromR5G6B5(rawC1);
        colors[0] = c0;
        colors[1] = c1;
        if (!oneBitAlpha || Short.compareUnsigned(rawC0, rawC1) > 0) {
            colors[2] = c0.lerp(c1, 2, 1);
            colors[3] = c0.lerp(c1, 1, 2);
        } else {
            colors[2] = c0.lerp(c1, 1, 1);
            colors[3] = Rgba.TRANSPARENT;
        }
    }

    private static Rgba colorLookupBc1(Rgba[] colors, byte[] colorIndices, int y, int x) {
        int colorIndex = (Byte.toUnsignedInt(colorIndices[y]) >>> (2 * x)) & 0x3;
        return colors[colorIndex];
    }

    private static byte alphaLookupBc2(byte[] alphas, int y, int x) {
        int alpha = (alphas[(y << 1) | (x >>> 1)] >>> (4 * (x & 0x1))) & 0xf;
        return (byte) (alpha * 17);
    }

    private static void loadAlphaBc3(ByteBuffer b, byte[] alphas, int[] alphaIndices) {
        byte rawA0 = b.get();
        byte rawA1 = b.get();
        alphaIndices[0] = DdsHelper.read24(b);
        alphaIndices[1] = DdsHelper.read24(b);

        int a0 = Byte.toUnsignedInt(rawA0);
        int a1 = Byte.toUnsignedInt(rawA1);

        alphas[0] = rawA0;
        alphas[1] = rawA1;
        if (Byte.compareUnsigned(rawA0, rawA1) > 0) {
            for (int i = 1; i <= 6; i++) {
                alphas[i + 1] = (byte) (((7 - i) * a0 + i * a1) / 7);
            }
        } else {
            for (int i = 1; i <= 4; i++) {
                alphas[i + 1] = (byte) (((5 - i) * a0 + i * a1) / 5);
            }
            alphas[6] = 0;
            alphas[7] = (byte) 255;
        }
    }

    private static byte alphaLookupBc3(byte[] alphas, int[] alphaIndices, int y, int x) {
        int alphaIndex = (alphaIndices[y >>> 1] >>> (3 * (((y & 0x1) << 2) | x))) & 0x7;
        return alphas[alphaIndex];
    }

    private static void bc1(int h, int w, WritableRaster raster, ByteBuffer b) {
        var colors = new Rgba[4];
        var colorIndices = new byte[4];
        for (int y = 0; Integer.compareUnsigned(y, h) < 0; y += 4) {
            for (int x = 0; Integer.compareUnsigned(x, w) < 0; x += 4) {
                loadColorsBc1(b, colors, colorIndices, true);

                int yMax = Math.min(4, h - y);
                int xMax = Math.min(4, w - x);
                for (int y_ = 0; y_ < yMax; y_++) {
                    for (int x_ = 0; x_ < xMax; x_++) {
                        var color = colorLookupBc1(colors, colorIndices, y_, x_);
                        raster.setDataElements(x + x_, y + y_, new int[] { color.asA8R8G8B8() });
                    }
                }
            }
        }
    }

    private static void bc2(int h, int w, WritableRaster raster, ByteBuffer b) {
        var alphas = new byte[8];
        var colors = new Rgba[4];
        var colorIndices = new byte[4];
        for (int y = 0; Integer.compareUnsigned(y, h) < 0; y += 4) {
            for (int x = 0; Integer.compareUnsigned(x, w) < 0; x += 4) {
                b.get(alphas, 0, 8);
                loadColorsBc1(b, colors, colorIndices, false);

                int yMax = Math.min(4, h - y);
                int xMax = Math.min(4, w - x);
                for (int y_ = 0; y_ < yMax; y_++) {
                    for (int x_ = 0; x_ < xMax; x_++) {
                        var alpha = alphaLookupBc2(alphas, y_, x_);
                        var color = colorLookupBc1(colors, colorIndices, y_, x_).withAlpha(alpha);
                        raster.setDataElements(x + x_, y + y_, new int[] { color.asA8R8G8B8() });
                    }
                }
            }
        }
    }

    private static void bc3(int h, int w, WritableRaster raster, ByteBuffer b) {
        var alphas = new byte[8];
        var alphaIndices = new int[2];
        var colors = new Rgba[4];
        var colorIndices = new byte[4];
        for (int y = 0; Integer.compareUnsigned(y, h) < 0; y += 4) {
            for (int x = 0; Integer.compareUnsigned(x, w) < 0; x += 4) {
                loadAlphaBc3(b, alphas, alphaIndices);
                loadColorsBc1(b, colors, colorIndices, false);

                int yMax = Math.min(4, h - y);
                int xMax = Math.min(4, w - x);
                for (int y_ = 0; y_ < yMax; y_++) {
                    for (int x_ = 0; x_ < xMax; x_++) {
                        var alpha = alphaLookupBc3(alphas, alphaIndices, y_, x_);
                        var color = colorLookupBc1(colors, colorIndices, y_, x_).withAlpha(alpha);
                        raster.setDataElements(x + x_, y + y_, new int[] { color.asA8R8G8B8() });
                    }
                }
            }
        }
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
        return List.of(DdsIioHelper.imageType(dds)).iterator();
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

        PixelFormat format = dds.isDxt10() ? dds.dxgiFormat() : dds.d3dFormat();
        if (format.isBlockCompressed()) {
            if (format == D3dFormat.DXT1 || format == DxgiFormat.BC1_UNORM || format == DxgiFormat.BC1_UNORM_SRGB) {
                bc1(h, w, raster, b);
            } else if (format == D3dFormat.DXT2 || format == D3dFormat.DXT3 || format == DxgiFormat.BC2_UNORM || format == DxgiFormat.BC2_UNORM_SRGB) {
                bc2(h, w, raster, b);
            } else if (format == D3dFormat.DXT4 || format == D3dFormat.DXT5 || format == DxgiFormat.BC3_UNORM || format == DxgiFormat.BC3_UNORM_SRGB) {
                bc3(h, w, raster, b);
            } else {
                throw new IIOException("unsupported block compression " + format + " from " + dds);
            }
        } else if (format.isPacked()) {
            throw new IIOException("unsupported packed format " + format + " from " + dds);
        } else if (format.isPlanar()) {
            throw new IIOException("unsupported planar format " + format + " from " + dds);
        } else {
            int bpp = format.getBitsPerPixel();
            for (int y = 0; Integer.compareUnsigned(y, h) < 0; y++) {
                for (int x = 0; Integer.compareUnsigned(x, w) < 0; x++) {
                    // TODO: support other bpp
                    Object arr = switch (bpp) {
                        case 8 -> new byte[] { b.get() };
                        case 16 -> new short[] { b.getShort() };
                        case 24 -> new int[] { DdsHelper.read24(b) };
                        case 32 -> new int[] { b.getInt() };
                        default ->
                                throw new IIOException("unsupported bpp " + bpp + " for format: " + format + " from " + dds);
                    };
                    raster.setDataElements(x, y, arr);
                }
            }
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
