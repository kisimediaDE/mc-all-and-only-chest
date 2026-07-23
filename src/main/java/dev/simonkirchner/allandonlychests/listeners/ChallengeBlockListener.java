package dev.simonkirchner.allandonlychests.listeners;

import dev.simonkirchner.allandonlychests.storage.BlockPosition;
import dev.simonkirchner.allandonlychests.storage.PlacedBlockRepository;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.TNTPrimeEvent;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enforces the challenge's block drop rule.
 */
public final class ChallengeBlockListener implements Listener {

    private static final String STORAGE_ERROR =
            "§cDer Block konnte nicht sicher gespeichert werden. Bitte versuche es erneut.";

    private final PlacedBlockRepository repository;
    private final Logger logger;

    public ChallengeBlockListener(PlacedBlockRepository repository, Logger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event instanceof BlockMultiPlaceEvent) {
            return;
        }

        persistPlacement(event, List.of(BlockPosition.from(event.getBlockPlaced())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        Collection<BlockPosition> positions = event.getReplacedBlockStates().stream()
                .map(BlockState::getBlock)
                .map(BlockPosition::from)
                .toList();
        persistPlacement(event, positions);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            boolean playerPlaced = repository.untrack(BlockPosition.from(event.getBlock()));
            if (!playerPlaced) {
                event.setDropItems(false);
            }
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(STORAGE_ERROR);
            logger.log(Level.SEVERE, "Could not persist a broken player-placed block", exception);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTntPrime(TNTPrimeEvent event) {
        try {
            repository.untrack(BlockPosition.from(event.getBlock()));
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            if (event.getPrimingEntity() instanceof Player player) {
                player.sendMessage(STORAGE_ERROR);
            }
            logger.log(Level.SEVERE, "Cancelled TNT priming after a storage failure", exception);
        }
    }

    private void persistPlacement(BlockPlaceEvent event, Collection<BlockPosition> positions) {
        try {
            repository.trackAll(positions);
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(STORAGE_ERROR);
            logger.log(Level.SEVERE, "Could not persist player-placed blocks", exception);
        }
    }
}
