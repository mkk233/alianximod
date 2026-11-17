package com.bslx.XueXiMod.network;

import net.minecraftforge.network.simple.SimpleChannel;

public interface NetworkPacket {
    void register(SimpleChannel channel, int packetId);
}
