package us.myles.ViaVersion.exception;

import io.netty.handler.codec.DecoderException;

public class DecoderCancelException extends DecoderException {

    public static final DecoderCancelException CACHED = new DecoderCancelException("Cached - Enable /viaver debug to not use cached exceptions") {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    };

    public DecoderCancelException() {
    }

    public DecoderCancelException(String message) {
        super(message);
    }
}
