package us.myles.ViaVersion.bungee.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.exception.CancelException;
import us.myles.ViaVersion.handlers.CommonTransformer;
import us.myles.ViaVersion.util.PipelineUtil;

import java.util.List;

@ChannelHandler.Sharable
public class BungeeDecodeHandler extends MessageToMessageDecoder<ByteBuf> {

    private final UserConnection info;

    public BungeeDecodeHandler(UserConnection info) {
        this.info = info;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> out) throws Exception {
        ByteBuf draft = ctx.alloc().buffer().writeBytes(bytebuf);
        try {
            CommonTransformer.transformServerbound(draft, info);
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
