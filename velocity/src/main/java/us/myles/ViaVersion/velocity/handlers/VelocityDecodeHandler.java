package us.myles.ViaVersion.velocity.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.AllArgsConstructor;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.exception.DecoderCancelException;
import us.myles.ViaVersion.handlers.CommonTransformer;
import us.myles.ViaVersion.util.PipelineUtil;

import java.util.List;

@ChannelHandler.Sharable
@AllArgsConstructor
public class VelocityDecodeHandler extends MessageToMessageDecoder<ByteBuf> {
    private final UserConnection info;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> out) throws Exception {
        if (CommonTransformer.preServerboundCheck(info)) return;
        if (!CommonTransformer.willTransformPacket(info)) {
            out.add(bytebuf.retain().readSlice(bytebuf.readableBytes()));
            return;
        }
        info.getVelocityLock().readLock().lock();
        ByteBuf draft = ctx.alloc().buffer().writeBytes(bytebuf);
        try {
            CommonTransformer.transformServerbound(draft, info);
            out.add(draft.retain());
        } finally {
            info.getVelocityLock().readLock().unlock();
            draft.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PipelineUtil.containsCause(cause, DecoderCancelException.class)) return;
        super.exceptionCaught(ctx, cause);
    }
}
