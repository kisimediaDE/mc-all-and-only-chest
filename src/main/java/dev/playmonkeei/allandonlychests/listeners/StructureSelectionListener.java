package dev.playmonkeei.allandonlychests.listeners;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.gui.StructureSelectionMenu;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
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

    public StructureSelectionListener(
            ChallengeStateRepository stateRepository,
            Logger logger,
            Plugin plugin
    ) {
        this.stateRepository = stateRepository;
        this.logger = logger;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof StructureSelectionMenu menu)) {
            return;
        }

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

        try {
            stateRepository.selectStructure(category);
            player.sendMessage("§aAktive Struktur: §f" + category.displayName());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                menu.render();
                player.updateInventory();
            });
        } catch (RuntimeException exception) {
            player.sendMessage("§cDie Auswahl konnte nicht gespeichert werden.");
            logger.log(Level.SEVERE, "Could not persist the selected structure", exception);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof StructureSelectionMenu) {
            event.setCancelled(true);
        }
    }
}
