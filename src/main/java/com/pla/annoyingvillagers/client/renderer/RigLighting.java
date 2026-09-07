package com.pla.annoyingvillagers.client.renderer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/** Keeps a rig's single light probe from blacking out the entire model inside a wall. */
public final class RigLighting {
    private static final Direction[] NEIGHBORS = Direction.values();

    private RigLighting() {
    }

    public static int getBrightness(Level level, LightLayer layer, BlockPos probe) {
        int brightness = level.getBrightness(layer, probe);
        if (!level.getBlockState(probe).isSolidRender(level, probe)) {
            return brightness;
        }

        // Only rescue a probe inside an opaque block. Sample actual adjacent light,
        // so dark rooms stay dark and ordinary lighting does not become full-bright.
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (Direction direction : NEIGHBORS) {
            neighbor.setWithOffset(probe, direction);
            if (level.hasChunkAt(neighbor)
                    && !level.getBlockState(neighbor).isSolidRender(level, neighbor)) {
                brightness = Math.max(brightness, level.getBrightness(layer, neighbor));
            }
        }
        return brightness;
    }
}
