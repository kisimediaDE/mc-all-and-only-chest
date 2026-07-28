package dev.playmonkeei.allandonlychests.challenge;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureGoalCatalogTest {

    private static final Logger LOGGER =
            Logger.getLogger(StructureGoalCatalogTest.class.getName());

    @Test
    void minecraft262KeepsBounceGoal() {
        List<String> configured = configuredGoals(StructureCategory.MINESHAFT);
        Map<String, Material> available = StructureGoalCatalog.availableMaterials(
                configured,
                StructureCategory.MINESHAFT,
                LOGGER,
                ignored -> Material.STONE,
                ignored -> true
        );

        assertEquals(22, available.size());
        assertTrue(available.containsKey("music_disc_bounce"));
    }

    @Test
    void minecraft261FiltersUnavailableBounceGoal() {
        List<String> configured = configuredGoals(StructureCategory.MINESHAFT);
        Map<String, Material> available = StructureGoalCatalog.availableMaterials(
                configured,
                StructureCategory.MINESHAFT,
                LOGGER,
                materialName -> materialName.equals("music_disc_bounce")
                        ? null
                        : Material.STONE,
                ignored -> true
        );

        assertEquals(21, available.size());
        assertFalse(available.containsKey("music_disc_bounce"));
    }

    @Test
    void configuredGoalNamesAreUniqueInEveryCategory() {
        for (StructureCategory category : StructureCategory.values()) {
            List<String> configured = configuredGoals(category);
            Set<String> distinct = new HashSet<>(configured);

            assertFalse(configured.isEmpty(), category.id());
            assertEquals(configured.size(), distinct.size(), category.id());
        }
    }

    private List<String> configuredGoals(StructureCategory category) {
        InputStream input = StructureGoalCatalogTest.class.getClassLoader()
                .getResourceAsStream("structure-goals.yml");
        assertNotNull(input);

        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(
                    new InputStreamReader(input, StandardCharsets.UTF_8)
            );
        } catch (Exception exception) {
            throw new AssertionError("Could not load structure-goals.yml", exception);
        }
        return configuration.getStringList(category.id());
    }
}
