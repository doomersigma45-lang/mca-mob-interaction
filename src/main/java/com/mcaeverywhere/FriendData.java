package com.mcaeverywhere;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Mob;

/** Dados de amizade guardados no NBT persistente do próprio mob (sobrevive a reinicios). */
public final class FriendData {
    public static final int FREE = 0;
    public static final int FOLLOW = 1;
    public static final int STAY = 2;

    private static final String KEY = "MCAEverywhere";
    private static final int MAX_FRIENDSHIP = 100;
    private static final int GIFT_HISTORY = 6;

    private FriendData() {}

    @Nullable
    private static CompoundTag peek(Mob mob) {
        CompoundTag root = mob.getPersistentData();
        return root.contains(KEY, Tag.TAG_COMPOUND) ? root.getCompound(KEY) : null;
    }

    private static CompoundTag edit(Mob mob) {
        CompoundTag root = mob.getPersistentData();
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) {
            root.put(KEY, new CompoundTag());
        }
        return root.getCompound(KEY);
    }

    public static boolean has(Mob mob) {
        return peek(mob) != null;
    }

    public static int friendship(Mob mob, UUID player) {
        CompoundTag d = peek(mob);
        if (d == null) return 0;
        return d.getCompound("hearts").getInt(player.toString());
    }

    public static void setFriendship(Mob mob, UUID player, int value) {
        CompoundTag d = edit(mob);
        if (!d.contains("hearts", Tag.TAG_COMPOUND)) {
            d.put("hearts", new CompoundTag());
        }
        d.getCompound("hearts").putInt(player.toString(), Math.max(0, Math.min(MAX_FRIENDSHIP, value)));
    }

    public static int addFriendship(Mob mob, UUID player, int delta) {
        int value = Math.max(0, Math.min(MAX_FRIENDSHIP, friendship(mob, player) + delta));
        setFriendship(mob, player, value);
        return value;
    }

    public static int mode(Mob mob) {
        CompoundTag d = peek(mob);
        return d == null ? FREE : d.getInt("mode");
    }

    public static void setMode(Mob mob, int mode) {
        edit(mob).putInt("mode", mode);
    }

    @Nullable
    public static UUID owner(Mob mob) {
        CompoundTag d = peek(mob);
        return d != null && d.hasUUID("owner") ? d.getUUID("owner") : null;
    }

    public static void setOwner(Mob mob, UUID owner) {
        edit(mob).putUUID("owner", owner);
    }

    public static boolean prevNoAi(Mob mob) {
        CompoundTag d = peek(mob);
        return d != null && d.getBoolean("prevNoAi");
    }

    public static void setPrevNoAi(Mob mob, boolean value) {
        edit(mob).putBoolean("prevNoAi", value);
    }

    public static long getLong(Mob mob, String key) {
        CompoundTag d = peek(mob);
        return d == null ? 0L : d.getLong(key);
    }

    public static void putLong(Mob mob, String key, long value) {
        edit(mob).putLong(key, value);
    }

    public static List<String> recentGifts(Mob mob) {
        List<String> out = new ArrayList<>();
        CompoundTag d = peek(mob);
        if (d == null) return out;
        ListTag list = d.getList("gifts", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    public static void pushGift(Mob mob, String itemId) {
        CompoundTag d = edit(mob);
        ListTag list = d.getList("gifts", Tag.TAG_STRING);
        list.add(StringTag.valueOf(itemId));
        while (list.size() > GIFT_HISTORY) {
            list.remove(0);
        }
        d.put("gifts", list);
    }
}
