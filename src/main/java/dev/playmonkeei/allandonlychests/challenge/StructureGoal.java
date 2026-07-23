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
        return new StructureGoal(
                material.getKey().getKey(),
                material,
                Component.translatable(material.translationKey()),
                item -> item.getType() == material
        );
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

        String type = potionMeta.getBasePotionType().getKey().getKey().toLowerCase(Locale.ROOT);
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
