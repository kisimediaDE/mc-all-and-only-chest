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

    private final Path databasePath;
    private final Map<StructureCategory, Set<String>> foundGoals =
            new EnumMap<>(StructureCategory.class);
    private final Set<StructureCategory> completedStructures = new HashSet<>();
    private Connection connection;
    private StructureCategory activeStructure;

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
            }

            activeStructure = readActiveStructure().orElse(null);
            loadProgress();
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

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ACTIVE_STRUCTURE_KEY);
            statement.setString(2, category.id());
            statement.executeUpdate();
            activeStructure = category;
            return SelectionResult.SELECTED;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to persist the active structure", exception);
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
                statement.executeUpdate("DELETE FROM challenge_state");
            }
            connection.commit();

            activeStructure = null;
            foundGoals.values().forEach(Set::clear);
            completedStructures.clear();
        } catch (SQLException exception) {
            rollback();
            throw new IllegalStateException("Failed to reset challenge progress", exception);
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
            return new ProgressUpdate(List.of(), false, foundCount(category), allGoals.size());
        }

        Set<String> existing = foundGoals.get(category);
        List<StructureGoal> newGoals = matchedGoals.stream()
                .filter(goal -> !existing.contains(goal.key()))
                .distinct()
                .toList();
        if (newGoals.isEmpty()) {
            return new ProgressUpdate(List.of(), false, existing.size(), allGoals.size());
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

        int resultingCount = existing.size() + newGoals.size();
        boolean completesNow = resultingCount == allGoals.size();

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
            }

            connection.commit();
            existing.addAll(newGoals.stream().map(StructureGoal::key).toList());
            if (completesNow) {
                completedStructures.add(category);
                if (activeStructure == category) {
                    activeStructure = null;
                }
            }
            return new ProgressUpdate(
                    List.copyOf(newGoals),
                    completesNow,
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
            int foundCount,
            int totalCount
    ) {
    }
}
