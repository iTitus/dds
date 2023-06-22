package io.github.ititus.ddsfx.internal;

import com.sun.javafx.iio.ImageFormatDescription;
import com.sun.javafx.iio.common.ImageDescriptor;

public class DdsDescriptor extends ImageDescriptor {

    private static final String FORMAT_NAME = "DDS";
    private static final String[] EXTENSIONS = { "dds" };
    private static final ImageFormatDescription.Signature[] SIGNATURES = {
            new ImageFormatDescription.Signature(new byte[] { 'D', 'D', 'S', ' ' })
    };
    private static final String[] MIME_SUBTYPES = { "dds", "vnd-ms.dds" };

    private static ImageDescriptor instance = null;

    private DdsDescriptor() {
        super(FORMAT_NAME, EXTENSIONS, SIGNATURES, MIME_SUBTYPES);
    }

    public static synchronized ImageDescriptor getInstance() {
        if (instance == null) {
            instance = new DdsDescriptor();
        }

        return instance;
    }
}
