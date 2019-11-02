package us.myles.ViaVersion.sponge.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.exception.DecoderCancelException;
import us.myles.ViaVersion.handlers.CommonTransformer;
import us.myles.ViaVersion.util.PipelineUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class SpongeDecodeHandler extends ByteToMessageDecoder {

    private final ByteToMessageDecoder minecraftDecoder;
    private final UserConnection info;

    public SpongeDecodeHandler(UserConnection info, ByteToMessageDecoder minecraftDecoder) {
        this.info = info;
        this.minecraftDecoder = minecraftDecoder;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> list) throws Exception {
        if (CommonTransformer.preServerboundCheck(info)) return;
        ByteBuf draft = ctx.alloc().buffer().writeBytes(bytebuf);
        try {
            if (CommonTransformer.willTransformPacket(info)) {
                CommonTransformer.transformServerbound(draft, info);
            }

            // call minecraft decoder
            try {
                list.addAll(PipelineUtil.callDecode(this.minecraftDecoder, ctx, draft));
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
            }
        } finally {
            draft.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PipelineUtil.containsCause(cause, DecoderCancelException.class)) return;
        super.exceptionCaught(ctx, cause);
    }
}
