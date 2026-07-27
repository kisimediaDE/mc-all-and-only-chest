package dev.playmonkeei.allandonlychests.listeners;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.challenge.StructureGoal;
import dev.playmonkeei.allandonlychests.challenge.StructureGoalCatalog;
import dev.playmonkeei.allandonlychests.storage.BlockPosition;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import dev.playmonkeei.allandonlychests.storage.PlacedBlockRepository;
import dev.playmonkeei.allandonlychests.ui.ChallengeSidebar;
import dev.playmonkeei.allandonlychests.ui.ChallengeVictoryNotifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.DecoratedPot;
import org.bukkit.block.Dispenser;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Hopper;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Restricts generated structure loot to the active category and records the
 * items visible in an allowed container. Vault and Trial Spawner rewards use
 * Paper's dispense-loot event, matching the original plugin's behavior.
 */
public final class StructureLootListener implements Listener {

    private final Plugin plugin;
    private final ChallengeStateRepository stateRepository;
    private final PlacedBlockRepository placedBlockRepository;
    private final StructureGoalCatalog goalCatalog;
    private final ChallengeSidebar sidebar;
    private final NamespacedKey structureCategoryKey;
    private final NamespacedKey playerPlacedContainerEntityKey;
    private final Logger logger;

    public StructureLootListener(
            Plugin plugin,
            ChallengeStateRepository stateRepository,
            PlacedBlockRepository placedBlockRepository,
            StructureGoalCatalog goalCatalog,
            ChallengeSidebar sidebar,
            Logger logger
    ) {
        this.plugin = plugin;
        this.stateRepository = stateRepository;
        this.placedBlockRepository = placedBlockRepository;
        this.goalCatalog = goalCatalog;
        this.sidebar = sidebar;
        this.logger = logger;
        structureCategoryKey = new NamespacedKey(plugin, "structure_category");
        playerPlacedContainerEntityKey =
                new NamespacedKey(plugin, "player_placed_container_entity");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onContainerEntityPlace(EntityPlaceEvent event) {
        if (event.getPlayer() == null
                || !(event.getEntity() instanceof InventoryHolder)
                || !(event.getEntity() instanceof PersistentDataHolder dataHolder)) {
            return;
        }

        dataHolder.getPersistentDataContainer().set(
                playerPlacedContainerEntityKey,
                PersistentDataType.BYTE,
                (byte) 1
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        if (event.getInventoryHolder() instanceof Dispenser
                || event.getInventoryHolder() instanceof DecoratedPot) {
            return;
        }
        StructureCategory.fromLootTable(event.getLootTable().getKey())
                .ifPresent(category -> {
                    tagHolder(event.getInventoryHolder(), category);
                    event.getLoot().stream()
                            .filter(item -> item != null && !item.getType().isAir())
                            .findFirst()
                            .ifPresent(item -> tagItem(item, category));
                });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null || !(event.getPlayer() instanceof Player player)) {
            return;
        }

        try {
            if (isEntirelyPlayerPlaced(holder)) {
                return;
            }
            if (holder instanceof Hopper || holder instanceof HopperMinecart) {
                event.setCancelled(true);
                player.sendMessage(
                        "§cNatürlich generierte Hopper dürfen nicht benutzt werden."
                );
                return;
            }
            if (holder instanceof Dispenser || holder instanceof BrewingStand) {
                event.setCancelled(true);
                player.sendMessage("§cNur erlaubte Strukturkisten dürfen geöffnet werden.");
                return;
            }

            Optional<StructureCategory> category = categoryFor(holder);
            if (category.isEmpty()) {
                category = categoryFromItems(event.getInventory().getStorageContents());
            }
            if (category.isEmpty()) {
                if (holder instanceof Lootable || holder instanceof DoubleChest) {
                    event.setCancelled(true);
                    player.sendMessage("§cDiese Kiste gehört keiner erlaubten Struktur.");
                }
                return;
            }

            StructureCategory resolvedCategory = category.get();
            StructureCategory active = stateRepository.activeStructure().orElse(null);
            if (active != resolvedCategory || stateRepository.isCompleted(resolvedCategory)) {
                event.setCancelled(true);
                player.sendMessage(
                        "§cDiese Kiste gehört nicht zu deiner aktuell erlaubten Struktur."
                );
                return;
            }

            sourceKey(holder).ifPresent(key ->
                    stateRepository.recordVisitedSource(resolvedCategory, key)
            );
            processItems(
                    resolvedCategory,
                    Arrays.asList(event.getInventory().getStorageContents())
            );
            sidebar.refreshAll();
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            player.sendMessage("§cDer Strukturfortschritt konnte nicht sicher gespeichert werden.");
            logger.log(Level.SEVERE, "Could not process an opened structure container", exception);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDispenseLoot(BlockDispenseLootEvent event) {
        Material blockType = event.getBlock().getType();
        if (blockType != Material.VAULT && blockType != Material.TRIAL_SPAWNER) {
            return;
        }

        StructureCategory active = stateRepository.activeStructure().orElse(null);
        if (active != StructureCategory.TRIAL_CHAMBERS
                || stateRepository.isCompleted(StructureCategory.TRIAL_CHAMBERS)) {
            event.setCancelled(true);
            return;
        }

        try {
            stateRepository.recordVisitedSource(
                    StructureCategory.TRIAL_CHAMBERS,
                    sourceKey(event.getBlock())
            );
            processItems(StructureCategory.TRIAL_CHAMBERS, event.getDispensedLoot());
            sidebar.refreshAll();
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            logger.log(Level.SEVERE, "Could not process dispensed Trial Chambers loot", exception);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVaultInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null
                || event.getClickedBlock().getType() != Material.VAULT) {
            return;
        }

        StructureCategory active = stateRepository.activeStructure().orElse(null);
        if (active != StructureCategory.TRIAL_CHAMBERS
                || stateRepository.isCompleted(StructureCategory.TRIAL_CHAMBERS)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    "§cVaults sind nur während der Prüfungskammern erlaubt."
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        InventoryHolder holder = source.getHolder();
        if (holder != null
                && !isEntirelyPlayerPlaced(holder)
                && (holder instanceof Hopper
                || holder instanceof HopperMinecart
                || categoryFor(holder).isPresent()
                || categoryFromItems(source.getStorageContents()).isPresent())) {
            event.setCancelled(true);
        }
    }

    private void processItems(StructureCategory category, Collection<ItemStack> items) {
        Map<String, StructureGoal> matched = new LinkedHashMap<>();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            for (StructureGoal goal : goalCatalog.match(category, item)) {
                matched.putIfAbsent(goal.key(), goal);
            }
        }

        ChallengeStateRepository.ProgressUpdate update =
                stateRepository.recordFoundGoals(
                        category,
                        matched.values(),
                        goalCatalog.goalsFor(category)
                );
        if (update.newGoals().isEmpty()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Component.text(
                    "Neue Items gefunden (" + update.foundCount() + "/"
                            + update.totalCount() + "):",
                    NamedTextColor.GRAY
            ));
            for (StructureGoal goal : update.newGoals()) {
                player.sendMessage(
                        Component.text(" ✔ ", NamedTextColor.GREEN)
                                .append(goal.displayName().color(NamedTextColor.WHITE))
                );
            }

            if (update.completedNow()) {
                player.sendMessage(Component.text(
                        category.displayName() + " abgeschlossen!",
                        NamedTextColor.GOLD
                ));
                if (!update.challengeCompletedNow()) {
                    player.playSound(
                            player.getLocation(),
                            Sound.UI_TOAST_CHALLENGE_COMPLETE,
                            1.0f,
                            1.0f
                    );
                }
            } else {
                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_PLAYER_LEVELUP,
                        1.0f,
                        1.0f
                );
            }
        }
        if (update.challengeCompletedNow()) {
            ChallengeVictoryNotifier.announce();
        }
    }

    private Optional<StructureCategory> categoryFor(InventoryHolder holder) {
        if (holder instanceof DoubleChest doubleChest) {
            Optional<StructureCategory> left = categoryFor(doubleChest.getLeftSide());
            return left.isPresent() ? left : categoryFor(doubleChest.getRightSide());
        }

        if (holder instanceof PersistentDataHolder dataHolder) {
            String storedId = dataHolder.getPersistentDataContainer().get(
                    structureCategoryKey,
                    PersistentDataType.STRING
            );
            if (storedId != null) {
                Optional<StructureCategory> stored = StructureCategory.fromId(storedId);
                if (stored.isPresent()) {
                    return stored;
                }
            }
        }

        if (holder instanceof Lootable lootable && lootable.getLootTable() != null) {
            return StructureCategory.fromLootTable(lootable.getLootTable().getKey());
        }
        return Optional.empty();
    }

    private void tagHolder(InventoryHolder holder, StructureCategory category) {
        if (!(holder instanceof PersistentDataHolder dataHolder)) {
            return;
        }

        dataHolder.getPersistentDataContainer().set(
                structureCategoryKey,
                PersistentDataType.STRING,
                category.id()
        );
        if (holder instanceof TileState tileState) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (tileState.getBlock().getState() instanceof TileState currentState) {
                    currentState.getPersistentDataContainer().set(
                            structureCategoryKey,
                            PersistentDataType.STRING,
                            category.id()
                    );
                    currentState.update(true, false);
                }
            });
        }
    }

    private void tagItem(ItemStack item, StructureCategory category) {
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(
                structureCategoryKey,
                PersistentDataType.STRING,
                category.id()
        );
        item.setItemMeta(meta);
    }

    private Optional<StructureCategory> categoryFromItems(ItemStack[] items) {
        for (ItemStack item : items) {
            if (item == null || !item.hasItemMeta()) {
                continue;
            }
            String storedId = item.getItemMeta().getPersistentDataContainer().get(
                    structureCategoryKey,
                    PersistentDataType.STRING
            );
            if (storedId != null) {
                Optional<StructureCategory> category = StructureCategory.fromId(storedId);
                if (category.isPresent()) {
                    return category;
                }
            }
        }
        return Optional.empty();
    }

    private boolean isEntirelyPlayerPlaced(InventoryHolder holder) {
        if (holder instanceof Entity entity
                && entity.getPersistentDataContainer().has(
                        playerPlacedContainerEntityKey,
                        PersistentDataType.BYTE
                )) {
            return true;
        }
        if (holder instanceof DoubleChest doubleChest) {
            return isEntirelyPlayerPlaced(doubleChest.getLeftSide())
                    && isEntirelyPlayerPlaced(doubleChest.getRightSide());
        }
        if (holder instanceof BlockState blockState) {
            return placedBlockRepository.isTracked(BlockPosition.from(blockState.getBlock()));
        }
        return false;
    }

    private Optional<String> sourceKey(InventoryHolder holder) {
        if (holder instanceof DoubleChest doubleChest) {
            List<String> halves = Stream.of(
                            sourceKey(doubleChest.getLeftSide()),
                            sourceKey(doubleChest.getRightSide())
                    )
                    .flatMap(Optional::stream)
                    .sorted()
                    .toList();
            return halves.isEmpty()
                    ? Optional.empty()
                    : Optional.of("double:" + String.join("+", halves));
        }
        if (holder instanceof BlockState blockState) {
            return Optional.of(sourceKey(blockState.getBlock()));
        }
        if (holder instanceof Entity entity) {
            return Optional.of("entity:" + entity.getUniqueId());
        }
        return Optional.empty();
    }

    private String sourceKey(Block block) {
        return "block:" + block.getWorld().getUID()
                + ":" + block.getX()
                + ":" + block.getY()
                + ":" + block.getZ();
    }
}
