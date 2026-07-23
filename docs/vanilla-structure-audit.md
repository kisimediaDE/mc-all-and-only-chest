# Vanilla structure audit for Minecraft 26.2

Audit date: 2026-07-23

This catalog was derived from the data bundled in Mojang's official server
JARs. It does not rely on a wiki or on release-note wording.

## Compared releases

| Release | Official server JAR SHA-1 | Chest loot tables |
| --- | --- | ---: |
| 1.21.4 (original plugin baseline) | `4707d00eb834b446575d89a61a11b5d548d8c001` | 56 |
| 26.1 | `3872a7f07a1a595e651aef8b058dfc2bb3772f46` | 56 |
| 26.1.1 | `49c8195703ad0ba4f0a4efbccfd85a4a8ca57431` | 56 |
| 26.1.2 | `97ccd4c0ed3f81bbb7bfacddd1090b0c56f9bc51` | 56 |
| 26.2 | `823e2250d24b3ddac457a60c92a6a941943fcd6a` | 56 |

The sorted contents of `data/minecraft/loot_table/chests/` are byte-for-byte
identical as filename lists in all five releases. `spawn_bonus_chest` is not a
generated structure and is excluded from the challenge. The remaining 55 loot
tables map to the following 18 structure categories.

## Binding category list

1. Ancient City (`ancient_city`)
2. Buried Treasure (`buried_treasure`)
3. Desert Pyramid (`desert_pyramid`)
4. End City (`end_city`)
5. Nether Fortress (`nether_bridge`)
6. Igloo (`igloo`)
7. Jungle Temple (`jungle_temple`)
8. Ocean Ruin (`underwater_ruin`)
9. Pillager Outpost (`pillager_outpost`)
10. Ruined Portal (`ruined_portal`)
11. Shipwreck (`shipwreck`)
12. Stronghold (`stronghold`)
13. Mineshaft (`mineshaft`)
14. Village (`village`)
15. Woodland Mansion (`woodland_mansion`)
16. Monster Room (`simple_dungeon`)
17. Bastion Remnant (`bastion`)
18. Trial Chambers (`trial_chambers`)

This is the same category set as the original plugin's 1.21.4 source.
Consequently, the number of chest-bearing structure categories added in 26.1,
26.1.1, 26.1.2, or 26.2 is **zero**.

## Explicit exclusions and edge cases

- `spawn_bonus_chest` is a world-start option, not a structure.
- Ocean Monuments and Swamp Huts have no Vanilla structure chest loot table.
- Copper Chests introduced after 1.21.4 have block loot tables. They are
  craftable/gameplay containers and are not generated structure categories.
- The ten new 26.2 templates under
  `data/minecraft/structure/spring/sulfur_spring_*.nbt` are terrain features.
  Their NBT contains no chest, barrel, or loot-table reference.
- `jungle_temple_dispenser` remains associated with the Jungle Temple category
  for complete Vanilla loot-table classification, but dispenser contents are
  not treated as a chest opening by the challenge.
- Trial Chambers contain ordinary containers in addition to vault and trial
  spawner rewards. Their complete Vanilla loot-table family remains one
  challenge category, matching the original plugin.

The executable mapping is maintained in
`StructureCategory.java`. Its class initialization rejects duplicate mappings
and asserts that exactly 55 structure loot tables are present.
