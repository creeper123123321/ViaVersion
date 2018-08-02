package us.myles.ViaVersion.protocols.base;

import us.myles.ViaVersion.packets.State;

public class BaseProtocol1_13 extends AbstractBaseProtocol1_7 {
    @Override
    protected void registerPackets() {
        super.registerPackets();
        registerOutgoing(State.PLAY, 0x19, 0x19, new BrandRemapper("minecraft:brand"));
    }
}
