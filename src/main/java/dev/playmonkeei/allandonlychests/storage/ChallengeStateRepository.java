package dev.playmonkeei.allandonlychests.storage;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.challenge.StructureGoal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Stores the global challenge selection independently of a server restart.
 */
public final class ChallengeStateRepository implements AutoCloseable {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS challenge_state (
                state_key TEXT PRIMARY KEY,
                state_value TEXT NOT NULL
            )
            """;

    private static final String ACTIVE_STRUCTURE_KEY = "active_structure";
    private static final String CHALLENGE_WON_KEY = "challenge_won";
    private static final String CREATE_FOUND_GOALS_TABLE = """
            CREATE TABLE IF NOT EXISTS found_structure_goals (
                category_id TEXT NOT NULL,
                goal_key TEXT NOT NULL,
                PRIMARY KEY (category_id, goal_key)
            )
            """;
    private static final String CREATE_COMPLETED_STRUCTURES_TABLE = """
            CREATE TABLE IF NOT EXISTS completed_structures (
                category_id TEXT PRIMARY KEY
            )
            """;
    private static final String CREATE_VISITED_SOURCES_TABLE = """
            CREATE TABLE IF NOT EXISTS visited_structure_sources (
                category_id TEXT NOT NULL,
                source_key TEXT NOT NULL,
                PRIMARY KEY (category_id, source_key)
            )
            """;

    private final Path databasePath;
    private final Map<StructureCategory, Set<String>> foundGoals =
            new EnumMap<>(StructureCategory.class);
    private final Set<StructureCategory> completedStructures = new HashSet<>();
    private Connection connection;
    private StructureCategory activeStructure;
    private int openedSources;
    private boolean challengeWon;

    public ChallengeStateRepository(Path databasePath) {
        this.databasePath = databasePath;
    }

    public void open() {
        if (connection != null) {
            throw new IllegalStateException("Challenge state repository is already open");
        }

        try {
            Files.createDirectories(databasePath.getParent());
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA synchronous=FULL");
                statement.execute(CREATE_TABLE);
                statement.execute(CREATE_FOUND_GOALS_TABLE);
                statement.execute(CREATE_COMPLETED_STRUCTURES_TABLE);
                statement.execute(CREATE_VISITED_SOURCES_TABLE);
            }

            activeStructure = readActiveStructure().orElse(null);
            openedSources = readVisitedSourceCount(activeStructure);
            loadProgress();
            challengeWon = readChallengeWon();
        } catch (IOException | SQLException | ClassNotFoundException exception) {
            close();
            throw new IllegalStateException("Failed to open " + databasePath, exception);
        }
    }

    public Optional<StructureCategory> activeStructure() {
        requireOpen();
        return Optional.ofNullable(activeStructure);
    }

    public SelectionResult selectStructure(StructureCategory category) {
        requireOpen();
        if (completedStructures.contains(category)) {
            return SelectionResult.COMPLETED;
        }
        if (activeStructure == category) {
            return SelectionResult.ALREADY_ACTIVE;
        }
        if (activeStructure != null) {
            return SelectionResult.ACTIVE_STRUCTURE_INCOMPLETE;
        }

        String sql = """
                INSERT INTO challenge_state (state_key, state_value)
                VALUES (?, ?)
                ON CONFLICT(state_key) DO UPDATE SET state_value = excluded.state_value
                """;

        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, ACTIVE_STRUCTURE_KEY);
                statement.setString(2, category.id());
                statement.executeUpdate();
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM visited_structure_sources");
            }
            connection.commit();
            activeStructure = category;
            openedSources = 0;
            return SelectionResult.SELECTED;
        } catch (SQLException exception) {
            rollback();
            throw new IllegalStateException("Failed to persist the active structure", exception);
        } finally {
            restoreAutoCommit();
        }
    }

    public int openedSourceCount() {
        requireOpen();
        return openedSources;
    }

    /**
     * Counts a distinct allowed container, Vault, or Trial Spawner for the
     * currently active structure. Reopening the same source does not increase
     * the counter.
     */
    public int recordVisitedSource(StructureCategory category, String sourceKey) {
        requireOpen();
        if (activeStructure != category || completedStructures.contains(category)) {
            return openedSources;
        }

        String sql = """
                INSERT OR IGNORE INTO visited_structure_sources (category_id, source_key)
                VALUES (?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.id());
            statement.setString(2, sourceKey);
            if (statement.executeUpdate() == 1) {
                openedSources++;
            }
            return openedSources;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to persist the visited loot source", exception);
        }
    }

    public boolean isCompleted(StructureCategory category) {
        requireOpen();
        return completedStructures.contains(category);
    }

    public boolean isFound(StructureCategory category, String goalKey) {
        requireOpen();
        return foundGoals.get(category).contains(goalKey);
    }

    public int foundCount(StructureCategory category) {
        requireOpen();
        return foundGoals.get(category).size();
    }

    public boolean hasWon() {
        requireOpen();
        return challengeWon;
    }

    /**
     * Deletes all structure selection and progress data without touching the
     * world or the placed-block table.
     */
    public void resetProgress() {
        requireOpen();
        try {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM found_structure_goals");
                statement.executeUpdate("DELETE FROM completed_structures");
                statement.executeUpdate("DELETE FROM visited_structure_sources");
                statement.executeUpdate("DELETE FROM challenge_state");
            }
            connection.commit();

            activeStructure = null;
            openedSources = 0;
            challengeWon = false;
            foundGoals.values().forEach(Set::clear);
            completedStructures.clear();
        } catch (SQLException exception) {
            rollback();
            throw new IllegalStateException("Failed to reset challenge progress", exception);
        } finally {
            restoreAutoCommit();
        }
    }

    /**
     * Resets one structure without touching any other structure progress or
     * the player-placed block index. If the structure is currently active, it
     * remains selected with zero goals and zero visited sources.
     */
    public StructureResetResult resetStructureProgress(StructureCategory category) {
        requireOpen();

        int foundGoalCount = foundGoals.get(category).size();
        boolean wasCompleted = completedStructures.contains(category);
        boolean challengeReopened = challengeWon;
        int removedSources;

        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM found_structure_goals WHERE category_id = ?"
            )) {
                statement.setString(1, category.id());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM completed_structures WHERE category_id = ?"
            )) {
                statement.setString(1, category.id());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM visited_structure_sources WHERE category_id = ?"
            )) {
                statement.setString(1, category.id());
                removedSources = statement.executeUpdate();
            }
            if (challengeWon) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM challenge_state WHERE state_key = ?"
                )) {
                    statement.setString(1, CHALLENGE_WON_KEY);
                    statement.executeUpdate();
                }
            }
            connection.commit();

            foundGoals.get(category).clear();
            completedStructures.remove(category);
            if (activeStructure == category) {
                openedSources = 0;
            }
            challengeWon = false;
            return new StructureResetResult(
                    foundGoalCount,
                    removedSources,
                    wasCompleted,
                    challengeReopened,
                    activeStructure == category
            );
        } catch (SQLException exception) {
            rollback();
            throw new IllegalStateException(
                    "Failed to reset structure progress for " + category.id(),
                    exception
            );
        } finally {
            restoreAutoCommit();
        }
    }

    public ProgressUpdate recordFoundGoals(
            StructureCategory category,
            Collection<StructureGoal> matchedGoals,
            Collection<StructureGoal> allGoals
    ) {
        requireOpen();
        if (matchedGoals.isEmpty() || completedStructures.contains(category)) {
            return new ProgressUpdate(
                    List.of(),
                    false,
                    false,
                    foundCount(category),
                    allGoals.size()
            );
        }

        Set<String> existing = foundGoals.get(category);
        List<StructureGoal> newGoals = matchedGoals.stream()
                .filter(goal -> !existing.contains(goal.key()))
                .distinct()
                .toList();
        if (newGoals.isEmpty()) {
            return new ProgressUpdate(
                    List.of(),
                    false,
                    false,
                    existing.size(),
                    allGoals.size()
            );
        }

        String insertGoal = """
                INSERT OR IGNORE INTO found_structure_goals (category_id, goal_key)
                VALUES (?, ?)
                """;
        String completeStructure = """
                INSERT OR IGNORE INTO completed_structures (category_id)
                VALUES (?)
                """;
        String clearActive = "DELETE FROM challenge_state WHERE state_key = ?";
        String markChallengeWon = """
                INSERT INTO challenge_state (state_key, state_value)
                VALUES (?, ?)
                ON CONFLICT(state_key) DO UPDATE SET state_value = excluded.state_value
                """;

        int resultingCount = existing.size() + newGoals.size();
        boolean completesNow = resultingCount == allGoals.size();
        boolean completesChallengeNow = completesNow
                && !challengeWon
                && completedStructures.size() + 1 == StructureCategory.values().length;

        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(insertGoal)) {
                for (StructureGoal goal : newGoals) {
                    statement.setString(1, category.id());
                    statement.setString(2, goal.key());
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            if (completesNow) {
                try (PreparedStatement statement = connection.prepareStatement(completeStructure)) {
                    statement.setString(1, category.id());
                    statement.executeUpdate();
                }
                if (activeStructure == category) {
                    try (PreparedStatement statement = connection.prepareStatement(clearActive)) {
                        statement.setString(1, ACTIVE_STRUCTURE_KEY);
                        statement.executeUpdate();
                    }
                }
                if (completesChallengeNow) {
                    try (PreparedStatement statement =
                                 connection.prepareStatement(markChallengeWon)) {
                        statement.setString(1, CHALLENGE_WON_KEY);
                        statement.setString(2, Boolean.TRUE.toString());
                        statement.executeUpdate();
                    }
                }
            }

            connection.commit();
            existing.addAll(newGoals.stream().map(StructureGoal::key).toList());
            if (completesNow) {
                completedStructures.add(category);
                if (activeStructure == category) {
                    activeStructure = null;
                }
            }
            if (completesChallengeNow) {
                challengeWon = true;
            }
            return new ProgressUpdate(
                    List.copyOf(newGoals),
                    completesNow,
                    completesChallengeNow,
                    resultingCount,
                    allGoals.size()
            );
        } catch (SQLException exception) {
            rollback();
            throw new IllegalStateException("Failed to persist structure progress", exception);
        } finally {
            restoreAutoCommit();
        }
    }

    private Optional<StructureCategory> readActiveStructure() throws SQLException {
        String sql = "SELECT state_value FROM challenge_state WHERE state_key = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ACTIVE_STRUCTURE_KEY);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                String storedId = result.getString("state_value");
                return StructureCategory.fromId(storedId);
            }
        }
    }

    private int readVisitedSourceCount(StructureCategory category) throws SQLException {
        if (category == null) {
            return 0;
        }
        String sql = """
                SELECT COUNT(*) AS source_count
                FROM visited_structure_sources
                WHERE category_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.id());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt("source_count") : 0;
            }
        }
    }

    private boolean readChallengeWon() throws SQLException {
        String sql = "SELECT state_value FROM challenge_state WHERE state_key = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, CHALLENGE_WON_KEY);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && Boolean.parseBoolean(result.getString("state_value"));
            }
        }
    }

    private void loadProgress() throws SQLException {
        foundGoals.clear();
        for (StructureCategory category : StructureCategory.values()) {
            foundGoals.put(category, new HashSet<>());
        }
        completedStructures.clear();

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT category_id, goal_key FROM found_structure_goals"
            )) {
            while (result.next()) {
                String categoryId = result.getString("category_id");
                String goalKey = result.getString("goal_key");
                StructureCategory.fromId(categoryId)
                        .ifPresent(category -> foundGoals.get(category).add(goalKey));
            }
        }

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT category_id FROM completed_structures"
             )) {
            while (result.next()) {
                StructureCategory.fromId(result.getString("category_id"))
                        .ifPresent(completedStructures::add);
            }
        }
    }

    private void rollback() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original persistence failure.
        }
    }

    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to restore SQLite auto-commit", exception);
        }
    }

    private void requireOpen() {
        if (connection == null) {
            throw new IllegalStateException("Challenge state repository is not open");
        }
    }

    @Override
    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException ignored) {
            // SQLite will recover its WAL when the server starts again.
        } finally {
            connection = null;
            activeStructure = null;
            openedSources = 0;
            challengeWon = false;
            foundGoals.clear();
            completedStructures.clear();
        }
    }

    public enum SelectionResult {
        SELECTED,
        ALREADY_ACTIVE,
        ACTIVE_STRUCTURE_INCOMPLETE,
        COMPLETED
    }

    public record ProgressUpdate(
            List<StructureGoal> newGoals,
            boolean completedNow,
            boolean challengeCompletedNow,
            int foundCount,
            int totalCount
    ) {
    }

    public record StructureResetResult(
            int removedGoalCount,
            int removedSourceCount,
            boolean wasCompleted,
            boolean challengeReopened,
            boolean remainsActive
    ) {
    }
}
