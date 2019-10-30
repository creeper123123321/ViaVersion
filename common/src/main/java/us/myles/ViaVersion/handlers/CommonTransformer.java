package us.myles.ViaVersion.handlers;

import io.netty.buffer.ByteBuf;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.Direction;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;

public class CommonTransformer {
    public static final String HANDLER_DECODER_NAME = "via-decoder";
    public static final String HANDLER_ENCODER_NAME = "via-encoder";

    public static void transformClientbound(ByteBuf draft, UserConnection user) throws Exception {
        if (!draft.isReadable()) return;
        // Increment sent
        user.incrementSent();
        transform(draft, user, Direction.OUTGOING);
    }

    public static void transformServerbound(ByteBuf draft, UserConnection user) throws Exception {
        if (!draft.isReadable()) return;
        // Ignore if pending disconnect
        if (user.isPendingDisconnect()) return;
        // Increment received + Check PPS
        if (user.incrementReceived() && user.handlePPS()) return;
        transform(draft, user, Direction.INCOMING);
    }

    private static void transform(ByteBuf draft, UserConnection user, Direction direction) throws Exception {
        if (!user.isActive()) return;

        int id = Type.VAR_INT.read(draft);
        if (id == PacketWrapper.PASSTHROUGH_ID) return;
        PacketWrapper wrapper = new PacketWrapper(id, draft, user);
        ProtocolInfo protInfo = user.get(ProtocolInfo.class);
        protInfo.getPipeline().transform(direction, protInfo.getState(), wrapper);
        ByteBuf transformed = draft.alloc().buffer();
        try {
            wrapper.writeToBuffer(transformed);
            draft.clear().writeBytes(transformed);
        } finally {
            transformed.release();
        }
    }
}