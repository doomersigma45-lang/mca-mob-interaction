package com.mcaeverywhere;

import com.mcaeverywhere.net.Net;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Leva o sistema de interação do Minecraft Comes Alive para qualquer Mob.
 * Não depende do MCA em tempo de compilação: usa só a API do Forge.
 */
@Mod(MCAEverywhere.MODID)
public class MCAEverywhere {
    public static final String MODID = "mca_everywhere";

    public MCAEverywhere() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        Net.register();
    }
}
