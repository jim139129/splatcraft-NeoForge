package net.splatcraft.neoforge.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.splatcraft.neoforge.registry.SplatcraftSounds;

public final class SplatcraftBlockProperties {
    private SplatcraftBlockProperties() {
    }

    public static BlockBehaviour.Properties inked() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(1.2F, 3.0F)
                .sound(SplatcraftSounds.SOUND_TYPE_INK);
    }

    public static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL);
    }

    public static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops()
                .strength(3.0F, 3.0F)
                .sound(SoundType.STONE);
    }

    public static BlockBehaviour.Properties glass() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .noOcclusion();
    }

    public static BlockBehaviour.Properties soft() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(0.8F)
                .sound(SoundType.WOOL);
    }

    public static BlockBehaviour.Properties switchLike() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5F, 3.0F)
                .sound(SoundType.METAL);
    }

    public static BlockBehaviour.Properties barrier() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(-1.0F, 3_600_000.0F)
                .noLootTable()
                .noOcclusion();
    }

    public static BlockBehaviour.Properties lightInk() {
        return inked().lightLevel(state -> 9);
    }

    public static Block block(BlockBehaviour.Properties properties) {
        return new Block(properties);
    }
}
