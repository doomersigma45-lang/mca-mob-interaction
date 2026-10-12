package com.mcaeverywhere;

import com.mcaeverywhere.net.Net;
import com.mcaeverywhere.net.StatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = MCAEverywhere.MODID)
public final class Events {
    private Events() {}

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new GiftValues());
    }

    // ---- Abrir o menu -------------------------------------------------------------------------
    // O cliente manda primeiro "interactAt" e depois "interact". Abrimos o menu no primeiro e
    // apenas cancelamos o segundo (para a interação normal do mob não acontecer junto).
    // Só atuamos no servidor: se cancelássemos no cliente o pacote nem seria enviado.

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleInteract(event, event.getTarget(), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        handleInteract(event, event.getTarget(), false);
    }

    private static void handleInteract(PlayerInteractEvent event, Entity target, boolean open) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(target instanceof Mob mob)) return;
        if (!shouldOpen(player, mob, event.getItemStack())) return;

        if (open) {
            Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new StatePacket(mob.getId(), true, FriendData.friendship(mob, player.getUUID()), FriendData.mode(mob), null));
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean shouldOpen(Player player, Mob mob, ItemStack held) {
        if (player.isSpectator() || !isEligibleMob(mob)) return false;
        switch (Config.TRIGGER.get()) {
            case SNEAK_RIGHT_CLICK:
                return player.isShiftKeyDown();
            case EMPTY_HAND_RIGHT_CLICK:
                return held.isEmpty();
            default:
                return true;
        }
    }

    /** Aldeões/mobs do próprio MCA já têm o menu deles, então ficam de fora. */
    public static boolean isEligibleMob(Mob mob) {
        if (!mob.isAlive()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (id == null) return false;
        if (id.getNamespace().equals("mca")) return false;
        return !Config.BLACKLIST.get().contains(id.toString());
    }

    // ---- Comportamento ------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide() || mob.tickCount % 10 != 0) return;
        Follow.tick(mob);
    }

    /** Amigos não atacam o jogador (nem mobs hostis). */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!Config.PEACEFUL_FRIENDS.get()) return;
        if (!(event.getNewTarget() instanceof ServerPlayer player)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!FriendData.has(mob)) return;

        boolean friend = FriendData.friendship(mob, player.getUUID()) >= Config.PEACEFUL_FRIENDSHIP.get();
        boolean owned = FriendData.mode(mob) != FriendData.FREE && player.getUUID().equals(FriendData.owner(mob));
        if (friend || owned) {
            event.setNewTarget(null);
        }
    }

    /** Bater num mob derruba a amizade e solta ele se ele estava seguindo/parado por você. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!FriendData.has(mob)) return;

        int f = FriendData.friendship(mob, player.getUUID());
        if (f > 0) {
            FriendData.setFriendship(mob, player.getUUID(), Math.max(0, f - 25));
        }
        if (FriendData.mode(mob) != FriendData.FREE && player.getUUID().equals(FriendData.owner(mob))) {
            Actions.release(mob);
        }
    }
}
