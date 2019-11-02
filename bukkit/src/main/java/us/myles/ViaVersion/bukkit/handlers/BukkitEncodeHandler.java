package us.myles.ViaVersion.bukkit.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.bukkit.util.NMSUtil;
import us.myles.ViaVersion.exception.EncoderCancelException;
import us.myles.ViaVersion.handlers.ChannelHandlerContextWrapper;
import us.myles.ViaVersion.handlers.CommonTransformer;
import us.myles.ViaVersion.handlers.ViaHandler;
import us.myles.ViaVersion.util.PipelineUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class BukkitEncodeHandler extends MessageToByteEncoder implements ViaHandler {
    private static Field versionField = null;

    static {
        try {
            versionField = NMSUtil.nms("PacketEncoder").getDeclaredField("version");
            versionField.setAccessible(true);
        } catch (Exception e) {
            // Not compat version
        }
    }

    private final UserConnection info;
    private final MessageToByteEncoder minecraftEncoder;

    public BukkitEncodeHandler(UserConnection info, MessageToByteEncoder minecraftEncoder) {
        this.info = info;
        this.minecraftEncoder = minecraftEncoder;
    }


    @Override
    protected void encode(final ChannelHandlerContext ctx, Object o, final ByteBuf bytebuf) throws Exception {
        if (versionField != null) {
            versionField.set(minecraftEncoder, versionField.get(this));
        }
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

    public void transform(ByteBuf bytebuf) throws Exception {
        CommonTransformer.preClientbound(info);
        if (!CommonTransformer.willTransformPacket(info)) return;
        CommonTransformer.transformClientbound(bytebuf, info);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PipelineUtil.containsCause(cause, EncoderCancelException.class)) return;
        super.exceptionCaught(ctx, cause);
    }
}
