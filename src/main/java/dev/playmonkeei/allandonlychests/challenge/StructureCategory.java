package dev.playmonkeei.allandonlychests.challenge;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The challenge's structure categories and their vanilla 26.2 loot tables.
 *
 * <p>The order deliberately matches the original plugin. The bonus chest is
 * not a generated structure and is therefore not part of this catalog.</p>
 */
public enum StructureCategory {

    ANCIENT_CITY(
            "ancient_city", "Antike Stadt", Material.SCULK_SHRIEKER,
            "ancient_city", "ancient_city_ice_box"
    ),
    BURIED_TREASURE(
            "buried_treasure", "Vergrabener Schatz", Material.HEART_OF_THE_SEA,
            "buried_treasure"
    ),
    DESERT_PYRAMID(
            "desert_pyramid", "Wüstenpyramide", Material.CHISELED_SANDSTONE,
            "desert_pyramid"
    ),
    END_CITY(
            "end_city", "Endsiedlung", Material.SHULKER_BOX,
            "end_city_treasure"
    ),
    NETHER_FORTRESS(
            "nether_bridge", "Netherfestung", Material.NETHER_BRICKS,
            "nether_bridge"
    ),
    IGLOO(
            "igloo", "Iglu", Material.SNOW_BLOCK,
            "igloo_chest"
    ),
    JUNGLE_TEMPLE(
            "jungle_temple", "Dschungelpyramide", Material.TRIPWIRE_HOOK,
            "jungle_temple", "jungle_temple_dispenser"
    ),
    OCEAN_RUIN(
            "underwater_ruin", "Ozeanruine", Material.SUSPICIOUS_GRAVEL,
            "underwater_ruin_big", "underwater_ruin_small"
    ),
    PILLAGER_OUTPOST(
            "pillager_outpost", "Plünderer-Außenposten", Material.CROSSBOW,
            "pillager_outpost"
    ),
    RUINED_PORTAL(
            "ruined_portal", "Ruinenportal", Material.CRYING_OBSIDIAN,
            "ruined_portal"
    ),
    SHIPWRECK(
            "shipwreck", "Schiffswrack", Material.OAK_BOAT,
            "shipwreck_map", "shipwreck_supply", "shipwreck_treasure"
    ),
    STRONGHOLD(
            "stronghold", "Festung", Material.END_PORTAL_FRAME,
            "stronghold_corridor", "stronghold_crossing", "stronghold_library"
    ),
    MINESHAFT(
            "mineshaft", "Minenstollen", Material.CHEST_MINECART,
            "abandoned_mineshaft"
    ),
    VILLAGE(
            "village", "Dorf", Material.EMERALD,
            "village/village_armorer",
            "village/village_butcher",
            "village/village_cartographer",
            "village/village_desert_house",
            "village/village_fisher",
            "village/village_fletcher",
            "village/village_mason",
            "village/village_plains_house",
            "village/village_savanna_house",
            "village/village_shepherd",
            "village/village_snowy_house",
            "village/village_taiga_house",
            "village/village_tannery",
            "village/village_temple",
            "village/village_toolsmith",
            "village/village_weaponsmith"
    ),
    WOODLAND_MANSION(
            "woodland_mansion", "Waldanwesen", Material.TOTEM_OF_UNDYING,
            "woodland_mansion"
    ),
    MONSTER_ROOM(
            "simple_dungeon", "Verlies", Material.SPAWNER,
            "simple_dungeon"
    ),
    BASTION_REMNANT(
            "bastion", "Bastionsruine", Material.GILDED_BLACKSTONE,
            "bastion_bridge", "bastion_hoglin_stable", "bastion_other", "bastion_treasure"
    ),
    TRIAL_CHAMBERS(
            "trial_chambers", "Prüfungskammern", Material.VAULT,
            "trial_chambers/corridor",
            "trial_chambers/entrance",
            "trial_chambers/intersection",
            "trial_chambers/intersection_barrel",
            "trial_chambers/reward",
            "trial_chambers/reward_common",
            "trial_chambers/reward_ominous",
            "trial_chambers/reward_ominous_common",
            "trial_chambers/reward_ominous_rare",
            "trial_chambers/reward_ominous_unique",
            "trial_chambers/reward_rare",
            "trial_chambers/reward_unique",
            "trial_chambers/supply"
    );

    private static final int EXPECTED_VANILLA_LOOT_TABLES = 55;
    private static final Map<String, StructureCategory> BY_ID;
    private static final Map<String, StructureCategory> BY_LOOT_TABLE;

    static {
        Map<String, StructureCategory> byId = new LinkedHashMap<>();
        Map<String, StructureCategory> byLootTable = new LinkedHashMap<>();

        for (StructureCategory category : values()) {
            requireUnique(byId, category.id, category, "structure id");
            for (String lootTable : category.lootTables) {
                requireUnique(byLootTable, lootTable, category, "loot table");
            }
        }

        if (byLootTable.size() != EXPECTED_VANILLA_LOOT_TABLES) {
            throw new IllegalStateException(
                    "Expected " + EXPECTED_VANILLA_LOOT_TABLES
                            + " structure loot tables, found " + byLootTable.size()
            );
        }

        BY_ID = Collections.unmodifiableMap(byId);
        BY_LOOT_TABLE = Collections.unmodifiableMap(byLootTable);
    }

    private final String id;
    private final String displayName;
    private final Material icon;
    private final Set<String> lootTables;

    StructureCategory(String id, String displayName, Material icon, String... lootTables) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.lootTables = Collections.unmodifiableSet(
                new LinkedHashSet<>(Arrays.asList(lootTables))
        );
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public Set<String> lootTables() {
        return lootTables;
    }

    public static Optional<StructureCategory> fromId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Optional<StructureCategory> fromLootTable(NamespacedKey key) {
        if (!NamespacedKey.MINECRAFT.equals(key.getNamespace())) {
            return Optional.empty();
        }
        String path = key.getKey();
        if (path.startsWith("chests/")) {
            path = path.substring("chests/".length());
        }
        return Optional.ofNullable(BY_LOOT_TABLE.get(path));
    }

    private static void requireUnique(
            Map<String, StructureCategory> destination,
            String key,
            StructureCategory category,
            String kind
    ) {
        StructureCategory previous = destination.putIfAbsent(key, category);
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate " + kind + " '" + key + "' in "
                            + previous.name() + " and " + category.name()
            );
        }
    }
}
