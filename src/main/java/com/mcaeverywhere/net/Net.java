package com.mcaeverywhere.net;

import com.mcaeverywhere.MCAEverywhere;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(MCAEverywhere.MODID, "main"),
        () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private Net() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, StatePacket.class, StatePacket::encode, StatePacket::decode, StatePacket::handle);
        CHANNEL.registerMessage(id++, ActionPacket.class, ActionPacket::encode, ActionPacket::decode, ActionPacket::handle);
    }
}
