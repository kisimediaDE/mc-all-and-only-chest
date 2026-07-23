package dev.simonkirchner.allandonlychests;

import dev.simonkirchner.allandonlychests.listeners.ChallengeBlockListener;
import dev.simonkirchner.allandonlychests.storage.PlacedBlockRepository;
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

    @Override
    public void onEnable() {
        placedBlockRepository = new PlacedBlockRepository(
                getDataFolder().toPath().resolve("data").resolve("challenge.db")
        );

        try {
            placedBlockRepository.open();
        } catch (RuntimeException exception) {
            getLogger().severe("Could not initialize challenge storage: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(
                new ChallengeBlockListener(placedBlockRepository, getLogger()),
                this
        );

        getLogger().info(
                "All and Only Chests enabled with "
                        + placedBlockRepository.size()
                        + " tracked player-placed blocks."
        );
    }

    @Override
    public void onDisable() {
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

        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl kann nur im Spiel verwendet werden.");
            return true;
        }

        sender.sendMessage("§aAll and Only Chests läuft! Die Strukturübersicht folgt im nächsten Schritt.");
        return true;
    }
}
