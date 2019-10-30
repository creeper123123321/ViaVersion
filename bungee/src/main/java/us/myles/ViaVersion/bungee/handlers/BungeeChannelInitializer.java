package us.myles.ViaVersion.bungee.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.Getter;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.protocol.ProtocolPipeline;
import us.myles.ViaVersion.handlers.CommonTransformer;

import java.lang.reflect.Method;

public class BungeeChannelInitializer extends ChannelInitializer<Channel> {
    @Getter
    private final ChannelInitializer<Channel> original;
    private Method method;

    public BungeeChannelInitializer(ChannelInitializer<Channel> oldInit) {
        this.original = oldInit;
        try {
            this.method = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
            this.method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void initChannel(Channel socketChannel) throws Exception {
        UserConnection info = new UserConnection(socketChannel);
        // init protocol
        new ProtocolPipeline(info);
        // Add originals
        this.method.invoke(this.original, socketChannel);

        if (socketChannel.pipeline().get("packet-encoder") == null) return; // Don't inject if no packet-encoder
        if (socketChannel.pipeline().get("packet-decoder") == null) return; // Don't inject if no packet-decoder
        // Add our transformers
        BungeeEncodeHandler encoder = new BungeeEncodeHandler(info);
        BungeeDecodeHandler decoder = new BungeeDecodeHandler(info);

        socketChannel.pipeline().addBefore("packet-encoder", CommonTransformer.HANDLER_ENCODER_NAME, encoder);
        socketChannel.pipeline().addBefore("packet-decoder", CommonTransformer.HANDLER_DECODER_NAME, decoder);

    }
}
