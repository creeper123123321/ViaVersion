package us.myles.ViaVersion.velocity.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.AllArgsConstructor;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.protocol.ProtocolPipeline;
import us.myles.ViaVersion.handlers.CommonTransformer;

import java.lang.reflect.Method;

@AllArgsConstructor
public class VelocityChannelInitializer extends ChannelInitializer {
    private ChannelInitializer original;
    private static Method initChannel;

    static {
        try {
            initChannel = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
            initChannel.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        initChannel.invoke(original, channel);

        UserConnection user = new UserConnection(channel);
        new ProtocolPipeline(user);

        // We need to add a separated handler because Velocity uses pipeline().get(MINECRAFT_DECODER)
        channel.pipeline().addBefore("minecraft-encoder", CommonTransformer.HANDLER_ENCODER_NAME, new VelocityEncodeHandler(user));
        channel.pipeline().addBefore("minecraft-decoder", CommonTransformer.HANDLER_DECODER_NAME, new VelocityDecodeHandler(user));
    }
}
