package dev.playmonkeei.allandonlychests.storage;

import org.bukkit.block.Block;

import java.util.UUID;

/**
 * Stable, serializable identity of a block across server restarts.
 */
public record BlockPosition(UUID worldId, int x, int y, int z) {

    public static BlockPosition from(Block block) {
        return new BlockPosition(
                block.getWorld().getUID(),
                block.getX(),
                block.getY(),
                block.getZ()
        );
    }

    public BlockPosition offset(int deltaX, int deltaY, int deltaZ) {
        return new BlockPosition(worldId, x + deltaX, y + deltaY, z + deltaZ);
    }
}
