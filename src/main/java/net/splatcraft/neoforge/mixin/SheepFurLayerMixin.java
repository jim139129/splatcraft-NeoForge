package net.splatcraft.neoforge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SheepFurLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.animal.Sheep;
import net.splatcraft.neoforge.data.InkOverlayInfo;
import net.splatcraft.neoforge.registry.SplatcraftAttachments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheepFurLayer.class)
public abstract class SheepFurLayerMixin extends RenderLayer<Sheep, SheepModel<Sheep>> {
    private static final ResourceLocation SPLATCRAFT$SHEEP_FUR_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/sheep/sheep_fur.png");

    @Shadow
    @Final
    private SheepFurModel<Sheep> model;

    protected SheepFurLayerMixin(RenderLayerParent<Sheep, SheepModel<Sheep>> renderer) {
        super(renderer);
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/Sheep;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splatcraft$renderInkedFur(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Sheep sheep,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback
    ) {
        if (sheep.isSheared() || sheep.isInvisible()) {
            return;
        }

        int woolColor = sheep.getExistingData(SplatcraftAttachments.INK_OVERLAY.get())
                .map(InkOverlayInfo::woolColor)
                .orElse(-1);
        if (woolColor < 0) {
            return;
        }

        coloredCutoutModelCopyLayerRender(
                this.getParentModel(),
                this.model,
                SPLATCRAFT$SHEEP_FUR_LOCATION,
                poseStack,
                buffer,
                packedLight,
                sheep,
                limbSwing,
                limbSwingAmount,
                ageInTicks,
                netHeadYaw,
                headPitch,
                partialTick,
                FastColor.ARGB32.opaque(woolColor));
        callback.cancel();
    }
}
