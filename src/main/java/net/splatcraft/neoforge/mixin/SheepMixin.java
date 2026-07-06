package net.splatcraft.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.splatcraft.neoforge.data.InkColorComponent;
import net.splatcraft.neoforge.data.InkOverlayInfo;
import net.splatcraft.neoforge.registry.SplatcraftAttachments;
import net.splatcraft.neoforge.registry.SplatcraftItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Sheep.class)
public class SheepMixin {
    @WrapOperation(
            method = "shear",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/Sheep;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/entity/item/ItemEntity;"
            )
    )
    private ItemEntity splatcraft$spawnInkedWool(
            Sheep sheep,
            ItemLike item,
            int offsetY,
            Operation<ItemEntity> original
    ) {
        int woolColor = sheep.getExistingData(SplatcraftAttachments.INK_OVERLAY.get())
                .map(InkOverlayInfo::woolColor)
                .orElse(-1);
        if (woolColor < 0) {
            return original.call(sheep, item, offsetY);
        }

        ItemStack stack = new ItemStack(SplatcraftItems.INK_STAINED_WOOL.get());
        InkColorComponent.setColor(stack, woolColor);
        InkColorComponent.setColorLocked(stack, true);
        return sheep.spawnAtLocation(stack, (float) offsetY);
    }
}
