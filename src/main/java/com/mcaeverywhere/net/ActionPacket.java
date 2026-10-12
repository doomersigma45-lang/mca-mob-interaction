package com.mcaeverywhere.net;

import com.mcaeverywhere.Actions;
import com.mcaeverywhere.Events;
import com.mcaeverywhere.FriendData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/** Cliente -> servidor: o jogador clicou num botão do menu. */
public class ActionPacket {
    private final int entityId;
    private final int action;

    public ActionPacket(int entityId, int action) {
        this.entityId = entityId;
        this.action = action;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(action);
    }

    public static ActionPacket decode(FriendlyByteBuf buf) {
        return new ActionPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (sender == null) return;
            Entity entity = sender.level().getEntity(msg.entityId);
            if (!(entity instanceof Mob mob)) return;
            if (!Events.isEligibleMob(mob) || mob.distanceToSqr(sender) > 100.0D) return;

            Component line = Actions.perform(sender, mob, msg.action);
            Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                new StatePacket(mob.getId(), false, FriendData.friendship(mob, sender.getUUID()), FriendData.mode(mob), line));
        });
        ctx.get().setPacketHandled(true);
    }
}
