package net.splatcraft.neoforge.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.neoforge.Splatcraft;
import net.splatcraft.neoforge.entity.InkSquidEntity;

public class InkSquidRenderer extends MobRenderer<InkSquidEntity, InkSquidModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/entity/ink_squid_overlay.png");

    public InkSquidRenderer(EntityRendererProvider.Context context) {
        super(context, new InkSquidModel(context.bakeLayer(InkSquidModel.LAYER_LOCATION)), 0.5F);
        addLayer(new InkSquidColorLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(InkSquidEntity entity) {
        return TEXTURE;
    }
}
