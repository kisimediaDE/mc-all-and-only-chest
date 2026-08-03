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
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.Action;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enforces the challenge's block drop rule.
 */
public final class ChallengeBlockListener implements Listener {

    private static final String STORAGE_ERROR =
            "§cDer Block konnte nicht sicher gespeichert werden. Bitte versuche es erneut.";
    private static final Set<String> WATER_BREAKABLE_VEGETATION = Set.of(
            "WHEAT", "CARROTS", "POTATOES", "BEETROOTS",
            "MELON_STEM", "ATTACHED_MELON_STEM",
            "PUMPKIN_STEM", "ATTACHED_PUMPKIN_STEM",
            "NETHER_WART", "COCOA", "SWEET_BERRY_BUSH",
            "TORCHFLOWER_CROP", "TORCHFLOWER",
            "PITCHER_CROP", "PITCHER_PLANT",
            "SUGAR_CANE", "BAMBOO", "BAMBOO_SAPLING", "CACTUS",
            "SHORT_GRASS", "TALL_GRASS", "FERN", "LARGE_FERN",
            "SHORT_DRY_GRASS", "TALL_DRY_GRASS", "DEAD_BUSH", "BUSH",
            "LEAF_LITTER", "WILDFLOWERS", "FIREFLY_BUSH",
            "DANDELION", "POPPY", "BLUE_ORCHID", "ALLIUM",
            "AZURE_BLUET", "RED_TULIP", "ORANGE_TULIP", "WHITE_TULIP",
            "PINK_TULIP", "OXEYE_DAISY", "CORNFLOWER",
            "LILY_OF_THE_VALLEY", "WITHER_ROSE", "SUNFLOWER", "LILAC",
            "ROSE_BUSH", "PEONY", "CACTUS_FLOWER",
            "OPEN_EYEBLOSSOM", "CLOSED_EYEBLOSSOM",
            "OAK_SAPLING", "SPRUCE_SAPLING", "BIRCH_SAPLING",
            "JUNGLE_SAPLING", "ACACIA_SAPLING", "DARK_OAK_SAPLING",
            "CHERRY_SAPLING", "PALE_OAK_SAPLING",
            "BROWN_MUSHROOM", "RED_MUSHROOM",
            "CRIMSON_FUNGUS", "WARPED_FUNGUS",
            "CRIMSON_ROOTS", "WARPED_ROOTS", "NETHER_SPROUTS",
            "PALE_HANGING_MOSS", "MOSS_CARPET", "PALE_MOSS_CARPET",
            "VINE", "HANGING_ROOTS", "SPORE_BLOSSOM",
            "SMALL_DRIPLEAF", "BIG_DRIPLEAF", "BIG_DRIPLEAF_STEM"
    );

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
    public void onStructureGrow(StructureGrowEvent event) {
        try {
            repository.untrackAll(event.getBlocks().stream()
                    .map(BlockState::getBlock)
                    .map(BlockPosition::from)
                    .toList());
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            logger.log(Level.SEVERE, "Cancelled structure growth after a storage failure", exception);
        }
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
    public void onWaterFlowIntoVegetation(BlockFromToEvent event) {
        if (event.getBlock().getType() != Material.WATER
                || !isWaterBreakableVegetation(event.getToBlock().getType())) {
            return;
        }

        try {
            if (repository.isTracked(BlockPosition.from(event.getToBlock()))) {
                return;
            }

            // Removing the natural vegetation before Vanilla processes the
            // incoming water prevents plants and farm loot from spawning.
            // The flow itself remains active and can occupy the cleared block.
            event.getToBlock().setType(Material.AIR, false);
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            logger.log(Level.SEVERE, "Cancelled vegetation flooding after a storage failure", exception);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVegetationPhysicsFromWater(BlockPhysicsEvent event) {
        if (!isWaterBreakableVegetation(event.getBlock().getType())
                || event.getSourceBlock().getType() != Material.WATER) {
            return;
        }

        try {
            if (repository.isTracked(BlockPosition.from(event.getBlock()))) {
                return;
            }

            // Falling water first enters the air above vegetation. Vanilla
            // then destroys the block through a physics update rather than a
            // second BlockFromToEvent, so handle that path without loot.
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR, false);
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            logger.log(Level.SEVERE, "Cancelled vegetation physics after a storage failure", exception);
        }
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

    private static boolean isWaterBreakableVegetation(Material material) {
        return WATER_BREAKABLE_VEGETATION.contains(material.name());
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
