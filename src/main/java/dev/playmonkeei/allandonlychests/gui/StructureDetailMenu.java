package dev.playmonkeei.allandonlychests.gui;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.challenge.StructureGoalCatalog;
import dev.playmonkeei.allandonlychests.challenge.StructureGoal;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Paginated view of the unique item types obtainable from a structure.
 */
public final class StructureDetailMenu implements InventoryHolder {

    public static final int BACK_SLOT = 45;
    public static final int PREVIOUS_PAGE_SLOT = 48;
    public static final int PAGE_SLOT = 49;
    public static final int NEXT_PAGE_SLOT = 50;
    public static final int SELECT_SLOT = 53;

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 18;
    private static final int[] GOAL_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    private final StructureCategory category;
    private final List<StructureGoal> goals;
    private final ChallengeStateRepository stateRepository;
    private final int page;
    private final int pageCount;
    private final Inventory inventory;

    public StructureDetailMenu(
            StructureCategory category,
            int requestedPage,
            StructureGoalCatalog goalCatalog,
            ChallengeStateRepository stateRepository
    ) {
        this.category = category;
        this.stateRepository = stateRepository;
        goals = goalCatalog.goalsFor(category);
        pageCount = Math.max(1, (goals.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        inventory = Bukkit.createInventory(
                this,
                SIZE,
                Component.text(category.displayName(), NamedTextColor.DARK_GREEN)
        );
        render();
    }

    public StructureCategory category() {
        return category;
    }

    public int page() {
        return page;
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    public boolean hasNextPage() {
        return page + 1 < pageCount;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private void render() {
        inventory.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, goals.size());

        ItemStack filler = createNamedItem(
                Material.BLACK_STAINED_GLASS_PANE,
                Component.empty(),
                true
        );
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        for (int goalIndex = start; goalIndex < end; goalIndex++) {
            StructureGoal goal = goals.get(goalIndex);
            int displayIndex = goalIndex - start;
            int goalSlot = GOAL_SLOTS[displayIndex];
            boolean found = stateRepository.isFound(category, goal.key());
            inventory.setItem(
                    goalSlot,
                    createGoalItem(goal, found)
            );
            inventory.setItem(goalSlot + 9, createGoalStatus(found));
        }

        inventory.setItem(
                BACK_SLOT,
                createNamedItem(
                        Material.ARROW,
                        Component.text("Zurück zur Übersicht", NamedTextColor.WHITE),
                        false
                )
        );

        if (hasPreviousPage()) {
            inventory.setItem(
                    PREVIOUS_PAGE_SLOT,
                    createNamedItem(
                            Material.SPECTRAL_ARROW,
                            Component.text("Vorherige Seite", NamedTextColor.YELLOW),
                            false
                    )
            );
        }

        inventory.setItem(
                PAGE_SLOT,
                createPageIndicator()
        );

        if (hasNextPage()) {
            inventory.setItem(
                    NEXT_PAGE_SLOT,
                    createNamedItem(
                            Material.SPECTRAL_ARROW,
                            Component.text("Nächste Seite", NamedTextColor.YELLOW),
                            false
                    )
            );
        }

        boolean completed = stateRepository.isCompleted(category);
        boolean active = stateRepository.activeStructure().orElse(null) == category;
        inventory.setItem(
                SELECT_SLOT,
                createNamedItem(
                        completed
                                ? Material.LIME_STAINED_GLASS_PANE
                                : active
                                        ? Material.YELLOW_STAINED_GLASS_PANE
                                        : Material.NETHER_STAR,
                        Component.text(
                                completed
                                        ? "✓ Abgeschlossen"
                                        : active
                                                ? "▶ Aktive Struktur"
                                                : "Als aktive Struktur auswählen",
                                completed
                                        ? NamedTextColor.GREEN
                                        : active ? NamedTextColor.GOLD : NamedTextColor.AQUA
                        ),
                        false
                )
        );
    }

    private ItemStack createGoalItem(StructureGoal goal, boolean found) {
        ItemStack item = new ItemStack(goal.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(goal.displayName().color(
                found ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
        meta.lore(List.of(
                Component.text(
                        found ? "✓ Gefunden" : "Noch nicht gefunden",
                        found ? NamedTextColor.GREEN : NamedTextColor.RED
                )
        ));
        meta.setEnchantmentGlintOverride(found);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGoalStatus(boolean found) {
        ItemStack item = new ItemStack(
                found ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE
        );
        ItemMeta meta = item.getItemMeta();
        if (found) {
            meta.displayName(Component.text("✓ Gefunden", NamedTextColor.GREEN));
        } else {
            meta.setHideTooltip(true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPageIndicator() {
        int found = stateRepository.foundCount(category);
        ItemStack item = createNamedItem(
                Material.BOOK,
                Component.text(
                        "Seite " + (page + 1) + "/" + pageCount,
                        NamedTextColor.GOLD
                ),
                false
        );
        ItemMeta meta = item.getItemMeta();
        meta.lore(List.of(
                Component.text(goals.size() + " unterschiedliche Items", NamedTextColor.GRAY),
                Component.text(
                        "Fortschritt: " + found + "/" + goals.size(),
                        found == goals.size() ? NamedTextColor.GREEN : NamedTextColor.RED
                )
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNamedItem(
            Material material,
            Component displayName,
            boolean hideTooltip
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(displayName);
        meta.setHideTooltip(hideTooltip);
        item.setItemMeta(meta);
        return item;
    }
}
