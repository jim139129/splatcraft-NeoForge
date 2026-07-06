package net.splatcraft.neoforge.client;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.ResourceLocation;

final class ClientAdvancementAccess {
    private static Field progressField;
    private static boolean progressFieldResolved;

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

        Map<AdvancementHolder, AdvancementProgress> progress = progress(advancements);
        if (progress == null) {
            return true;
        }
        AdvancementProgress advancementProgress = progress.get(holder);
        return advancementProgress != null && advancementProgress.isDone();
    }

    @SuppressWarnings("unchecked")
    private static Map<AdvancementHolder, AdvancementProgress> progress(ClientAdvancements advancements) {
        Field field = progressField();
        if (field == null) {
            return null;
        }

        try {
            return (Map<AdvancementHolder, AdvancementProgress>) field.get(advancements);
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    private static Field progressField() {
        if (progressFieldResolved) {
            return progressField;
        }
        progressFieldResolved = true;
        for (Field field : ClientAdvancements.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                progressField = field;
                break;
            }
        }
        return progressField;
    }
}
