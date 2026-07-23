package dev.playmonkeei.allandonlychests.listeners;

import dev.playmonkeei.allandonlychests.storage.BlockPosition;
import dev.playmonkeei.allandonlychests.storage.PlacedBlockRepository;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Keeps player-placed tracking attached to blocks moved by pistons or gravity.
 */
public final class MovingBlockListener implements Listener {

    private final PlacedBlockRepository repository;
    private final Logger logger;
    private final NamespacedKey playerPlacedFallingKey;

    public MovingBlockListener(
            PlacedBlockRepository repository,
            Logger logger,
            NamespacedKey playerPlacedFallingKey
    ) {
        this.repository = repository;
        this.logger = logger;
        this.playerPlacedFallingKey = playerPlacedFallingKey;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        migratePistonBlocks(event.getBlocks(), event.getDirection(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        migratePistonBlocks(event.getBlocks(), event.getDirection(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallingBlockChange(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) {
            return;
        }

        try {
            if (event.getTo().isAir()) {
                beginFalling(event, fallingBlock);
            } else {
                finishFalling(event, fallingBlock);
            }
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            logger.log(Level.SEVERE, "Cancelled a falling block change after a storage failure", exception);
        }
    }

    private void beginFalling(EntityChangeBlockEvent event, FallingBlock fallingBlock) {
        boolean playerPlaced = repository.untrack(BlockPosition.from(event.getBlock()));
        fallingBlock.setDropItem(playerPlaced);
        if (playerPlaced) {
            fallingBlock.getPersistentDataContainer().set(
                    playerPlacedFallingKey,
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        }
    }

    private void finishFalling(EntityChangeBlockEvent event, FallingBlock fallingBlock) {
        if (!fallingBlock.getPersistentDataContainer().has(
                playerPlacedFallingKey,
                PersistentDataType.BYTE
        )) {
            fallingBlock.setDropItem(false);
            return;
        }

        repository.trackAll(List.of(BlockPosition.from(event.getBlock())));
    }

    private void migratePistonBlocks(
            List<Block> blocks,
            BlockFace movementDirection,
            org.bukkit.event.Cancellable event
    ) {
        try {
            Map<BlockPosition, BlockPosition> movements = blocks.stream()
                    .map(BlockPosition::from)
                    .collect(Collectors.toMap(
                            Function.identity(),
                            position -> position.offset(
                                    movementDirection.getModX(),
                                    movementDirection.getModY(),
                                    movementDirection.getModZ()
                            )
                    ));
            repository.moveAll(movements);
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            logger.log(Level.SEVERE, "Cancelled a piston movement after a storage failure", exception);
        }
    }
}
