package net.splatcraft.neoforge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.splatcraft.neoforge.Splatcraft;

public final class SplatcraftStats {
    public static final ResourceLocation TURF_WARS_WON = id("turf_wars_won");
    public static final ResourceLocation BLOCKS_INKED = id("blocks_inked");
    public static final ResourceLocation WEAPONS_CRAFTED = id("weapons_crafted");
    public static final ResourceLocation INKWELLS_CRAFTED = id("inkwells_crafted");
    public static final ResourceLocation SQUID_TIME = id("squid_time");
    public static final ResourceLocation SUBS_USED = id("subs_used");
    public static final ResourceLocation SPECIALS_USED = id("specials_used");

    private SplatcraftStats() {
    }

    public static void register(RegisterEvent event) {
        event.register(Registries.CUSTOM_STAT, helper -> {
            register(helper, TURF_WARS_WON, StatFormatter.DEFAULT);
            register(helper, BLOCKS_INKED, StatFormatter.DEFAULT);
            register(helper, WEAPONS_CRAFTED, StatFormatter.DEFAULT);
            register(helper, INKWELLS_CRAFTED, StatFormatter.DEFAULT);
            register(helper, SQUID_TIME, StatFormatter.TIME);
            register(helper, SUBS_USED, StatFormatter.DEFAULT);
            register(helper, SPECIALS_USED, StatFormatter.DEFAULT);
        });
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, name);
    }

    private static void register(RegisterEvent.RegisterHelper<ResourceLocation> helper, ResourceLocation id, StatFormatter formatter) {
        helper.register(id, id);
        Stats.CUSTOM.get(id, formatter);
    }
}
