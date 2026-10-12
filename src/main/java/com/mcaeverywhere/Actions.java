package com.mcaeverywhere;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Lógica (servidor) de cada botão do menu de interação. */
public final class Actions {
    public static final int TALK = 0;
    public static final int JOKE = 1;
    public static final int GIFT = 2;
    public static final int HUG = 3;
    public static final int FOLLOW = 4;
    public static final int STAY = 5;
    public static final int FREE = 6;

    private Actions() {}

    /** Executa a ação e devolve a fala do mob (pode ser null). */
    public static Component perform(ServerPlayer player, Mob mob, int action) {
        switch (action) {
            case TALK:
                return talk(player, mob);
            case JOKE:
                return joke(player, mob);
            case GIFT:
                return gift(player, mob);
            case HUG:
                return hug(player, mob);
            case FOLLOW:
                return follow(player, mob);
            case STAY:
                return stay(player, mob);
            case FREE:
                return free(mob);
            default:
                return null;
        }
    }

    private static Component talk(ServerPlayer player, Mob mob) {
        UUID id = player.getUUID();
        int f = FriendData.friendship(mob, id);
        int tier = f < 10 ? 0 : f < 40 ? 1 : f < 75 ? 2 : 3;

        List<String> pool = new ArrayList<>();
        for (int i = 1; i <= 3; i++) pool.add("talk.t" + tier + "." + i);
        if (tier < 3) {
            if (mob instanceof Enemy) {
                for (int i = 1; i <= 4; i++) pool.add("talk.enemy." + i);
            } else if (mob instanceof Animal) {
                for (int i = 1; i <= 4; i++) pool.add("talk.animal." + i);
            }
        }
        String key = pool.get(mob.getRandom().nextInt(pool.size()));

        if (ready(mob, "talkAt", 600)) {
            FriendData.addFriendship(mob, id, 2);
            particles(mob, ParticleTypes.HEART, 2);
        }
        return line(key);
    }

    private static Component joke(ServerPlayer player, Mob mob) {
        UUID id = player.getUUID();
        if (!ready(mob, "jokeAt", 800)) {
            return line("joke.again");
        }
        int f = FriendData.friendship(mob, id);
        int chance = Math.min(90, 35 + f / 2);
        int n = 1 + mob.getRandom().nextInt(3);
        if (mob.getRandom().nextInt(100) < chance) {
            FriendData.addFriendship(mob, id, 2);
            particles(mob, ParticleTypes.HAPPY_VILLAGER, 5);
            return line("joke.good." + n);
        }
        particles(mob, ParticleTypes.SMOKE, 4);
        return line("joke.bad." + n);
    }

    private static Component gift(ServerPlayer player, Mob mob) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return line("gift.empty");
        }
        UUID id = player.getUUID();
        String itemId = String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));

        int base = GiftValues.value(stack);
        int scaled = Math.max(2, Math.min(10, (base + 1) / 3));

        int repeats = 0;
        for (String past : FriendData.recentGifts(mob)) {
            if (past.equals(itemId)) repeats++;
        }
        double factor = repeats == 0 ? 1.0 : repeats == 1 ? 0.6 : repeats == 2 ? 0.3 : 0.0;
        int delta = (int) Math.round(scaled * factor);
        if (delta <= 0) {
            return line("gift.full");
        }

        boolean edible = stack.isEdible();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        FriendData.pushGift(mob, itemId);
        FriendData.addFriendship(mob, id, delta);
        if (edible) {
            mob.heal(2.0F);
        }
        particles(mob, ParticleTypes.HEART, 3 + delta);

        String tier = delta >= 7 ? "best" : delta >= 4 ? "better" : delta >= 2 ? "good" : "meh";
        return line("gift." + tier);
    }

    private static Component hug(ServerPlayer player, Mob mob) {
        UUID id = player.getUUID();
        if (FriendData.friendship(mob, id) < 5) {
            particles(mob, ParticleTypes.ANGRY_VILLAGER, 3);
            return line("hug.refuse");
        }
        if (!ready(mob, "hugAt", 1200)) {
            return line("hug.again");
        }
        FriendData.addFriendship(mob, id, 3);
        particles(mob, ParticleTypes.HEART, 8);
        return line("hug.ok");
    }

    private static Component follow(ServerPlayer player, Mob mob) {
        UUID id = player.getUUID();
        if (FriendData.friendship(mob, id) < Config.MIN_FRIENDSHIP_FOLLOW.get()) {
            return line("follow.refuse");
        }
        if (FriendData.mode(mob) == FriendData.STAY) {
            restoreAi(mob);
        }
        FriendData.setMode(mob, FriendData.FOLLOW);
        FriendData.setOwner(mob, id);
        mob.setTarget(null);
        mob.getNavigation().stop();
        return line("follow.ok");
    }

    private static Component stay(ServerPlayer player, Mob mob) {
        UUID id = player.getUUID();
        if (FriendData.friendship(mob, id) < Config.MIN_FRIENDSHIP_FOLLOW.get()) {
            return line("stay.refuse");
        }
        if (FriendData.mode(mob) != FriendData.STAY) {
            FriendData.setPrevNoAi(mob, mob.isNoAi());
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }
        FriendData.setMode(mob, FriendData.STAY);
        FriendData.setOwner(mob, id);
        return line("stay.ok");
    }

    private static Component free(Mob mob) {
        release(mob);
        return line("free.ok");
    }

    /** Solta o mob: volta a andar livremente e devolve a IA original. */
    public static void release(Mob mob) {
        if (FriendData.mode(mob) == FriendData.STAY) {
            restoreAi(mob);
        }
        FriendData.setMode(mob, FriendData.FREE);
    }

    private static void restoreAi(Mob mob) {
        mob.setNoAi(FriendData.prevNoAi(mob));
    }

    private static boolean ready(Mob mob, String key, long cooldownTicks) {
        long now = mob.level().getGameTime();
        long last = FriendData.getLong(mob, key);
        if (last == 0L || now < last || now - last >= cooldownTicks) {
            FriendData.putLong(mob, key, now);
            return true;
        }
        return false;
    }

    private static void particles(Mob mob, ParticleOptions type, int count) {
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(type, mob.getX(), mob.getY() + mob.getBbHeight() + 0.1D, mob.getZ(),
                count, 0.35D, 0.25D, 0.35D, 0.02D);
        }
    }

    private static Component line(String key) {
        return Component.translatable("mca_everywhere." + key);
    }
}
