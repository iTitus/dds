package io.github.ititus.ddsiio;

import io.github.ititus.dds.*;

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

        Rgba c0 = Rgba.fromR5G6B5(rawC0);
        Rgba c1 = Rgba.fromR5G6B5(rawC1);
        colors[0] = c0;
        colors[1] = c1;
        if (!oneBitAlpha || Short.compareUnsigned(rawC0, rawC1) > 0) {
            colors[2] = c0.lerp(c1, 1.0f / 3.0f);
            colors[3] = c0.lerp(c1, 2.0f / 3.0f);
        } else {
            colors[2] = c0.lerp(c1, 1.0f / 2.0f);
            colors[3] = Rgba.TRANSPARENT;
        }
    }

    private static Rgba colorLookupBc1(Rgba[] colors, byte[] colorIndices, int y, int x) {
        int colorIndex = (Byte.toUnsignedInt(colorIndices[y]) >>> (2 * x)) & 0x3;
        return colors[colorIndex];
    }

    private static void loadAlphaBc3(ByteBuffer b, float[] alphas, int[] alphaIndices) {
        byte rawA0 = b.get();
        byte rawA1 = b.get();
        alphaIndices[0] = DdsHelper.read24(b);
        alphaIndices[1] = DdsHelper.read24(b);

        float a0 = Byte.toUnsignedInt(rawA0) / 255.0f;
        float a1 = Byte.toUnsignedInt(rawA1) / 255.0f;

        alphas[0] = a0;
        alphas[1] = a1;
        if (Byte.compareUnsigned(rawA0, rawA1) > 0) {
            for (int i = 1; i <= 6; i++) {
                alphas[i + 1] = ((7 - i) / 7.0f) * a0 + (i / 7.0f) * a1;
            }
        } else {
            for (int i = 1; i <= 4; i++) {
                alphas[i + 1] = ((5 - i) / 5.0f) * a0 + (i / 5.0f) * a1;
            }
            alphas[6] = 0.0f;
            alphas[7] = 1.0f;
        }
    }

    private static float alphaLookupBc3(float[] alphas, int[] alphaIndices, int y, int x) {
        int alphaIndex = (alphaIndices[y >>> 1] >>> (3 * (((y & 0x1) << 2) | x))) & 0x7;
        return alphas[alphaIndex];
    }

    private static void bc1(int h, int w, WritableRaster raster, ByteBuffer b) {
        Rgba[] colors = new Rgba[4];
        byte[] colorIndices = new byte[4];
        for (int y = 0; y < h; y += 4) {
            for (int x = 0; x < w; x += 4) {
                loadColorsBc1(b, colors, colorIndices, true);

                int yMax = Math.min(4, h - y);
                int xMax = Math.min(4, w - x);
                for (int y_ = 0; y_ < yMax; y_++) {
                    for (int x_ = 0; x_ < xMax; x_++) {
                        Rgba color = colorLookupBc1(colors, colorIndices, y_, x_);
                        raster.setDataElements(x + x_, y + y_, new int[] { color.asA8R8G8B8() });
                    }
                }
            }
        }
    }

    private static void bc2(int h, int w, WritableRaster raster, ByteBuffer b) {
        byte[] alphas = new byte[8];
        Rgba[] colors = new Rgba[4];
        byte[] colorIndices = new byte[4];
        for (int y = 0; y < h; y += 4) {
            for (int x = 0; x < w; x += 4) {
                b.get(alphas, 0, 8);
                loadColorsBc1(b, colors, colorIndices, false);

                int yMax = Math.min(4, h - y);
                int xMax = Math.min(4, w - x);
                for (int y_ = 0; y_ < yMax; y_++) {
                    for (int x_ = 0; x_ < xMax; x_++) {
                        int a = (alphas[(y_ << 1) | (x_ >>> 1)] >>> (4 * (x_ & 0x1))) & 0xf;
                        Rgba color = colorLookupBc1(colors, colorIndices, y_, x_)
                                .withAlpha(a / 15.0f);
                        raster.setDataElements(x + x_, y + y_, new int[] { color.asA8R8G8B8() });
                    }
                }
            }
        }
    }

    private static void bc3(int h, int w, WritableRaster raster, ByteBuffer b) {
        float[] alphas = new float[8];
        int[] alphaIndices = new int[2];
        Rgba[] colors = new Rgba[4];
        byte[] colorIndices = new byte[4];
        for (int y = 0; y < h; y += 4) {
            for (int x = 0; x < w; x += 4) {
                loadAlphaBc3(b, alphas, alphaIndices);
                loadColorsBc1(b, colors, colorIndices, false);

                int yMax = Math.min(4, h - y);
                int xMax = Math.min(4, w - x);
                for (int y_ = 0; y_ < yMax; y_++) {
                    for (int x_ = 0; x_ < xMax; x_++) {
                        Rgba color = colorLookupBc1(colors, colorIndices, y_, x_).withAlpha(alphaLookupBc3(alphas,
                                alphaIndices, y_, x_));
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
        return dds.width();
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        loadAndCheckIndex(imageIndex);
        return dds.height();
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

        DdsResource resource = dds.resources().get(imageIndex);
        DdsSurface surface = resource.getSurfaces().get(0); // TODO: make this addressable
        ByteBuffer b = surface.getBuffer();
        b.order(ByteOrder.LITTLE_ENDIAN);

        int h = getHeight(imageIndex);
        int w = getWidth(imageIndex);
        BufferedImage img = getDestination(param, getImageTypes(imageIndex), w, h);
        WritableRaster raster = img.getRaster();

        if (dds.isDxt10()) {
            // TODO: support dxgi format as well
            throw new UnsupportedOperationException("dxt10 header not supported");
        } else {
            D3dFormat d3dFormat = dds.d3dFormat();
            if (d3dFormat.isBlockCompressed()) {
                switch (d3dFormat) {
                    case DXT1 -> bc1(h, w, raster, b);
                    case DXT2, DXT3 -> bc2(h, w, raster, b);
                    case DXT4, DXT5 -> bc3(h, w, raster, b);
                    default -> throw new RuntimeException("unsupported block compression: " + d3dFormat);
                }
            } else {
                int bpp = d3dFormat.getBitsPerPixel();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        Object arr = switch (bpp) {
                            case 8 -> new byte[] { b.get() };
                            case 16 -> new short[] { b.getShort() };
                            case 24 -> new int[] { DdsHelper.read24(b) };
                            case 32 -> new int[] { b.getInt() };
                            default -> throw new RuntimeException("unsupported bpp: " + bpp); // TODO: support other bpp
                        };
                        raster.setDataElements(x, y, arr);
                    }
                }
            }
        }

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

        dds = DdsFile.load(stream);
    }
}
