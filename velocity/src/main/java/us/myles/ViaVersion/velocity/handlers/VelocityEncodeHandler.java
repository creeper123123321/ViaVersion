package us.myles.ViaVersion.velocity.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.exception.EncoderCancelException;
import us.myles.ViaVersion.handlers.CommonTransformer;
import us.myles.ViaVersion.util.PipelineUtil;

import java.util.List;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class VelocityEncodeHandler extends MessageToMessageEncoder<ByteBuf> {
    @NonNull
    private final UserConnection info;
    private boolean handledCompression = false;

    @Override
    protected void encode(final ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> out) throws Exception {
        CommonTransformer.preClientbound(info);
        if (!CommonTransformer.willTransformPacket(info)) {
            out.add(bytebuf.retain().readSlice(bytebuf.readableBytes()));
            return;
        }
        ByteBuf draft = ctx.alloc().buffer().writeBytes(bytebuf);
        try {
            boolean needsCompress = false;
            if (!handledCompression
                    && ctx.pipeline().names().indexOf("compression-encoder") > ctx.pipeline().names().indexOf(CommonTransformer.HANDLER_ENCODER_NAME)) {
                // Need to decompress this packet due to bad order
                ByteBuf decompressed = (ByteBuf) PipelineUtil.callDecode((MessageToMessageDecoder) ctx.pipeline().get("compression-decoder"), ctx, draft).get(0);
                try {
                    draft.clear().writeBytes(decompressed);
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
            CommonTransformer.transformClientbound(draft, info);

            if (needsCompress) {
                ByteBuf compressed = ctx.alloc().buffer();
                try {
                    PipelineUtil.callEncode((MessageToByteEncoder) ctx.pipeline().get("compression-encoder"), ctx, draft, compressed);
                    draft.clear().writeBytes(compressed);
                } finally {
                    compressed.release();
                }
            }
            out.add(draft.retain());
        } finally {
            draft.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PipelineUtil.containsCause(cause, EncoderCancelException.class)) return;
        super.exceptionCaught(ctx, cause);
    }
}
