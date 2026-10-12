package com.mcaeverywhere;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Lê data/&lt;namespace&gt;/gifts/*.json (o mesmo formato do MCA) e usa o campo "items"
 * para saber o quanto um presente vale. Sem o MCA, usa uma tabela interna simples.
 */
public class GiftValues extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    private static volatile Map<Item, Integer> ITEMS = new HashMap<>();
    private static volatile Map<TagKey<Item>, Integer> TAGS = new HashMap<>();

    public GiftValues() {
        super(GSON, "gifts");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Item, Integer> items = new HashMap<>();
        Map<TagKey<Item>, Integer> tags = new HashMap<>();

        for (JsonElement element : files.values()) {
            if (!element.isJsonObject()) continue;
            JsonObject obj = element.getAsJsonObject();
            if (!obj.has("items") || !obj.get("items").isJsonObject()) continue;

            for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("items").entrySet()) {
                int value;
                try {
                    value = entry.getValue().getAsInt();
                } catch (RuntimeException ex) {
                    continue;
                }
                if (value <= 0) continue;

                String key = entry.getKey();
                try {
                    if (key.startsWith("#")) {
                        TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(key.substring(1)));
                        tags.merge(tag, value, Math::max);
                    } else {
                        ResourceLocation id = new ResourceLocation(key);
                        if (ForgeRegistries.ITEMS.containsKey(id)) {
                            items.merge(ForgeRegistries.ITEMS.getValue(id), value, Math::max);
                        }
                    }
                } catch (RuntimeException ignored) {
                    // id inválido: ignora a entrada
                }
            }
        }
        ITEMS = items;
        TAGS = tags;
    }

    /** Valor bruto do presente (sempre &gt;= 1). Escala parecida com a do MCA (5 = ok, 15 = ótimo, 25 = incrível). */
    public static int value(ItemStack stack) {
        Integer direct = ITEMS.get(stack.getItem());
        int best = direct == null ? 0 : direct;
        for (Map.Entry<TagKey<Item>, Integer> e : TAGS.entrySet()) {
            if (e.getValue() > best && stack.is(e.getKey())) {
                best = e.getValue();
            }
        }
        return best > 0 ? best : fallback(stack);
    }

    private static int fallback(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.NETHERITE_INGOT || item == Items.DIAMOND) return 20;
        if (item == Items.EMERALD) return 18;
        if (item == Items.GOLD_INGOT) return 10;
        if (item == Items.IRON_INGOT) return 6;
        if (stack.is(ItemTags.FLOWERS)) return 8;
        FoodProperties food = item.getFoodProperties();
        if (food != null) return 4 + food.getNutrition() / 2;
        return 2;
    }
}
