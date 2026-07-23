package dev.playmonkeei.allandonlychests.challenge;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Version-aware item goals generated from the official Vanilla 26.2 loot data.
 */
public final class StructureGoalCatalog {

    private final Map<StructureCategory, List<StructureGoal>> goals;

    private StructureGoalCatalog(Map<StructureCategory, List<StructureGoal>> goals) {
        this.goals = Collections.unmodifiableMap(goals);
    }

    public static StructureGoalCatalog load(Plugin plugin, Logger logger) {
        YamlConfiguration configuration = new YamlConfiguration();

        try (InputStream input = plugin.getResource("structure-goals.yml")) {
            if (input == null) {
                throw new IllegalStateException("Missing structure-goals.yml");
            }
            configuration.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException exception) {
            throw new IllegalStateException("Could not load structure goal catalog", exception);
        }

        Map<StructureCategory, List<StructureGoal>> goals = new EnumMap<>(StructureCategory.class);
        for (StructureCategory category : StructureCategory.values()) {
            List<StructureGoal> categoryGoals = new ArrayList<>();
            for (String materialName : configuration.getStringList(category.id())) {
                Material material = Material.matchMaterial("minecraft:" + materialName);
                if (material == null || !material.isItem()) {
                    logger.info(
                            "Skipping structure goal unavailable in this Minecraft version: "
                                    + materialName + " (" + category.id() + ")"
                    );
                    continue;
                }
                categoryGoals.add(StructureGoal.material(material));
            }

            if (category == StructureCategory.TRIAL_CHAMBERS) {
                categoryGoals.addAll(trialChamberVariantGoals());
            }

            if (categoryGoals.isEmpty()) {
                throw new IllegalStateException("No item goals found for " + category.id());
            }

            long distinctKeys = categoryGoals.stream()
                    .map(StructureGoal::key)
                    .distinct()
                    .count();
            if (distinctKeys != categoryGoals.size()) {
                throw new IllegalStateException("Duplicate goal key in " + category.id());
            }
            goals.put(category, List.copyOf(categoryGoals));
        }

        int trialGoalCount = goals.get(StructureCategory.TRIAL_CHAMBERS).size();
        if (trialGoalCount != 64) {
            throw new IllegalStateException(
                    "Expected 64 Trial Chambers goals like the original plugin, found "
                            + trialGoalCount
            );
        }
        return new StructureGoalCatalog(goals);
    }

    public List<StructureGoal> goalsFor(StructureCategory category) {
        return goals.get(category);
    }

    public List<StructureGoal> match(
            StructureCategory category,
            org.bukkit.inventory.ItemStack item
    ) {
        List<StructureGoal> matches = goals.get(category).stream()
                .filter(goal -> goal.matcher().matches(item))
                .collect(Collectors.toCollection(ArrayList::new));

        if (category == StructureCategory.TRIAL_CHAMBERS
                && item.getType() == Material.DIAMOND_AXE
                && !item.getEnchantments().isEmpty()) {
            matches.removeIf(goal -> goal.key().equals("diamond_axe"));
        }
        return List.copyOf(matches);
    }

    private static List<StructureGoal> trialChamberVariantGoals() {
        return List.of(
                StructureGoal.enchanted(
                        Material.DIAMOND_AXE,
                        "enchanted:diamond_axe",
                        "Verzauberte Diamantaxt"
                ),
                StructureGoal.potion(
                        Material.POTION,
                        "regeneration",
                        "Trank der Regeneration"
                ),
                StructureGoal.potion(
                        Material.POTION,
                        "strength",
                        "Trank der Stärke"
                ),
                StructureGoal.potion(
                        Material.POTION,
                        "swiftness",
                        "Trank der Schnelligkeit"
                ),
                StructureGoal.potion(
                        Material.TIPPED_ARROW,
                        "poison",
                        "Pfeil der Vergiftung"
                ),
                StructureGoal.potion(
                        Material.TIPPED_ARROW,
                        "slowness",
                        "Pfeil der Langsamkeit"
                )
        );
    }
}
