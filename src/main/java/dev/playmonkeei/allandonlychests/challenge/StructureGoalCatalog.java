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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Version-aware item goals generated from the official Vanilla 26.2 loot data.
 */
public final class StructureGoalCatalog {

    private static final Set<Material> BASTION_ENCHANTED_MATERIALS =
            EnumSet.of(
                    Material.CROSSBOW,
                    Material.GOLDEN_HELMET,
                    Material.GOLDEN_CHESTPLATE,
                    Material.GOLDEN_LEGGINGS,
                    Material.GOLDEN_BOOTS,
                    Material.DIAMOND_PICKAXE,
                    Material.DIAMOND_SHOVEL,
                    Material.DIAMOND_SPEAR,
                    Material.DIAMOND_SWORD,
                    Material.DIAMOND_HELMET,
                    Material.DIAMOND_CHESTPLATE,
                    Material.DIAMOND_LEGGINGS,
                    Material.DIAMOND_BOOTS
            );

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
            if (category == StructureCategory.BASTION_REMNANT) {
                categoryGoals.addAll(bastionEnchantedVariantGoals());
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
        int bastionGoalCount = goals.get(StructureCategory.BASTION_REMNANT).size();
        if (bastionGoalCount != 66) {
            throw new IllegalStateException(
                    "Expected 66 Bastion goals for Minecraft 26.1/26.2, found "
                            + bastionGoalCount
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
        if (category == StructureCategory.BASTION_REMNANT
                && BASTION_ENCHANTED_MATERIALS.contains(item.getType())
                && !item.getEnchantments().isEmpty()) {
            matches.removeIf(goal -> goal.key().equals(item.getType().getKey().getKey()));
        }
        return List.copyOf(matches);
    }

    private static List<StructureGoal> bastionEnchantedVariantGoals() {
        return List.of(
                StructureGoal.enchanted(
                        Material.CROSSBOW,
                        "enchanted:crossbow",
                        "Verzauberte Armbrust"
                ),
                StructureGoal.enchanted(
                        Material.GOLDEN_HELMET,
                        "enchanted:golden_helmet",
                        "Verzauberter Goldhelm"
                ),
                StructureGoal.enchanted(
                        Material.GOLDEN_CHESTPLATE,
                        "enchanted:golden_chestplate",
                        "Verzauberter Goldharnisch"
                ),
                StructureGoal.enchanted(
                        Material.GOLDEN_LEGGINGS,
                        "enchanted:golden_leggings",
                        "Verzauberter Goldbeinschutz"
                ),
                StructureGoal.enchanted(
                        Material.GOLDEN_BOOTS,
                        "enchanted:golden_boots",
                        "Verzauberte Goldstiefel"
                ),
                StructureGoal.enchanted(
                        Material.DIAMOND_PICKAXE,
                        "enchanted:diamond_pickaxe",
                        "Verzauberte Diamantspitzhacke"
                ),
                StructureGoal.enchanted(
                        Material.DIAMOND_SHOVEL,
                        "enchanted:diamond_shovel",
                        "Verzauberte Diamantschaufel"
                ),
                StructureGoal.enchanted(
                        Material.DIAMOND_SPEAR,
                        "enchanted:diamond_spear",
                        "Verzauberter Diamantspeer"
                ),
                StructureGoal.enchanted(
                        Material.DIAMOND_SWORD,
                        "enchanted:diamond_sword",
                        "Verzaubertes Diamantschwert"
                ),
                StructureGoal.enchanted(
                        Material.DIAMOND_HELMET,
                        "enchanted:diamond_helmet",
                        "Verzauberter Diamanthelm"
                ),
                StructureGoal.enchanted(
                        Material.DIAMOND_CHESTPLATE,
                        "enchanted:diamond_chestplate",
                        "Verzauberter Diamantharnisch"
                ),
                StructureGoal.enchanted(
                        Material.DIAMOND_LEGGINGS,
                        "enchanted:diamond_leggings",
                        "Verzauberter Diamantbeinschutz"
                ),
                StructureGoal.enchanted(
                        Material.DIAMOND_BOOTS,
                        "enchanted:diamond_boots",
                        "Verzauberte Diamantstiefel"
                )
        );
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
