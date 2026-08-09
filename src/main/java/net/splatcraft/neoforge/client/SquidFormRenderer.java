package net.splatcraft.neoforge.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.neoforge.Splatcraft;

final class SquidFormRenderer extends LivingEntityRenderer<Player, InkSquidModel<Player>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Splatcraft.MOD_ID, "textures/entity/ink_squid_overlay.png");

    SquidFormRenderer(EntityRendererProvider.Context context) {
        super(context, new InkSquidModel<>(context.bakeLayer(InkSquidModel.LAYER_LOCATION)), 0.5F);
        addLayer(new InkSquidColorLayer<>(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(Player player) {
        return TEXTURE;
    }
}
