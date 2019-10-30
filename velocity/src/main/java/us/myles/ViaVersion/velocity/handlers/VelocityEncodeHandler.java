package us.myles.ViaVersion.velocity.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.exception.CancelException;
import us.myles.ViaVersion.handlers.CommonTransformer;
import us.myles.ViaVersion.util.PipelineUtil;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class VelocityEncodeHandler extends MessageToByteEncoder<ByteBuf> {
    @NonNull
    private final UserConnection info;
    private boolean handledCompression = false;

    @Override
    protected void encode(final ChannelHandlerContext ctx, ByteBuf bytebuf, ByteBuf out) throws Exception {
        if (!bytebuf.isReadable()) {
            throw Via.getManager().isDebug() ? new CancelException() : CancelException.CACHED;
        }
        out.writeBytes(bytebuf);
        boolean needsCompress = false;
        if (!handledCompression
                && ctx.pipeline().names().indexOf("compression-encoder") > ctx.pipeline().names().indexOf(CommonTransformer.HANDLER_ENCODER_NAME)) {
            // Need to decompress this packet due to bad order
            ByteBuf decompressed = (ByteBuf) PipelineUtil.callDecode((MessageToMessageDecoder) ctx.pipeline().get("compression-decoder"), ctx, out).get(0);
            try {
                out.clear().writeBytes(decompressed);
            } finally {
                decompressed.release();
            }
            ChannelHandler encoder = ctx.pipeline().get(CommonTransformer.HANDLER_ENCODER_NAME);
            ChannelHandler decoder = ctx.pipeline().get(CommonTransformer.HANDLER_DECODER_NAME);
            ctx.pipeline().remove(encoder);
            ctx.pipeline().remove(decoder);
            ctx.pipeline().addAfter("compression-encoder", CommonTransformer.HANDLER_ENCODER_NAME, encoder);
            ctx.pipeline().addAfter("compression-decoder", CommonTransformer.HANDLER_DECODER_NAME, decoder);
            needsCompress = true;
            handledCompression = true;
        }
        CommonTransformer.transformClientbound(out, info);

        if (needsCompress) {
            ByteBuf compressed = ctx.alloc().buffer();
            try {
                PipelineUtil.callEncode((MessageToByteEncoder) ctx.pipeline().get("compression-encoder"), ctx, out, compressed);
                out.clear().writeBytes(compressed);
            } finally {
                compressed.release();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PipelineUtil.containsCause(cause, CancelException.class)) return;
        super.exceptionCaught(ctx, cause);
    }
}
