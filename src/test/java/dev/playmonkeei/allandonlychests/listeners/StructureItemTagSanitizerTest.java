package dev.playmonkeei.allandonlychests.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StructureItemTagSanitizerTest {

    private static final NamespacedKey STRUCTURE_KEY =
            new NamespacedKey("allandonlychests", "structure_category");
    private static final NamespacedKey OTHER_PLUGIN_KEY =
            new NamespacedKey("anotherplugin", "custom_data");

    private final StructureItemTagSanitizer sanitizer =
            new StructureItemTagSanitizer(STRUCTURE_KEY);

    @Test
    void removesOnlyTheStructureMarkerAndReappliesExistingMeta() {
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        ItemMeta meta = mock(ItemMeta.class);
        ItemStack item = item(meta);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.has(STRUCTURE_KEY)).thenReturn(true);

        assertTrue(sanitizer.removeFrom(item));

        verify(data).remove(STRUCTURE_KEY);
        verify(data, never()).remove(OTHER_PLUGIN_KEY);
        verify(item).setItemMeta(meta);
    }

    @Test
    void leavesCompletelyUnmarkedItemsUntouched() {
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        ItemMeta meta = mock(ItemMeta.class);
        ItemStack item = item(meta);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.has(STRUCTURE_KEY)).thenReturn(false);

        assertFalse(sanitizer.removeFrom(item));

        verify(data, never()).remove(STRUCTURE_KEY);
        verify(item, never()).setItemMeta(meta);
    }

    @Test
    void recursivelyMigratesTaggedItemsInsideBundles() {
        PersistentDataContainer childData = mock(PersistentDataContainer.class);
        ItemMeta childMeta = mock(ItemMeta.class);
        ItemStack child = item(childMeta);
        when(childMeta.getPersistentDataContainer()).thenReturn(childData);
        when(childData.has(STRUCTURE_KEY)).thenReturn(true);

        PersistentDataContainer bundleData = mock(PersistentDataContainer.class);
        BundleMeta bundleMeta = mock(BundleMeta.class);
        ItemStack bundle = item(Material.BUNDLE, bundleMeta);
        when(bundleMeta.getPersistentDataContainer()).thenReturn(bundleData);
        when(bundleData.has(STRUCTURE_KEY)).thenReturn(false);
        when(bundleMeta.hasItems()).thenReturn(true);
        when(bundleMeta.getItems()).thenReturn(List.of(child));

        assertTrue(sanitizer.removeFrom(bundle));

        verify(childData).remove(STRUCTURE_KEY);
        verify(child).setItemMeta(childMeta);
        verify(bundleMeta).setItems(List.of(child));
        verify(bundle).setItemMeta(bundleMeta);
    }

    @Test
    void recursivelyMigratesTaggedItemsInsidePortableContainers() {
        PersistentDataContainer childData = mock(PersistentDataContainer.class);
        ItemMeta childMeta = mock(ItemMeta.class);
        ItemStack child = item(childMeta);
        when(childMeta.getPersistentDataContainer()).thenReturn(childData);
        when(childData.has(STRUCTURE_KEY)).thenReturn(true);

        Inventory containerInventory = mock(Inventory.class);
        ItemStack[] containerContents = {child, null};
        when(containerInventory.getContents()).thenReturn(containerContents);

        Container container = mock(Container.class);
        when(container.getInventory()).thenReturn(containerInventory);

        PersistentDataContainer shulkerData = mock(PersistentDataContainer.class);
        BlockStateMeta shulkerMeta = mock(BlockStateMeta.class);
        ItemStack shulker = item(Material.SHULKER_BOX, shulkerMeta);
        when(shulkerMeta.getPersistentDataContainer()).thenReturn(shulkerData);
        when(shulkerData.has(STRUCTURE_KEY)).thenReturn(false);
        when(shulkerMeta.getBlockState()).thenReturn(container);

        assertTrue(sanitizer.removeFrom(shulker));

        verify(childData).remove(STRUCTURE_KEY);
        verify(child).setItemMeta(childMeta);
        verify(containerInventory).setContents(containerContents);
        verify(shulkerMeta).setBlockState(container);
        verify(shulker).setItemMeta(shulkerMeta);
    }

    @Test
    void leavesPortableContainersUntouchedWhenNestedItemsHaveNoMarker() {
        PersistentDataContainer childData = mock(PersistentDataContainer.class);
        ItemMeta childMeta = mock(ItemMeta.class);
        ItemStack child = item(childMeta);
        when(childMeta.getPersistentDataContainer()).thenReturn(childData);
        when(childData.has(STRUCTURE_KEY)).thenReturn(false);

        Inventory containerInventory = mock(Inventory.class);
        ItemStack[] containerContents = {child};
        when(containerInventory.getContents()).thenReturn(containerContents);

        Container container = mock(Container.class);
        when(container.getInventory()).thenReturn(containerInventory);

        PersistentDataContainer shulkerData = mock(PersistentDataContainer.class);
        BlockStateMeta shulkerMeta = mock(BlockStateMeta.class);
        ItemStack shulker = item(Material.SHULKER_BOX, shulkerMeta);
        when(shulkerMeta.getPersistentDataContainer()).thenReturn(shulkerData);
        when(shulkerData.has(STRUCTURE_KEY)).thenReturn(false);
        when(shulkerMeta.getBlockState()).thenReturn(container);

        assertFalse(sanitizer.removeFrom(shulker));

        verify(childData, never()).remove(STRUCTURE_KEY);
        verify(containerInventory, never()).setContents(containerContents);
        verify(shulkerMeta, never()).setBlockState(container);
        verify(shulker, never()).setItemMeta(shulkerMeta);
    }

    @Test
    void writesBackOnlyChangedInventorySlots() {
        ItemMeta taggedMeta = mock(ItemMeta.class);
        PersistentDataContainer taggedData = mock(PersistentDataContainer.class);
        ItemStack tagged = item(taggedMeta);
        when(taggedMeta.getPersistentDataContainer()).thenReturn(taggedData);
        when(taggedData.has(STRUCTURE_KEY)).thenReturn(true);

        ItemMeta plainMeta = mock(ItemMeta.class);
        PersistentDataContainer plainData = mock(PersistentDataContainer.class);
        ItemStack plain = item(plainMeta);
        when(plainMeta.getPersistentDataContainer()).thenReturn(plainData);
        when(plainData.has(STRUCTURE_KEY)).thenReturn(false);

        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(new ItemStack[]{tagged, plain, null});

        sanitizer.removeFrom(inventory);

        verify(inventory).setItem(0, tagged);
        verify(inventory, never()).setItem(1, plain);
    }

    private ItemStack item(ItemMeta meta) {
        return item(Material.DIAMOND, meta);
    }

    private ItemStack item(Material material, ItemMeta meta) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        return item;
    }
}
