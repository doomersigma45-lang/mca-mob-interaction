package com.mcaeverywhere.net;

import com.mcaeverywhere.client.ClientHooks;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Servidor -> cliente: abre o menu (open = true) ou atualiza amizade/modo/fala do menu já aberto. */
public class StatePacket {
    public final int entityId;
    public final boolean open;
    public final int friendship;
    public final int mode;
    @Nullable
    public final Component line;

    public StatePacket(int entityId, boolean open, int friendship, int mode, @Nullable Component line) {
        this.entityId = entityId;
        this.open = open;
        this.friendship = friendship;
        this.mode = mode;
        this.line = line;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBoolean(open);
        buf.writeVarInt(friendship);
        buf.writeVarInt(mode);
        buf.writeBoolean(line != null);
        if (line != null) {
            buf.writeComponent(line);
        }
    }

    public static StatePacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        boolean open = buf.readBoolean();
        int friendship = buf.readVarInt();
        int mode = buf.readVarInt();
        Component line = buf.readBoolean() ? buf.readComponent() : null;
        return new StatePacket(entityId, open, friendship, mode, line);
    }

    public static void handle(StatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.handleState(msg)));
        ctx.get().setPacketHandled(true);
    }
}
