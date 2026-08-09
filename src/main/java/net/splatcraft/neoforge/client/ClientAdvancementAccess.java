package net.splatcraft.neoforge.client;

import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.neoforge.mixin.ClientAdvancementsAccessor;

final class ClientAdvancementAccess {
    private ClientAdvancementAccess() {
    }

    static boolean isDone(ResourceLocation advancementId) {
        if (Minecraft.getInstance().getConnection() == null) {
            return true;
        }

        ClientAdvancements advancements = Minecraft.getInstance().getConnection().getAdvancements();
        AdvancementHolder holder = advancements.get(advancementId);
        if (holder == null) {
            return false;
        }

        Map<AdvancementHolder, AdvancementProgress> progress = ((ClientAdvancementsAccessor) advancements).splatcraft$getProgress();
        AdvancementProgress advancementProgress = progress.get(holder);
        return advancementProgress != null && advancementProgress.isDone();
    }
}
