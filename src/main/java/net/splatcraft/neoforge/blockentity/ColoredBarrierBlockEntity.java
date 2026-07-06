package net.splatcraft.neoforge.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.splatcraft.neoforge.data.InkColorData;
import net.splatcraft.neoforge.entity.ColoredEntity;
import net.splatcraft.neoforge.entity.SpawnShieldEntity;
import net.splatcraft.neoforge.player.SplatcraftPlayerInfoEvents;
import net.splatcraft.neoforge.registry.SplatcraftBlockEntities;
import net.splatcraft.neoforge.registry.SplatcraftBlocks;
import net.splatcraft.neoforge.worldink.InkDamageUtils;

public class ColoredBarrierBlockEntity extends StageBarrierBlockEntity {
    private int color = InkColorData.DEFAULT_COLOR;
    private boolean inverted;
    private String team = "";

    public ColoredBarrierBlockEntity(BlockPos pos, BlockState state) {
        super(SplatcraftBlockEntities.COLOR_BARRIER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ColoredBarrierBlockEntity barrier) {
        if (barrier.activeTime > 0) {
            barrier.activeTime--;
        }

        for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(0.05D))) {
            if (entity instanceof SpawnShieldEntity || entityColor(entity) < 0 || barrier.canAllowThrough(entity)) {
                continue;
            }
            barrier.resetActiveTime();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Color", color);
        tag.putString("Team", team);
        tag.putBoolean("Inverted", inverted);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        color = InkColorBlockEntity.readColor(tag, -1);
        team = tag.getString("Team");
        inverted = tag.getBoolean("Inverted");
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        if (this.color == color) {
            return;
        }
        this.color = color;
        syncBlockEntity();
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        if (this.inverted == inverted) {
            return;
        }
        this.inverted = inverted;
        syncBlockEntity();
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        String normalizedTeam = team == null ? "" : team;
        if (this.team.equals(normalizedTeam)) {
            return;
        }
        this.team = normalizedTeam;
        syncBlockEntity();
    }

    public boolean canAllowThrough(Entity entity) {
        int entityColor = entityColor(entity);
        if (entityColor < 0 || level == null) {
            return !blocksColor();
        }
        return blocksColor() != InkDamageUtils.sameColor(level, worldPosition, entityColor, color);
    }

    public boolean blocksColor() {
        return level != null && level.getBlockState(worldPosition).is(SplatcraftBlocks.DENIED_COLOR_BARRIER.get());
    }

    public static int entityColor(Entity entity) {
        if (entity instanceof Player player) {
            return SplatcraftPlayerInfoEvents.color(player);
        }
        return entity instanceof ColoredEntity colored ? colored.splatcraftColor() : -1;
    }
}
