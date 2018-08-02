package us.myles.ViaVersion.protocols.base;

import us.myles.ViaVersion.packets.State;

public class BaseProtocol1_9 extends AbstractBaseProtocol1_7 {
    @Override
    protected void registerPackets() {
        super.registerPackets();
        registerOutgoing(State.PLAY, 0x18, 0x18, new BrandRemapper("MC|Brand"));
    }
}
