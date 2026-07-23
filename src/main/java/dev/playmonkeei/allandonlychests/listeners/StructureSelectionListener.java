package dev.playmonkeei.allandonlychests.listeners;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.challenge.StructureGoalCatalog;
import dev.playmonkeei.allandonlychests.gui.StructureDetailMenu;
import dev.playmonkeei.allandonlychests.gui.StructureSelectionMenu;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import dev.playmonkeei.allandonlychests.ui.ChallengeSidebar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles selections without trusting item names or inventory titles.
 */
public final class StructureSelectionListener implements Listener {

    private final ChallengeStateRepository stateRepository;
    private final Logger logger;
    private final Plugin plugin;
    private final StructureGoalCatalog goalCatalog;
    private final ChallengeSidebar sidebar;

    public StructureSelectionListener(
            ChallengeStateRepository stateRepository,
            StructureGoalCatalog goalCatalog,
            ChallengeSidebar sidebar,
            Logger logger,
            Plugin plugin
    ) {
        this.stateRepository = stateRepository;
        this.goalCatalog = goalCatalog;
        this.sidebar = sidebar;
        this.logger = logger;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof StructureSelectionMenu menu) {
            handleOverviewClick(event, menu);
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof StructureDetailMenu menu) {
            handleDetailClick(event, menu);
        }
    }

    private void handleOverviewClick(InventoryClickEvent event, StructureSelectionMenu menu) {
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        String structureId = clicked.getItemMeta().getPersistentDataContainer().get(
                menu.structureIdKey(),
                PersistentDataType.STRING
        );
        if (structureId == null) {
            return;
        }

        StructureCategory category = StructureCategory.fromId(structureId).orElse(null);
        if (category == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> openDetail(player, category, 0)
        );
    }

    private void handleDetailClick(InventoryClickEvent event, StructureDetailMenu menu) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot == StructureDetailMenu.BACK_SLOT) {
            plugin.getServer().getScheduler().runTask(
                    plugin,
                    () -> player.openInventory(
                            new StructureSelectionMenu(
                                    plugin,
                                    stateRepository,
                                    goalCatalog
                            ).getInventory()
                    )
            );
            return;
        }
        if (slot == StructureDetailMenu.PREVIOUS_PAGE_SLOT && menu.hasPreviousPage()) {
            plugin.getServer().getScheduler().runTask(
                    plugin,
                    () -> openDetail(player, menu.category(), menu.page() - 1)
            );
            return;
        }
        if (slot == StructureDetailMenu.NEXT_PAGE_SLOT && menu.hasNextPage()) {
            plugin.getServer().getScheduler().runTask(
                    plugin,
                    () -> openDetail(player, menu.category(), menu.page() + 1)
            );
            return;
        }
        if (slot != StructureDetailMenu.SELECT_SLOT) {
            return;
        }

        try {
            ChallengeStateRepository.SelectionResult result =
                    stateRepository.selectStructure(menu.category());
            sidebar.refreshAll();
            switch (result) {
                case SELECTED -> player.sendMessage(
                        "§aAktive Struktur: §f" + menu.category().displayName()
                );
                case ALREADY_ACTIVE -> player.sendMessage(
                        "§eDiese Struktur ist bereits aktiv."
                );
                case ACTIVE_STRUCTURE_INCOMPLETE -> player.sendMessage(
                        "§cSchließe zuerst die aktuell aktive Struktur ab."
                );
                case COMPLETED -> player.sendMessage(
                        "§aDiese Struktur ist bereits abgeschlossen."
                );
            }
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    openDetail(player, menu.category(), menu.page())
            );
        } catch (RuntimeException exception) {
            player.sendMessage("§cDie Auswahl konnte nicht gespeichert werden.");
            logger.log(Level.SEVERE, "Could not persist the selected structure", exception);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof StructureSelectionMenu
                || event.getView().getTopInventory().getHolder() instanceof StructureDetailMenu) {
            event.setCancelled(true);
        }
    }

    private void openDetail(Player player, StructureCategory category, int page) {
        player.openInventory(
                new StructureDetailMenu(
                        category,
                        page,
                        goalCatalog,
                        stateRepository
                ).getInventory()
        );
    }
}
