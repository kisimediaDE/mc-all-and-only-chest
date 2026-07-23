package dev.playmonkeei.allandonlychests.storage;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

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

    private final Path databasePath;
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
            }

            activeStructure = readActiveStructure().orElse(null);
        } catch (IOException | SQLException | ClassNotFoundException exception) {
            close();
            throw new IllegalStateException("Failed to open " + databasePath, exception);
        }
    }

    public Optional<StructureCategory> activeStructure() {
        requireOpen();
        return Optional.ofNullable(activeStructure);
    }

    public void selectStructure(StructureCategory category) {
        requireOpen();

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
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to persist the active structure", exception);
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
        }
    }
}
