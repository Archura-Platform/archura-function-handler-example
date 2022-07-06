package io.archura.platform.exception;

import java.io.IOException;

public class FunctionException extends RuntimeException {
    public FunctionException(Exception e) {
        super(e);
    }
}
