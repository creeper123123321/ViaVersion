package us.myles.ViaVersion.exception;

import io.netty.handler.codec.EncoderException;

public class EncoderCancelException extends EncoderException {

    public static final EncoderException CACHED = new EncoderCancelException("Cached - Enable /viaver debug to not use cached exceptions") {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    };

    public EncoderCancelException() {
    }

    public EncoderCancelException(String message) {
        super(message);
    }
}
