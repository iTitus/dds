package io.github.ititus.dds;

public sealed interface PixelFormat permits D3dFormat, DxgiFormat {

    /**
     * bpp
     */
    int getBitsPerPixel();

    /**
     * Only applies to block compressed formats.
     * Others may return 1 or throw an exception.
     */
    int getHorizontalPixelsPerBlock();

    /**
     * Only applies to block compressed formats.
     * Others may return 1 or throw an exception.
     */
    int getVerticalPixelsPerBlock();

    /**
     * Only applies to block compressed formats.
     * Others may return 1 or throw an exception.
     */
    default int getBitsPerBlock() {
        return getBitsPerPixel() * getHorizontalPixelsPerBlock() * getVerticalPixelsPerBlock();
    }

    /**
     * Is this block compressed format, like BC1-7.
     */
    boolean isBlockCompressed();

    /**
     * Is this a packed format,
     * in the sense of multiple pixel values being packed together as one macropixel
     * or even sharing components.
     */
    boolean isPacked();

    /**
     * Is this a YUV (video) resource format.
     */
    boolean isYUVFormat();

    /**
     * Is this a (planar) YUV video resource format.
     */
    boolean isSRGB();

}
