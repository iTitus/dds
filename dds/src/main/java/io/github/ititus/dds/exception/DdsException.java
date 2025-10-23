package io.github.ititus.dds.exception;

import java.io.IOException;

public class DdsException extends IOException {

    public DdsException(String message) {
        super(message);
    }

    public DdsException(String message, Throwable cause) {
        super(message, cause);
    }
}
