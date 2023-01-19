package io.github.ititus.ddsfx;

import java.lang.reflect.Method;

public final class DdsFx {

    private static boolean initialized = false;

    private DdsFx() {
    }

    public static synchronized void setup() {
        if (!initialized) {
            try {
                Class<?> ddsImageLoaderFactory = Class.forName("io.github.ititus.ddsfx.internal.DdsImageLoaderFactory");
                var ddsImageLoaderFactoryGetInstance = ddsImageLoaderFactory.getDeclaredMethod("getInstance");
                ddsImageLoaderFactoryGetInstance.setAccessible(true);
                var ddsImageLoaderFactoryInstance = ddsImageLoaderFactoryGetInstance.invoke(null);

                Class<?> imageStorage = Class.forName("com.sun.javafx.iio.ImageStorage");
                Class<?> imageLoaderFactory = Class.forName("com.sun.javafx.iio.ImageLoaderFactory");
                Method addImageLoaderFactory = imageStorage.getDeclaredMethod("addImageLoaderFactory", imageLoaderFactory);

                var imageStorageGetInstance = imageStorage.getDeclaredMethod("getInstance");
                imageStorageGetInstance.setAccessible(true);
                var imageStorageInstance = imageStorageGetInstance.invoke(null);

                addImageLoaderFactory.invoke(imageStorageInstance, ddsImageLoaderFactoryInstance);
            } catch (Exception e) {
                throw new RuntimeException("could not call ImageStorage#addImageLoaderFactory method, it is required to register the dds image loader", e);
            }
            initialized = true;
        }
    }
}
