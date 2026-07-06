package net.splatcraft.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.entity.SquidBumperEntity;

public class SquidBumperRenderer extends LivingEntityRenderer<SquidBumperEntity, SquidBumperModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/entity/squid_bumper_overlay.png");

    public SquidBumperRenderer(EntityRendererProvider.Context context) {
        super(context, new SquidBumperModel(context.bakeLayer(SquidBumperModel.LAYER_LOCATION)), 0.5F);
        addLayer(new SquidBumperColorLayer(this, context.getModelSet()));
    }

    @Override
    protected boolean shouldShowName(SquidBumperEntity entity) {
        return (!entity.hasCustomName() && entity.inkHealth() < SquidBumperEntity.MAX_INK_HEALTH) || super.shouldShowName(entity);
    }

    @Override
    protected void renderNameTag(
            SquidBumperEntity entity,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float partialTick
    ) {
        if (entity.hasCustomName()) {
            super.renderNameTag(entity, displayName, poseStack, bufferSource, packedLight, partialTick);
            return;
        }

        float damage = SquidBumperEntity.MAX_INK_HEALTH - entity.inkHealth();
        Component damageText = Component.literal(String.format(Locale.ROOT, "%.1f", damage));
        if (damage >= SquidBumperEntity.MAX_INK_HEALTH) {
            damageText = damageText.copy().withStyle(ChatFormatting.DARK_RED);
        }
        super.renderNameTag(entity, damageText, poseStack, bufferSource, packedLight, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(SquidBumperEntity entity) {
        return TEXTURE;
    }
}
