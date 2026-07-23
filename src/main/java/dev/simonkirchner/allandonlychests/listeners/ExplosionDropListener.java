package dev.simonkirchner.allandonlychests.listeners;

import dev.simonkirchner.allandonlychests.storage.BlockPosition;
import dev.simonkirchner.allandonlychests.storage.PlacedBlockRepository;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allows explosions to destroy blocks while suppressing every block drop.
 */
public final class ExplosionDropListener implements Listener {

    private final PlacedBlockRepository repository;
    private final Logger logger;

    public ExplosionDropListener(PlacedBlockRepository repository, Logger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        try {
            processAffectedBlocks(event.blockList());
            event.setYield(0.0F);
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            logger.log(Level.SEVERE, "Cancelled entity explosion after a storage failure", exception);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        try {
            processAffectedBlocks(event.blockList());
            event.setYield(0.0F);
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            logger.log(Level.SEVERE, "Cancelled block explosion after a storage failure", exception);
        }
    }

    private void processAffectedBlocks(List<Block> blocks) {
        Set<Block> snowBlocks = new LinkedHashSet<>();
        for (Block block : List.copyOf(blocks)) {
            if (isSnow(block)) {
                snowBlocks.add(block);
            }

            Block blockAbove = block.getRelative(BlockFace.UP);
            if (isSnow(blockAbove)) {
                snowBlocks.add(blockAbove);
            }
        }

        List<Block> trackedCandidates = new ArrayList<>(blocks);
        trackedCandidates.addAll(snowBlocks);
        repository.untrackAll(trackedCandidates.stream().map(BlockPosition::from).toList());

        // Unsupported snow layers can break through a later physics update and
        // bypass the explosion yield. Remove those blocks before that update.
        for (Block snowBlock : snowBlocks) {
            snowBlock.setType(Material.AIR, false);
            blocks.remove(snowBlock);
        }
    }

    private static boolean isSnow(Block block) {
        return block.getType() == Material.SNOW || block.getType() == Material.SNOW_BLOCK;
    }
}
