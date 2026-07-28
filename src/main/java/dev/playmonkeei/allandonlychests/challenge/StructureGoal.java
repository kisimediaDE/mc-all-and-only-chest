package dev.playmonkeei.allandonlychests.challenge;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.Locale;

/**
 * A single completion goal. Most goals match a material; Trial Chambers also
 * distinguish a few potion/effect and enchantment variants like the original.
 */
public record StructureGoal(
        String key,
        Material icon,
        Component displayName,
        GoalMatcher matcher
) {

    public static StructureGoal material(Material material) {
        return material(material, materialDisplayName(material));
    }

    static StructureGoal material(Material material, Component displayName) {
        return new StructureGoal(
                materialKey(material),
                material,
                displayName,
                item -> item.getType() == material
        );
    }

    private static Component materialDisplayName(Material material) {
        String materialKey = materialKey(material);
        if (!materialKey.startsWith("music_disc_")) {
            return Component.translatable(material.translationKey());
        }

        String track = materialKey.substring("music_disc_".length());
        String displayTrack = switch (track) {
            case "13" -> "C418 – 13";
            case "cat" -> "C418 – cat";
            default -> track.replace('_', ' ');
        };
        return Component.text("Schallplatte: " + displayTrack);
    }

    static String materialKey(Material material) {
        return material.name().toLowerCase(Locale.ROOT);
    }

    public static StructureGoal enchanted(
            Material material,
            String key,
            String germanDisplayName
    ) {
        return new StructureGoal(
                key,
                material,
                Component.text(germanDisplayName),
                item -> item.getType() == material && !item.getEnchantments().isEmpty()
        );
    }

    public static StructureGoal potion(
            Material material,
            String baseType,
            String germanDisplayName
    ) {
        return new StructureGoal(
                material.getKey().getKey() + ":" + baseType,
                material,
                Component.text(germanDisplayName),
                item -> item.getType() == material
                        && item.getItemMeta() instanceof PotionMeta potionMeta
                        && normalizedPotionType(potionMeta).equals(baseType)
        );
    }

    private static String normalizedPotionType(PotionMeta potionMeta) {
        if (potionMeta.getBasePotionType() == null) {
            return "";
        }

        String type = potionMeta.getBasePotionType().name().toLowerCase(Locale.ROOT);
        if (type.startsWith("long_")) {
            return type.substring("long_".length());
        }
        if (type.startsWith("strong_")) {
            return type.substring("strong_".length());
        }
        return type;
    }

    @FunctionalInterface
    public interface GoalMatcher {
        boolean matches(ItemStack item);
    }
}
