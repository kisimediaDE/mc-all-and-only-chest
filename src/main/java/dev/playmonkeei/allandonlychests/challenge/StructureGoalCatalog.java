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

/**
 * Version-aware item goals generated from the official Vanilla 26.2 loot data.
 */
public final class StructureGoalCatalog {

    private final Map<StructureCategory, List<Material>> goals;

    private StructureGoalCatalog(Map<StructureCategory, List<Material>> goals) {
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

        Map<StructureCategory, List<Material>> goals = new EnumMap<>(StructureCategory.class);
        for (StructureCategory category : StructureCategory.values()) {
            List<Material> materials = new ArrayList<>();
            for (String materialName : configuration.getStringList(category.id())) {
                Material material = Material.matchMaterial("minecraft:" + materialName);
                if (material == null || !material.isItem()) {
                    logger.info(
                            "Skipping structure goal unavailable in this Minecraft version: "
                                    + materialName + " (" + category.id() + ")"
                    );
                    continue;
                }
                materials.add(material);
            }

            if (materials.isEmpty()) {
                throw new IllegalStateException("No item goals found for " + category.id());
            }
            goals.put(category, List.copyOf(materials));
        }

        return new StructureGoalCatalog(goals);
    }

    public List<Material> goalsFor(StructureCategory category) {
        return goals.get(category);
    }
}
