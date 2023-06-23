package io.github.ititus.dds;

public sealed interface PixelFormat permits D3dFormat, DxgiFormat {

    int getBitsPerPixel();

    int getHorizontalPixelsPerBlock();

    int getVerticalPixelsPerBlock();

    default int getBitsPerBlock() {
        return getBitsPerPixel() * getHorizontalPixelsPerBlock() * getVerticalPixelsPerBlock();
    }

    default boolean isBlockCompressed() {
        return getHorizontalPixelsPerBlock() > 1 || getVerticalPixelsPerBlock() > 1;
    }
}
