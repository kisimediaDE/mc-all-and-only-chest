package dev.playmonkeei.allandonlychests;

import dev.playmonkeei.allandonlychests.challenge.StructureGoalCatalog;
import dev.playmonkeei.allandonlychests.commands.StructureCompleteCommand;
import dev.playmonkeei.allandonlychests.commands.ResetCommand;
import dev.playmonkeei.allandonlychests.listeners.ChallengeBlockListener;
import dev.playmonkeei.allandonlychests.listeners.ExplosionDropListener;
import dev.playmonkeei.allandonlychests.listeners.MobDropListener;
import dev.playmonkeei.allandonlychests.listeners.MovingBlockListener;
import dev.playmonkeei.allandonlychests.listeners.StructureSelectionListener;
import dev.playmonkeei.allandonlychests.listeners.StructureLootListener;
import dev.playmonkeei.allandonlychests.gui.StructureSelectionMenu;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import dev.playmonkeei.allandonlychests.storage.PlacedBlockRepository;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for the All and Only Chests challenge plugin.
 */
public final class AllAndOnlyChestsPlugin extends JavaPlugin {

    private PlacedBlockRepository placedBlockRepository;
    private ChallengeStateRepository challengeStateRepository;
    private StructureGoalCatalog structureGoalCatalog;

    @Override
    public void onEnable() {
        var databasePath = getDataFolder().toPath().resolve("data").resolve("challenge.db");
        placedBlockRepository = new PlacedBlockRepository(databasePath);
        challengeStateRepository = new ChallengeStateRepository(databasePath);

        try {
            structureGoalCatalog = StructureGoalCatalog.load(this, getLogger());
            placedBlockRepository.open();
            challengeStateRepository.open();
        } catch (RuntimeException exception) {
            getLogger().severe("Could not initialize challenge storage: " + exception.getMessage());
            challengeStateRepository.close();
            placedBlockRepository.close();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(
                new ChallengeBlockListener(placedBlockRepository, getLogger()),
                this
        );
        getServer().getPluginManager().registerEvents(
                new ExplosionDropListener(placedBlockRepository, getLogger()),
                this
        );
        getServer().getPluginManager().registerEvents(new MobDropListener(), this);
        getServer().getPluginManager().registerEvents(
                new MovingBlockListener(
                        placedBlockRepository,
                        getLogger(),
                        new NamespacedKey(this, "player_placed_falling_block")
                ),
                this
        );
        getServer().getPluginManager().registerEvents(
                new StructureSelectionListener(
                        challengeStateRepository,
                        structureGoalCatalog,
                        getLogger(),
                        this
                ),
                this
        );
        getServer().getPluginManager().registerEvents(
                new StructureLootListener(
                        this,
                        challengeStateRepository,
                        placedBlockRepository,
                        structureGoalCatalog,
                        getLogger()
                ),
                this
        );

        StructureCompleteCommand structureCompleteCommand =
                new StructureCompleteCommand(challengeStateRepository, structureGoalCatalog);
        getCommand("structurecomplete").setExecutor(structureCompleteCommand);
        getCommand("structurecomplete").setTabCompleter(structureCompleteCommand);

        ResetCommand resetCommand = new ResetCommand(
                challengeStateRepository,
                placedBlockRepository,
                getLogger()
        );
        getCommand("reset").setExecutor(resetCommand);
        getCommand("reset").setTabCompleter(resetCommand);

        getLogger().info(
                "All and Only Chests enabled with "
                        + placedBlockRepository.size()
                        + " tracked player-placed blocks and active structure "
                        + challengeStateRepository.activeStructure()
                                .map(category -> "'" + category.id() + "'")
                                .orElse("none")
                        + "."
        );
    }

    @Override
    public void onDisable() {
        if (challengeStateRepository != null) {
            challengeStateRepository.close();
        }
        if (placedBlockRepository != null) {
            placedBlockRepository.close();
        }
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!command.getName().equalsIgnoreCase("gui")) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur im Spiel verwendet werden.");
            return true;
        }

        player.openInventory(
                new StructureSelectionMenu(
                        this,
                        challengeStateRepository,
                        structureGoalCatalog
                ).getInventory()
        );
        return true;
    }
}
