package us.myles.ViaVersion.protocols.base;

import us.myles.ViaVersion.packets.State;

public class BaseProtocol1_8 extends AbstractBaseProtocol1_7 {
    @Override
    protected void registerPackets() {
        super.registerPackets();
        registerOutgoing(State.PLAY, 0x3F, 0x3F, new BrandRemapper("MC|Brand"));
    }
}
