package com.mcaeverywhere.client;

import com.mcaeverywhere.net.StatePacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientHooks {
    private ClientHooks() {}

    public static void handleState(StatePacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (p.open) {
            mc.setScreen(new MobInteractScreen(p.entityId, p.friendship, p.mode));
        } else if (mc.screen instanceof MobInteractScreen screen && screen.entityId() == p.entityId) {
            screen.update(p.friendship, p.mode, p.line);
        }
    }
}
