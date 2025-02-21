package io.github.ititus.ddsfx;

public final class DdsFx {

    private static boolean initialized = false;

    private DdsFx() {
    }

    @SuppressWarnings({ "unused", "Java9ReflectionClassVisibility", "JavaReflectionInvocation" })
    public static synchronized void setup() {
        if (!initialized) {
            try {
                var ddsImageLoaderFactory = Class.forName("io.github.ititus.ddsfx.internal.DdsImageLoaderFactory");
                var ddsImageLoaderFactoryGetInstance = ddsImageLoaderFactory.getDeclaredMethod("getInstance");
                ddsImageLoaderFactoryGetInstance.setAccessible(true);
                var ddsImageLoaderFactoryInstance = ddsImageLoaderFactoryGetInstance.invoke(null);

                var imageStorage = Class.forName("com.sun.javafx.iio.ImageStorage");
                var imageStorageGetInstance = imageStorage.getDeclaredMethod("getInstance");
                imageStorageGetInstance.setAccessible(true);
                var imageStorageInstance = imageStorageGetInstance.invoke(null);

                var imageLoaderFactory = Class.forName("com.sun.javafx.iio.ImageLoaderFactory");
                var addImageLoaderFactory = imageStorage.getDeclaredMethod("addImageLoaderFactory", imageLoaderFactory);
                addImageLoaderFactory.setAccessible(true);
                addImageLoaderFactory.invoke(imageStorageInstance, ddsImageLoaderFactoryInstance);
            } catch (Exception e) {
                throw new RuntimeException("could not call ImageStorage#addImageLoaderFactory method, it is required to register the dds image loader", e);
            }
            initialized = true;
        }
    }
}
