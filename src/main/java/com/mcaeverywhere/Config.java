package com.mcaeverywhere;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public enum Trigger {
        /** Shift + botão direito (padrão, não atrapalha as interações normais). */
        SNEAK_RIGHT_CLICK,
        /** Botão direito com a mão principal vazia. */
        EMPTY_HAND_RIGHT_CLICK,
        /** Qualquer botão direito (substitui a interação normal do mob!). */
        RIGHT_CLICK
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<Trigger> TRIGGER;
    public static final ForgeConfigSpec.IntValue MIN_FRIENDSHIP_FOLLOW;
    public static final ForgeConfigSpec.BooleanValue PEACEFUL_FRIENDS;
    public static final ForgeConfigSpec.IntValue PEACEFUL_FRIENDSHIP;
    public static final ForgeConfigSpec.IntValue TELEPORT_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        TRIGGER = b.comment(
                "Como abrir o menu de interação num mob.",
                "SNEAK_RIGHT_CLICK = Shift + botão direito | EMPTY_HAND_RIGHT_CLICK = botão direito com a mão vazia",
                "RIGHT_CLICK = qualquer botão direito (bloqueia a interação normal do mob, ex.: ordenhar vaca)")
            .defineEnum("trigger", Trigger.SNEAK_RIGHT_CLICK);

        MIN_FRIENDSHIP_FOLLOW = b.comment(
                "Amizade mínima (0-100) para o mob aceitar 'Seguir' / 'Ficar'. 0 = sempre aceita.")
            .defineInRange("minFriendshipToFollow", 0, 0, 100);

        PEACEFUL_FRIENDS = b.comment(
                "Se verdadeiro, mobs que são amigos do jogador deixam de atacá-lo (inclusive hostis).")
            .define("peacefulFriends", true);

        PEACEFUL_FRIENDSHIP = b.comment(
                "Amizade (0-100) a partir da qual o mob deixa de atacar o jogador.")
            .defineInRange("peacefulFriendship", 10, 0, 100);

        TELEPORT_DISTANCE = b.comment(
                "Distância (blocos) a partir da qual um mob que está seguindo teleporta para perto do jogador.")
            .defineInRange("followTeleportDistance", 20, 8, 128);

        BLACKLIST = b.comment(
                "Mobs que NÃO abrem o menu, por id. Ex.: [\"minecraft:ender_dragon\", \"minecraft:wither\"]")
            .defineListAllowEmpty("blacklist", List.<String>of(), o -> o instanceof String);

        SPEC = b.build();
    }

    private Config() {}
}
