package dev.playmonkeei.allandonlychests.storage;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.challenge.StructureGoal;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeStateRepositoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void selectionProgressAndDistinctSourcesSurviveRestart() {
        Path database = temporaryDirectory.resolve("challenge.db");
        List<StructureGoal> goals = List.of(
                goal("diamond"),
                goal("emerald")
        );

        try (ChallengeStateRepository repository = open(database)) {
            assertEquals(
                    ChallengeStateRepository.SelectionResult.SELECTED,
                    repository.selectStructure(StructureCategory.BURIED_TREASURE)
            );
            assertEquals(
                    ChallengeStateRepository.SelectionResult.ALREADY_ACTIVE,
                    repository.selectStructure(StructureCategory.BURIED_TREASURE)
            );
            assertEquals(
                    ChallengeStateRepository.SelectionResult.ACTIVE_STRUCTURE_INCOMPLETE,
                    repository.selectStructure(StructureCategory.IGLOO)
            );

            assertEquals(
                    1,
                    repository.recordVisitedSource(
                            StructureCategory.BURIED_TREASURE,
                            "world:10:20:30"
                    )
            );
            assertEquals(
                    1,
                    repository.recordVisitedSource(
                            StructureCategory.BURIED_TREASURE,
                            "world:10:20:30"
                    )
            );

            ChallengeStateRepository.ProgressUpdate update = repository.recordFoundGoals(
                    StructureCategory.BURIED_TREASURE,
                    List.of(goals.getFirst()),
                    goals
            );
            assertEquals(1, update.foundCount());
            assertFalse(update.completedNow());
        }

        try (ChallengeStateRepository repository = open(database)) {
            assertEquals(
                    StructureCategory.BURIED_TREASURE,
                    repository.activeStructure().orElseThrow()
            );
            assertEquals(1, repository.openedSourceCount());
            assertEquals(1, repository.foundCount(StructureCategory.BURIED_TREASURE));
            assertTrue(repository.isFound(StructureCategory.BURIED_TREASURE, "diamond"));
        }
    }

    @Test
    void completingStructureClearsSelectionAndBlocksReselection() {
        Path database = temporaryDirectory.resolve("challenge.db");
        StructureGoal goal = goal("diamond");

        try (ChallengeStateRepository repository = open(database)) {
            repository.selectStructure(StructureCategory.END_CITY);
            ChallengeStateRepository.ProgressUpdate update = repository.recordFoundGoals(
                    StructureCategory.END_CITY,
                    List.of(goal),
                    List.of(goal)
            );

            assertTrue(update.completedNow());
            assertFalse(update.challengeCompletedNow());
            assertTrue(repository.activeStructure().isEmpty());
            assertTrue(repository.isCompleted(StructureCategory.END_CITY));
            assertEquals(
                    ChallengeStateRepository.SelectionResult.COMPLETED,
                    repository.selectStructure(StructureCategory.END_CITY)
            );
        }
    }

    @Test
    void structureResetOnlyClearsTargetAndKeepsItActive() {
        Path database = temporaryDirectory.resolve("challenge.db");
        StructureGoal goal = goal("emerald");

        try (ChallengeStateRepository repository = open(database)) {
            repository.selectStructure(StructureCategory.IGLOO);
            repository.recordVisitedSource(StructureCategory.IGLOO, "world:1:2:3");
            repository.recordFoundGoals(
                    StructureCategory.IGLOO,
                    List.of(goal),
                    List.of(goal, goal("apple"))
            );

            ChallengeStateRepository.StructureResetResult result =
                    repository.resetStructureProgress(StructureCategory.IGLOO);

            assertEquals(1, result.removedGoalCount());
            assertEquals(1, result.removedSourceCount());
            assertFalse(result.wasCompleted());
            assertTrue(result.remainsActive());
            assertEquals(0, repository.foundCount(StructureCategory.IGLOO));
            assertEquals(0, repository.openedSourceCount());
            assertEquals(
                    StructureCategory.IGLOO,
                    repository.activeStructure().orElseThrow()
            );
        }
    }

    @Test
    void globalResetClearsAllChallengeState() {
        Path database = temporaryDirectory.resolve("challenge.db");
        StructureGoal goal = goal("heart_of_the_sea");

        try (ChallengeStateRepository repository = open(database)) {
            repository.selectStructure(StructureCategory.BURIED_TREASURE);
            repository.recordVisitedSource(
                    StructureCategory.BURIED_TREASURE,
                    "world:4:5:6"
            );
            repository.recordFoundGoals(
                    StructureCategory.BURIED_TREASURE,
                    List.of(goal),
                    List.of(goal, goal("diamond"))
            );

            repository.resetProgress();

            assertTrue(repository.activeStructure().isEmpty());
            assertEquals(0, repository.openedSourceCount());
            assertEquals(0, repository.foundCount(StructureCategory.BURIED_TREASURE));
            assertFalse(repository.isCompleted(StructureCategory.BURIED_TREASURE));
            assertFalse(repository.hasWon());
        }

        try (ChallengeStateRepository repository = open(database)) {
            assertTrue(repository.activeStructure().isEmpty());
            assertEquals(0, repository.openedSourceCount());
            assertEquals(0, repository.foundCount(StructureCategory.BURIED_TREASURE));
        }
    }

    @Test
    void lastStructureCompletesChallengeAndTargetedResetReopensIt() {
        Path database = temporaryDirectory.resolve("challenge.db");
        StructureGoal goal = goal("only_goal");

        try (ChallengeStateRepository repository = open(database)) {
            for (StructureCategory category : StructureCategory.values()) {
                assertEquals(
                        ChallengeStateRepository.SelectionResult.SELECTED,
                        repository.selectStructure(category)
                );
                ChallengeStateRepository.ProgressUpdate update =
                        repository.recordFoundGoals(
                                category,
                                List.of(goal),
                                List.of(goal)
                        );

                assertTrue(update.completedNow());
                assertEquals(
                        category == StructureCategory.TRIAL_CHAMBERS,
                        update.challengeCompletedNow()
                );
            }

            assertTrue(repository.hasWon());
            assertTrue(repository.activeStructure().isEmpty());

            ChallengeStateRepository.StructureResetResult reset =
                    repository.resetStructureProgress(StructureCategory.END_CITY);

            assertTrue(reset.wasCompleted());
            assertTrue(reset.challengeReopened());
            assertFalse(reset.remainsActive());
            assertFalse(repository.hasWon());
            assertFalse(repository.isCompleted(StructureCategory.END_CITY));
            assertEquals(
                    ChallengeStateRepository.SelectionResult.SELECTED,
                    repository.selectStructure(StructureCategory.END_CITY)
            );
        }

        try (ChallengeStateRepository repository = open(database)) {
            assertFalse(repository.hasWon());
            assertEquals(
                    StructureCategory.END_CITY,
                    repository.activeStructure().orElseThrow()
            );
        }
    }

    @Test
    void hudModeSurvivesRestartAndChallengeReset() {
        Path database = temporaryDirectory.resolve("challenge.db");
        UUID playerId = UUID.randomUUID();

        try (ChallengeStateRepository repository = open(database)) {
            assertTrue(repository.hudMode(playerId).isEmpty());
            repository.setHudMode(playerId, "bossbar");
        }

        try (ChallengeStateRepository repository = open(database)) {
            assertEquals("bossbar", repository.hudMode(playerId).orElseThrow());

            repository.resetProgress();

            assertEquals("bossbar", repository.hudMode(playerId).orElseThrow());
            repository.setHudMode(playerId, "off");
        }

        try (ChallengeStateRepository repository = open(database)) {
            assertEquals("off", repository.hudMode(playerId).orElseThrow());
        }
    }

    @Test
    void catalogReductionKeepsRegularProgressAndRemovesObsoleteGoals() {
        Path database = temporaryDirectory.resolve("challenge.db");
        StructureGoal emerald = goal("emerald");
        StructureGoal apple = goal("apple");
        StructureGoal obsoleteWool = goal("white_wool");

        try (ChallengeStateRepository repository = open(database)) {
            repository.selectStructure(StructureCategory.VILLAGE);
            repository.recordFoundGoals(
                    StructureCategory.VILLAGE,
                    List.of(emerald, obsoleteWool),
                    List.of(emerald, apple, obsoleteWool)
            );
        }

        try (ChallengeStateRepository repository = open(database)) {
            ChallengeStateRepository.ProgressReconciliation reconciliation =
                    repository.reconcileProgress(catalogWithVillageGoals(emerald, apple));

            assertEquals(1, reconciliation.removedGoalCount());
            assertTrue(reconciliation.completedStructures().isEmpty());
            assertFalse(reconciliation.challengeCompleted());
            assertEquals(1, repository.foundCount(StructureCategory.VILLAGE));
            assertTrue(repository.isFound(StructureCategory.VILLAGE, "emerald"));
            assertFalse(repository.isFound(StructureCategory.VILLAGE, "white_wool"));
            assertEquals(
                    StructureCategory.VILLAGE,
                    repository.activeStructure().orElseThrow()
            );
        }

        try (ChallengeStateRepository repository = open(database)) {
            assertEquals(1, repository.foundCount(StructureCategory.VILLAGE));
            assertFalse(repository.isFound(StructureCategory.VILLAGE, "white_wool"));
        }
    }

    @Test
    void catalogReconciliationKeepsFourteenRegularVillageGoalsUnchanged() {
        Path database = temporaryDirectory.resolve("challenge.db");
        List<StructureGoal> foundGoals = IntStream.rangeClosed(1, 14)
                .mapToObj(index -> goal("regular_goal_" + index))
                .toList();
        List<StructureGoal> currentGoals = IntStream.rangeClosed(1, 15)
                .mapToObj(index -> goal("regular_goal_" + index))
                .toList();

        try (ChallengeStateRepository repository = open(database)) {
            repository.selectStructure(StructureCategory.VILLAGE);
            repository.recordFoundGoals(
                    StructureCategory.VILLAGE,
                    foundGoals,
                    currentGoals
            );
        }

        try (ChallengeStateRepository repository = open(database)) {
            ChallengeStateRepository.ProgressReconciliation reconciliation =
                    repository.reconcileProgress(catalogWithVillageGoals(
                            currentGoals.toArray(StructureGoal[]::new)
                    ));

            assertEquals(0, reconciliation.removedGoalCount());
            assertTrue(reconciliation.completedStructures().isEmpty());
            assertFalse(reconciliation.challengeCompleted());
            assertEquals(14, repository.foundCount(StructureCategory.VILLAGE));
            assertFalse(repository.isCompleted(StructureCategory.VILLAGE));
            assertEquals(
                    StructureCategory.VILLAGE,
                    repository.activeStructure().orElseThrow()
            );
        }
    }

    @Test
    void catalogReductionCompletesStructureFromExistingRegularProgress() {
        Path database = temporaryDirectory.resolve("challenge.db");
        StructureGoal emerald = goal("emerald");
        StructureGoal apple = goal("apple");
        StructureGoal obsoleteWool = goal("white_wool");

        try (ChallengeStateRepository repository = open(database)) {
            repository.selectStructure(StructureCategory.VILLAGE);
            repository.recordFoundGoals(
                    StructureCategory.VILLAGE,
                    List.of(emerald, apple),
                    List.of(emerald, apple, obsoleteWool)
            );
        }

        try (ChallengeStateRepository repository = open(database)) {
            ChallengeStateRepository.ProgressReconciliation reconciliation =
                    repository.reconcileProgress(catalogWithVillageGoals(emerald, apple));

            assertEquals(0, reconciliation.removedGoalCount());
            assertEquals(
                    Set.of(StructureCategory.VILLAGE),
                    reconciliation.completedStructures()
            );
            assertFalse(reconciliation.challengeCompleted());
            assertTrue(repository.isCompleted(StructureCategory.VILLAGE));
            assertTrue(repository.activeStructure().isEmpty());
            assertEquals(2, repository.foundCount(StructureCategory.VILLAGE));
        }

        try (ChallengeStateRepository repository = open(database)) {
            assertTrue(repository.isCompleted(StructureCategory.VILLAGE));
            assertTrue(repository.activeStructure().isEmpty());
        }
    }

    private ChallengeStateRepository open(Path database) {
        ChallengeStateRepository repository = new ChallengeStateRepository(database);
        repository.open();
        return repository;
    }

    private StructureGoal goal(String key) {
        return new StructureGoal(
                key,
                Material.STONE,
                Component.text(key),
                item -> false
        );
    }

    private Map<StructureCategory, List<StructureGoal>> catalogWithVillageGoals(
            StructureGoal... villageGoals
    ) {
        Map<StructureCategory, List<StructureGoal>> catalog =
                new EnumMap<>(StructureCategory.class);
        for (StructureCategory category : StructureCategory.values()) {
            catalog.put(category, category == StructureCategory.VILLAGE
                    ? List.of(villageGoals)
                    : List.of(goal(category.id() + "_goal")));
        }
        return catalog;
    }
}
