package us.myles.ViaVersion.exception;

import io.netty.handler.codec.EncoderException;

public class CancelException extends EncoderException { // Encoder exception as workaround for performance issue caused by (ab)using exceptions
    public static final CancelException CACHED = new CancelException("Cached - Enable /viaver debug to not use cached exception") {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    };

    public CancelException() {
    }

    public CancelException(String message) {
        super(message);
    }

    public CancelException(String message, Throwable cause) {
        super(message, cause);
    }

    public CancelException(Throwable cause) {
        super(cause);
    }
}
