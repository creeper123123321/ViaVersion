package us.myles.ViaVersion.sponge.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.exception.CancelException;
import us.myles.ViaVersion.handlers.ChannelHandlerContextWrapper;
import us.myles.ViaVersion.handlers.CommonTransformer;
import us.myles.ViaVersion.handlers.ViaHandler;
import us.myles.ViaVersion.util.PipelineUtil;

import java.lang.reflect.InvocationTargetException;

public class SpongeEncodeHandler extends MessageToByteEncoder implements ViaHandler {
    private final UserConnection info;
    private final MessageToByteEncoder minecraftEncoder;

    public SpongeEncodeHandler(UserConnection info, MessageToByteEncoder minecraftEncoder) {
        this.info = info;
        this.minecraftEncoder = minecraftEncoder;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, Object o, final ByteBuf bytebuf) throws Exception {
        // handle the packet type
        if (!(o instanceof ByteBuf)) {
            // call minecraft encoder
            try {
                PipelineUtil.callEncode(this.minecraftEncoder, new ChannelHandlerContextWrapper(ctx, this), o, bytebuf);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
            }
        }

        transform(bytebuf);
    }

    @Override
    public void transform(ByteBuf bytebuf) throws Exception {
        CommonTransformer.preClientbound(info);
        if (!CommonTransformer.willTransformPacket(info)) return;
        CommonTransformer.transformClientbound(bytebuf, info);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PipelineUtil.containsCause(cause, CancelException.class)) return;
        super.exceptionCaught(ctx, cause);
    }
}
