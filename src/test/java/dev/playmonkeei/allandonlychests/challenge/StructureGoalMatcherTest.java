package dev.playmonkeei.allandonlychests.challenge;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureGoalMatcherTest {

    @Test
    void materialMatcherRequiresTheExactMaterial() {
        StructureGoal goal = StructureGoal.material(
                Material.DIAMOND,
                Component.text("Diamant")
        );

        assertTrue(goal.matcher().matches(item(Material.DIAMOND)));
        assertFalse(goal.matcher().matches(item(Material.EMERALD)));
    }

    @Test
    void enchantedMatcherRequiresMaterialAndEnchantment() {
        StructureGoal goal = StructureGoal.enchanted(
                Material.DIAMOND_AXE,
                "enchanted:diamond_axe",
                "Verzauberte Diamantaxt"
        );

        assertTrue(goal.matcher().matches(enchantedItem(Material.DIAMOND_AXE)));
        assertFalse(goal.matcher().matches(item(Material.DIAMOND_AXE)));
        assertFalse(goal.matcher().matches(enchantedItem(Material.GOLDEN_AXE)));
    }

    @Test
    void potionMatcherAcceptsNormalLongAndStrongVariants() {
        StructureGoal goal = StructureGoal.potion(
                Material.POTION,
                "regeneration",
                "Trank der Regeneration"
        );

        assertTrue(goal.matcher().matches(
                potionItem(Material.POTION, PotionType.REGENERATION)
        ));
        assertTrue(goal.matcher().matches(
                potionItem(Material.POTION, PotionType.LONG_REGENERATION)
        ));
        assertTrue(goal.matcher().matches(
                potionItem(Material.POTION, PotionType.STRONG_REGENERATION)
        ));
    }

    @Test
    void potionMatcherRejectsWrongEffectMaterialAndMissingBaseType() {
        StructureGoal goal = StructureGoal.potion(
                Material.POTION,
                "regeneration",
                "Trank der Regeneration"
        );

        assertFalse(goal.matcher().matches(
                potionItem(Material.POTION, PotionType.STRENGTH)
        ));
        assertFalse(goal.matcher().matches(
                potionItem(Material.TIPPED_ARROW, PotionType.REGENERATION)
        ));
        assertFalse(goal.matcher().matches(potionItem(Material.POTION, null)));
        assertFalse(goal.matcher().matches(item(Material.POTION)));
    }

    static ItemStack item(Material material) {
        return new TestItemStack(material, false, null);
    }

    static ItemStack enchantedItem(Material material) {
        return new TestItemStack(material, true, null);
    }

    static ItemStack potionItem(Material material, PotionType potionType) {
        return new TestItemStack(material, false, potionMeta(potionType));
    }

    private static PotionMeta potionMeta(PotionType potionType) {
        return (PotionMeta) Proxy.newProxyInstance(
                PotionMeta.class.getClassLoader(),
                new Class<?>[]{PotionMeta.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getBasePotionType" -> potionType;
                    case "hasBasePotionType" -> potionType != null;
                    case "toString" -> "PotionMeta[" + potionType + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0F;
        }
        return 0.0D;
    }

    private static final class TestItemStack extends ItemStack {

        private final Material material;
        private final Map<Enchantment, Integer> enchantments;
        private final ItemMeta itemMeta;

        private TestItemStack(
                Material material,
                boolean enchanted,
                ItemMeta itemMeta
        ) {
            this.material = material;
            this.enchantments = enchanted
                    ? Collections.singletonMap(null, 1)
                    : Map.of();
            this.itemMeta = itemMeta;
        }

        @Override
        public Material getType() {
            return material;
        }

        @Override
        public Map<Enchantment, Integer> getEnchantments() {
            return enchantments;
        }

        @Override
        public ItemMeta getItemMeta() {
            return itemMeta;
        }
    }
}
