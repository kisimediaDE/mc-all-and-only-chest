package dev.playmonkeei.allandonlychests.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Removes the legacy structure marker without changing any other item data. */
final class StructureItemTagSanitizer {

    private final NamespacedKey structureCategoryKey;

    StructureItemTagSanitizer(NamespacedKey structureCategoryKey) {
        this.structureCategoryKey = structureCategoryKey;
    }

    void removeFrom(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (removeFrom(item)) {
                inventory.setItem(slot, item);
            }
        }
    }

    /**
     * Removes only this plugin's legacy structure marker. Vanilla components,
     * enchantments, potion data, names and data belonging to other plugins are
     * deliberately preserved. Bundle and portable-container contents are
     * migrated recursively so Beta 2 items become stack-compatible as well.
     */
    boolean removeFrom(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        boolean changed = false;
        if (meta.getPersistentDataContainer().has(structureCategoryKey)) {
            meta.getPersistentDataContainer().remove(structureCategoryKey);
            changed = true;
        }

        if (meta instanceof BundleMeta bundleMeta && bundleMeta.hasItems()) {
            List<ItemStack> bundleItems = new ArrayList<>(bundleMeta.getItems());
            boolean bundleChanged = false;
            for (ItemStack bundleItem : bundleItems) {
                bundleChanged |= removeFrom(bundleItem);
            }
            if (bundleChanged) {
                bundleMeta.setItems(bundleItems);
                changed = true;
            }
        }

        if (meta instanceof BlockStateMeta blockStateMeta
                && blockStateMeta.getBlockState() instanceof Container container) {
            ItemStack[] containerItems = container.getInventory().getContents();
            boolean containerChanged = false;
            for (ItemStack containerItem : containerItems) {
                containerChanged |= removeFrom(containerItem);
            }
            if (containerChanged) {
                container.getInventory().setContents(containerItems);
                blockStateMeta.setBlockState(container);
                changed = true;
            }
        }

        if (changed) {
            item.setItemMeta(meta);
        }
        return changed;
    }
}
