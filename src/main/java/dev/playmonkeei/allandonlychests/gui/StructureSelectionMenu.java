package dev.playmonkeei.allandonlychests.gui;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Inventory view for the 18 verified vanilla structure categories.
 */
public final class StructureSelectionMenu implements InventoryHolder {

    private static final int SIZE = 45;
    private static final int[] CATEGORY_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    private final ChallengeStateRepository stateRepository;
    private final NamespacedKey structureIdKey;
    private final Inventory inventory;

    public StructureSelectionMenu(Plugin plugin, ChallengeStateRepository stateRepository) {
        this.stateRepository = stateRepository;
        structureIdKey = new NamespacedKey(plugin, "structure_id");
        inventory = Bukkit.createInventory(
                this,
                SIZE,
                Component.text("Struktur auswählen", NamedTextColor.DARK_GREEN)
        );
        render();
    }

    public NamespacedKey structureIdKey() {
        return structureIdKey;
    }

    public void render() {
        inventory.clear();
        StructureCategory active = stateRepository.activeStructure().orElse(null);
        StructureCategory[] categories = StructureCategory.values();

        for (int index = 0; index < categories.length; index++) {
            StructureCategory category = categories[index];
            int categorySlot = CATEGORY_SLOTS[index];
            boolean selected = category == active;
            inventory.setItem(categorySlot, createCategoryItem(category, selected));
            inventory.setItem(categorySlot + 9, createStatusItem(selected));
        }

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setHideTooltip(true);
        filler.setItemMeta(fillerMeta);
        for (int slot = 36; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private ItemStack createCategoryItem(StructureCategory category, boolean selected) {
        ItemStack item = new ItemStack(category.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(
                category.displayName(),
                selected ? NamedTextColor.GOLD : NamedTextColor.WHITE
        ));
        meta.lore(List.of(
                Component.text(
                        selected ? "Aktuell aktiv" : "Details öffnen",
                        selected ? NamedTextColor.GOLD : NamedTextColor.GRAY
                ),
                Component.text(
                        category.lootTables().size() + " Vanilla-Loot-Tabelle(n)",
                        NamedTextColor.DARK_GRAY
                )
        ));
        meta.setEnchantmentGlintOverride(selected);
        meta.getPersistentDataContainer().set(
                structureIdKey,
                PersistentDataType.STRING,
                category.id()
        );
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatusItem(boolean selected) {
        ItemStack item = new ItemStack(
                selected ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE
        );
        ItemMeta meta = item.getItemMeta();
        if (selected) {
            meta.displayName(Component.text("▶ Aktiv", NamedTextColor.GOLD));
        } else {
            meta.setHideTooltip(true);
        }
        item.setItemMeta(meta);
        return item;
    }
}
