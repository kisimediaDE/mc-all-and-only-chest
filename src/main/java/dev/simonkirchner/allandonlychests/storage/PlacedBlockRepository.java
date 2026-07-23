package dev.simonkirchner.allandonlychests.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Durable SQLite repository with an in-memory index for event-time lookups.
 */
public final class PlacedBlockRepository implements AutoCloseable {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS placed_blocks (
                world_uuid TEXT NOT NULL,
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                z INTEGER NOT NULL,
                PRIMARY KEY (world_uuid, x, y, z)
            )
            """;

    private static final String INSERT_BLOCK = """
            INSERT OR IGNORE INTO placed_blocks (world_uuid, x, y, z)
            VALUES (?, ?, ?, ?)
            """;

    private static final String DELETE_BLOCK = """
            DELETE FROM placed_blocks
            WHERE world_uuid = ? AND x = ? AND y = ? AND z = ?
            """;

    private final Path databasePath;
    private final Set<BlockPosition> placedBlocks = new HashSet<>();
    private Connection connection;

    public PlacedBlockRepository(Path databasePath) {
        this.databasePath = databasePath;
    }

    public void open() {
        if (connection != null) {
            throw new IllegalStateException("Placed block repository is already open");
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

            loadIndex();
        } catch (IOException | SQLException | ClassNotFoundException exception) {
            close();
            throw new IllegalStateException("Failed to open " + databasePath, exception);
        }
    }

    public int size() {
        return placedBlocks.size();
    }

    public void trackAll(Collection<BlockPosition> positions) {
        if (positions.isEmpty()) {
            return;
        }

        requireOpen();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT_BLOCK)) {
                for (BlockPosition position : positions) {
                    bind(statement, position);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
            placedBlocks.addAll(positions);
        } catch (SQLException exception) {
            rollback();
            throw new IllegalStateException("Failed to persist placed blocks", exception);
        } finally {
            restoreAutoCommit();
        }
    }

    /**
     * Removes a tracked position and returns whether the block was player-placed.
     */
    public boolean untrack(BlockPosition position) {
        requireOpen();
        if (!placedBlocks.contains(position)) {
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement(DELETE_BLOCK)) {
            bind(statement, position);
            statement.executeUpdate();
            placedBlocks.remove(position);
            return true;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to remove placed block", exception);
        }
    }

    /**
     * Removes all tracked positions in one transaction.
     */
    public int untrackAll(Collection<BlockPosition> positions) {
        requireOpen();
        List<BlockPosition> trackedPositions = positions.stream()
                .filter(placedBlocks::contains)
                .distinct()
                .toList();

        if (trackedPositions.isEmpty()) {
            return 0;
        }

        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(DELETE_BLOCK)) {
                for (BlockPosition position : trackedPositions) {
                    bind(statement, position);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
            placedBlocks.removeAll(trackedPositions);
            return trackedPositions.size();
        } catch (SQLException exception) {
            rollback();
            throw new IllegalStateException("Failed to remove placed blocks", exception);
        } finally {
            restoreAutoCommit();
        }
    }

    private void loadIndex() throws SQLException {
        placedBlocks.clear();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT world_uuid, x, y, z FROM placed_blocks")) {
            while (result.next()) {
                placedBlocks.add(new BlockPosition(
                        UUID.fromString(result.getString("world_uuid")),
                        result.getInt("x"),
                        result.getInt("y"),
                        result.getInt("z")
                ));
            }
        }
    }

    private static void bind(PreparedStatement statement, BlockPosition position) throws SQLException {
        statement.setString(1, position.worldId().toString());
        statement.setInt(2, position.x());
        statement.setInt(3, position.y());
        statement.setInt(4, position.z());
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
            throw new IllegalStateException("Placed block repository is not open");
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
            // The server is already shutting down; SQLite will recover its WAL on next open.
        } finally {
            connection = null;
            placedBlocks.clear();
        }
    }
}
