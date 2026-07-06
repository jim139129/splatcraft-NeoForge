package net.splatcraft.neoforge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.neoforge.data.SplatcraftTags;
import net.splatcraft.neoforge.entity.SplatcraftEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class SquidPassthroughMixin {
    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("TAIL"),
            cancellable = true
    )
    private void splatcraft$getCollisionShape(
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> callback
    ) {
        BlockBehaviour.BlockStateBase state = (BlockBehaviour.BlockStateBase) (Object) this;
        try {
            if (state.is(SplatcraftTags.Blocks.SQUID_PASSTHROUGH)
                    && context instanceof EntityCollisionContext entityContext
                    && entityContext.getEntity() != null
                    && SplatcraftEntityState.isSquid(entityContext.getEntity())) {
                callback.setReturnValue(Shapes.empty());
            }
        } catch (IllegalStateException ignored) {
            // Tags may be unavailable during early bootstrap shape queries.
        }
    }
}
