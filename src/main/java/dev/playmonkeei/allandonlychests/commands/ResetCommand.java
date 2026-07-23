package dev.playmonkeei.allandonlychests.commands;

import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import dev.playmonkeei.allandonlychests.storage.PlacedBlockRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resets plugin-owned challenge data after an explicit confirmation.
 */
public final class ResetCommand implements CommandExecutor, TabCompleter {

    private final ChallengeStateRepository stateRepository;
    private final PlacedBlockRepository placedBlockRepository;
    private final Logger logger;

    public ResetCommand(
            ChallengeStateRepository stateRepository,
            PlacedBlockRepository placedBlockRepository,
            Logger logger
    ) {
        this.stateRepository = stateRepository;
        this.placedBlockRepository = placedBlockRepository;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            sender.sendMessage(
                    "§cAchtung: Das löscht deinen gesamten Challenge-Fortschritt "
                            + "und alle gespeicherten Player-Placed-Positionen."
            );
            sender.sendMessage("§cDie Welt selbst wird nicht gelöscht.");
            sender.sendMessage("§eBestätige mit §f/reset confirm§e.");
            return true;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§cVerwendung: /reset confirm");
            return true;
        }

        try {
            int removedBlocks = placedBlockRepository.reset();
            stateRepository.resetProgress();
            if (sender instanceof Player player) {
                player.closeInventory();
            }
            sender.sendMessage("§aChallenge-Fortschritt vollständig zurückgesetzt.");
            sender.sendMessage(
                    "§7Entfernte Player-Placed-Positionen: " + removedBlocks
            );
            sender.sendMessage("§7Die Welt und vorhandene Blöcke wurden nicht verändert.");
        } catch (RuntimeException exception) {
            sender.sendMessage(
                    "§cDer Reset konnte nicht vollständig durchgeführt werden. "
                            + "Bitte prüfe das Serverlog."
            );
            logger.log(Level.SEVERE, "Could not reset challenge data", exception);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return "confirm".startsWith(prefix) ? List.of("confirm") : List.of();
    }
}
