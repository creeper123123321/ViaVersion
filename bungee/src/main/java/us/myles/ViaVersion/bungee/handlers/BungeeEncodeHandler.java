package us.myles.ViaVersion.bungee.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.bungee.util.BungeePipelineUtil;
import us.myles.ViaVersion.exception.CancelException;
import us.myles.ViaVersion.handlers.CommonTransformer;
import us.myles.ViaVersion.util.PipelineUtil;

import java.util.List;

@ChannelHandler.Sharable
public class BungeeEncodeHandler extends MessageToMessageEncoder<ByteBuf> {
    private final UserConnection info;
    private boolean handledCompression = false;

    public BungeeEncodeHandler(UserConnection info) {
        this.info = info;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> out) throws Exception {
        CommonTransformer.preClientbound(info);
        if (!CommonTransformer.willTransformPacket(info)) {
            out.add(bytebuf.retain());
            return;
        }
        ByteBuf draft = ctx.alloc().buffer().writeBytes(bytebuf);
        try {
            boolean needsCompress = false;
            if (!handledCompression) {
                if (ctx.pipeline().names().indexOf("compress") > ctx.pipeline().names().indexOf("via-encoder")) {
                    // Need to decompress this packet due to bad order
                    ByteBuf decompressed = BungeePipelineUtil.decompress(ctx, draft);
                    try {
                        draft.clear().writeBytes(decompressed);
                    } finally {
                        decompressed.release();
                    }
                    ChannelHandler dec = ctx.pipeline().get(CommonTransformer.HANDLER_DECODER_NAME);
                    ChannelHandler enc = ctx.pipeline().get(CommonTransformer.HANDLER_ENCODER_NAME);
                    ctx.pipeline().remove(dec);
                    ctx.pipeline().remove(enc);
                    ctx.pipeline().addAfter("decompress", CommonTransformer.HANDLER_DECODER_NAME, dec);
                    ctx.pipeline().addAfter("compress", CommonTransformer.HANDLER_ENCODER_NAME, enc);
                    needsCompress = true;
                    handledCompression = true;
                }
            }
            CommonTransformer.transformClientbound(draft, info);

            if (needsCompress) {
                ByteBuf compressed = BungeePipelineUtil.compress(ctx, draft);
                try {
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
        if (PipelineUtil.containsCause(cause, CancelException.class)) return;
        super.exceptionCaught(ctx, cause);
    }
}
