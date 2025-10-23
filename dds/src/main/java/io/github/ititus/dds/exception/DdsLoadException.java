package io.github.ititus.dds.exception;

public class DdsLoadException extends DdsException {

    public DdsLoadException(String message) {
        super(message);
    }

    public DdsLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
