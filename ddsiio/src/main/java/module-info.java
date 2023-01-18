module io.github.ititus.ddsiio {
    requires transitive io.github.ititus.dds;
    requires transitive java.desktop;

    exports io.github.ititus.ddsiio;

    provides javax.imageio.spi.ImageReaderSpi with io.github.ititus.ddsiio.DdsImageReaderSpi;
}
