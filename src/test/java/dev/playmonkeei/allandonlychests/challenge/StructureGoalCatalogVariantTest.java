package dev.playmonkeei.allandonlychests.challenge;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StructureGoalCatalogVariantTest {

    private static final Logger LOGGER =
            Logger.getLogger(StructureGoalCatalogVariantTest.class.getName());

    private static StructureGoalCatalog catalog;

    @BeforeAll
    static void loadCatalog() {
        InputStream input = StructureGoalCatalogVariantTest.class
                .getClassLoader()
                .getResourceAsStream("structure-goals.yml");
        assertNotNull(input);

        catalog = StructureGoalCatalog.load(
                input,
                LOGGER,
                materialName -> Material.getMaterial(
                        materialName.toUpperCase(Locale.ROOT)
                ),
                ignored -> true,
                material -> StructureGoal.material(
                        material,
                        Component.text(StructureGoal.materialKey(material))
                )
        );
    }

    @Test
    void trialChamberSeparatesNormalAndEnchantedDiamondAxes() {
        assertEquals(
                Set.of("diamond_axe"),
                matchingKeys(
                        StructureCategory.TRIAL_CHAMBERS,
                        StructureGoalMatcherTest.item(Material.DIAMOND_AXE)
                )
        );
        assertEquals(
                Set.of("enchanted:diamond_axe"),
                matchingKeys(
                        StructureCategory.TRIAL_CHAMBERS,
                        StructureGoalMatcherTest.enchantedItem(Material.DIAMOND_AXE)
                )
        );
    }

    @Test
    void trialChamberSeparatesPotionAndTippedArrowEffects() {
        assertEquals(
                Set.of("potion:regeneration"),
                matchingKeys(
                        StructureCategory.TRIAL_CHAMBERS,
                        StructureGoalMatcherTest.potionItem(
                                Material.POTION,
                                PotionType.STRONG_REGENERATION
                        )
                )
        );
        assertEquals(
                Set.of("potion:strength"),
                matchingKeys(
                        StructureCategory.TRIAL_CHAMBERS,
                        StructureGoalMatcherTest.potionItem(
                                Material.POTION,
                                PotionType.LONG_STRENGTH
                        )
                )
        );
        assertEquals(
                Set.of("potion:swiftness"),
                matchingKeys(
                        StructureCategory.TRIAL_CHAMBERS,
                        StructureGoalMatcherTest.potionItem(
                                Material.POTION,
                                PotionType.SWIFTNESS
                        )
                )
        );
        assertEquals(
                Set.of("tipped_arrow:poison"),
                matchingKeys(
                        StructureCategory.TRIAL_CHAMBERS,
                        StructureGoalMatcherTest.potionItem(
                                Material.TIPPED_ARROW,
                                PotionType.LONG_POISON
                        )
                )
        );
        assertEquals(
                Set.of("tipped_arrow:slowness"),
                matchingKeys(
                        StructureCategory.TRIAL_CHAMBERS,
                        StructureGoalMatcherTest.potionItem(
                                Material.TIPPED_ARROW,
                                PotionType.STRONG_SLOWNESS
                        )
                )
        );
    }

    @Test
    void bastionSeparatesEveryConfiguredEnchantedEquipmentVariant() {
        List<Material> materials = List.of(
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

        for (Material material : materials) {
            String materialKey = StructureGoal.materialKey(material);
            Set<String> expectedPlainKeys = material == Material.DIAMOND_PICKAXE
                    ? Set.of()
                    : Set.of(materialKey);
            assertEquals(
                    expectedPlainKeys,
                    matchingKeys(
                            StructureCategory.BASTION_REMNANT,
                            StructureGoalMatcherTest.item(material)
                    ),
                    "plain " + materialKey
            );
            assertEquals(
                    Set.of("enchanted:" + materialKey),
                    matchingKeys(
                            StructureCategory.BASTION_REMNANT,
                            StructureGoalMatcherTest.enchantedItem(material)
                    ),
                    "enchanted " + materialKey
            );
        }
    }

    @Test
    void bastionKeepsBaseGoalForEnchantedItemsWithoutSpecialVariant() {
        assertEquals(
                Set.of("golden_axe"),
                matchingKeys(
                        StructureCategory.BASTION_REMNANT,
                        StructureGoalMatcherTest.enchantedItem(Material.GOLDEN_AXE)
                )
        );
    }

    @Test
    void unrelatedItemsDoNotMatchTrialOrBastionGoals() {
        assertEquals(
                Set.of(),
                matchingKeys(
                        StructureCategory.TRIAL_CHAMBERS,
                        StructureGoalMatcherTest.item(Material.NETHER_STAR)
                )
        );
        assertEquals(
                Set.of(),
                matchingKeys(
                        StructureCategory.BASTION_REMNANT,
                        StructureGoalMatcherTest.item(Material.NETHER_STAR)
                )
        );
    }

    private Set<String> matchingKeys(
            StructureCategory category,
            org.bukkit.inventory.ItemStack item
    ) {
        return catalog.match(category, item).stream()
                .map(StructureGoal::key)
                .collect(Collectors.toSet());
    }
}
