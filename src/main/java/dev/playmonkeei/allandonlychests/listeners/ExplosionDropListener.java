package dev.playmonkeei.allandonlychests.listeners;

import dev.playmonkeei.allandonlychests.storage.BlockPosition;
import dev.playmonkeei.allandonlychests.storage.PlacedBlockRepository;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;

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

    private final Plugin plugin;
    private final PlacedBlockRepository repository;
    private final Logger logger;

    public ExplosionDropListener(
            Plugin plugin,
            PlacedBlockRepository repository,
            Logger logger
    ) {
        this.plugin = plugin;
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
        List<Block> affectedBlocks = List.copyOf(blocks);
        Set<Block> secondaryBlocks = new LinkedHashSet<>();
        for (Block block : affectedBlocks) {
            Block blockAbove = block.getRelative(BlockFace.UP);
            if (canBecomeUnsupported(blockAbove)) {
                secondaryBlocks.add(blockAbove);
            }
        }

        List<Block> trackedCandidates = new ArrayList<>(blocks);
        trackedCandidates.addAll(secondaryBlocks);
        repository.untrackAll(trackedCandidates.stream().map(BlockPosition::from).toList());

        // Plants, mushrooms, snow layers, redstone, and similar blocks can
        // break through a later support-physics update and bypass the
        // explosion yield. Remove them before that update.
        for (Block secondaryBlock : secondaryBlocks) {
            secondaryBlock.setType(Material.AIR, false);
            blocks.remove(secondaryBlock);
        }

        removeDelayedExplosionDrops(affectedBlocks);
    }

    private void removeDelayedExplosionDrops(List<Block> affectedBlocks) {
        if (affectedBlocks.isEmpty()) {
            return;
        }

        // Paper can create special block drops (notably snowballs) through a
        // later physics/loot step even though the explosion yield is zero.
        // On the next tick, remove only brand-new item entities immediately
        // around blocks that this exact explosion destroyed.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Set<Item> delayedDrops = new LinkedHashSet<>();
            for (Block block : affectedBlocks) {
                var center = block.getLocation().add(0.5, 0.5, 0.5);
                block.getWorld().getNearbyEntities(
                        center,
                        1.0,
                        1.0,
                        1.0,
                        entity -> entity instanceof Item item
                                && item.getTicksLived() <= 2
                ).forEach(entity -> delayedDrops.add((Item) entity));
            }
            delayedDrops.forEach(Item::remove);
        });
    }

    private static boolean canBecomeUnsupported(Block block) {
        Material type = block.getType();
        return !type.isAir()
                && !type.isSolid()
                && type != Material.WATER
                && type != Material.LAVA;
    }
}
