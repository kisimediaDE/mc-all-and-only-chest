package dev.playmonkeei.allandonlychests.listeners;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import dev.playmonkeei.allandonlychests.storage.BlockPosition;
import dev.playmonkeei.allandonlychests.storage.PlacedBlockRepository;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrushableBlock;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.Action;

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

    private final Plugin plugin;
    private final PlacedBlockRepository repository;
    private final Logger logger;

    public ChallengeBlockListener(
            Plugin plugin,
            PlacedBlockRepository repository,
            Logger logger
    ) {
        this.plugin = plugin;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDestroyedByWorld(BlockDestroyEvent event) {
        try {
            boolean playerPlaced = repository.untrack(BlockPosition.from(event.getBlock()));
            if (!playerPlaced) {
                event.setWillDrop(false);
            }
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            logger.log(Level.SEVERE, "Cancelled a world-caused block destruction after a storage failure", exception);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (repository.isTracked(BlockPosition.from(event.getBlock()))) {
            return;
        }

        // LeavesDecayEvent cannot disable drops independently. Cancel the
        // Vanilla decay and remove the natural leaf without loot instead.
        event.setCancelled(true);
        event.getBlock().setType(Material.AIR, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getShooter() instanceof BlockProjectileSource source)
                || repository.isTracked(BlockPosition.from(source.getBlock()))) {
            return;
        }

        // Natural dispenser traps may still fire and hurt players, but their
        // arrows must not become an item source outside allowed containers.
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (event.getBlock().getType() != Material.DISPENSER
                || repository.isTracked(BlockPosition.from(event.getBlock()))) {
            return;
        }

        Material dispensedMaterial = event.getItem().getType();
        var world = event.getBlock().getWorld();
        var dispenserLocation = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            world.getNearbyEntities(
                    dispenserLocation,
                    3.0,
                    3.0,
                    3.0,
                    entity -> entity.getTicksLived() <= 3
                            && (
                                    isArrow(dispensedMaterial)
                                            && entity instanceof AbstractArrow
                                    || entity instanceof Item item
                                            && isDispensedItem(item, dispensedMaterial)
                            )
            ).forEach(entity -> {
                if (entity instanceof AbstractArrow arrow) {
                    arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                } else {
                    entity.remove();
                }
            });
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChiseledBookshelfInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null
                || event.getClickedBlock().getType() != Material.CHISELED_BOOKSHELF
                || repository.isTracked(BlockPosition.from(event.getClickedBlock()))) {
            return;
        }

        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) {
            event.getPlayer().sendMessage(
                    "§cNatürlich generierte gemeißelte Bücherregale dürfen nicht benutzt werden."
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!repository.isTracked(BlockPosition.from(event.getBlock()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrushableBlockDrop(BlockDropItemEvent event) {
        if (event.getBlockState() instanceof BrushableBlock
                && !repository.isTracked(BlockPosition.from(event.getBlock()))) {
            event.setCancelled(true);
        }
    }

    private static boolean isArrow(Material material) {
        return material == Material.ARROW
                || material == Material.SPECTRAL_ARROW
                || material == Material.TIPPED_ARROW;
    }

    private static boolean isDispensedItem(Item item, Material dispensedMaterial) {
        Material droppedMaterial = item.getItemStack().getType();
        return droppedMaterial == dispensedMaterial
                || dispensedMaterial == Material.WATER_BUCKET
                        && droppedMaterial == Material.BUCKET;
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
