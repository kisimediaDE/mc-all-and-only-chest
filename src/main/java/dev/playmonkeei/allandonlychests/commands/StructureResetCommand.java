package dev.playmonkeei.allandonlychests.commands;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import dev.playmonkeei.allandonlychests.ui.ChallengeSidebar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resets the persisted progress of one structure after explicit confirmation.
 */
public final class StructureResetCommand implements CommandExecutor, TabCompleter {

    private final ChallengeStateRepository stateRepository;
    private final ChallengeSidebar sidebar;
    private final Logger logger;

    public StructureResetCommand(
            ChallengeStateRepository stateRepository,
            ChallengeSidebar sidebar,
            Logger logger
    ) {
        this.stateRepository = stateRepository;
        this.sidebar = sidebar;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length < 1 || args.length > 2) {
            sendUsage(sender);
            return true;
        }

        StructureCategory category = StructureCategory.fromId(
                args[0].toLowerCase(Locale.ROOT)
        ).orElse(null);
        if (category == null) {
            sender.sendMessage("§cUnbekannte Struktur-Kategorie: §f" + args[0]);
            sender.sendMessage("§7Nutze die Tab-Vervollständigung für gültige Kategorien.");
            return true;
        }

        if (args.length == 1) {
            sender.sendMessage(
                    "§cAchtung: Der Fortschritt von §f" + category.displayName()
                            + " §cwird gelöscht."
            );
            sender.sendMessage(
                    "§7Aktuell: " + stateRepository.foundCount(category)
                            + " gefundene Ziele, abgeschlossen: "
                            + (stateRepository.isCompleted(category) ? "ja" : "nein")
            );
            sender.sendMessage(
                    "§eBestätige mit §f/structurereset " + category.id() + " confirm§e."
            );
            sender.sendMessage("§7Die Welt und vorhandene Kisten werden nicht zurückgesetzt.");
            return true;
        }

        if (!args[1].equalsIgnoreCase("confirm")) {
            sendUsage(sender);
            return true;
        }

        try {
            ChallengeStateRepository.StructureResetResult result =
                    stateRepository.resetStructureProgress(category);
            sidebar.refreshAll();
            if (sender instanceof Player player) {
                player.closeInventory();
            }

            sender.sendMessage(
                    "§aStruktur zurückgesetzt: §f" + category.displayName()
            );
            sender.sendMessage(
                    "§7Entfernte Ziele: " + result.removedGoalCount()
                            + ", entfernte Kisten/Quellen: " + result.removedSourceCount()
            );
            if (result.challengeReopened()) {
                sender.sendMessage("§eDer Gesamtsieg wurde wieder geöffnet.");
            }
            if (result.remainsActive()) {
                sender.sendMessage(
                        "§eDie Struktur bleibt aktiv und beginnt wieder bei 0 Zielen."
                );
            } else {
                sender.sendMessage(
                        "§7Die Struktur kann jetzt über /gui erneut ausgewählt werden."
                );
            }
            sender.sendMessage("§7Die Welt und vorhandene Kisten wurden nicht verändert.");
        } catch (RuntimeException exception) {
            sender.sendMessage(
                    "§cDie Struktur konnte nicht sicher zurückgesetzt werden. "
                            + "Bitte prüfe das Serverlog."
            );
            logger.log(
                    Level.SEVERE,
                    "Could not reset structure progress for " + category.id(),
                    exception
            );
        }
        return true;
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage("§cVerwendung: /structurereset <Kategorie> confirm");
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Arrays.stream(StructureCategory.values())
                    .map(StructureCategory::id)
                    .filter(id -> id.startsWith(prefix))
                    .toList();
        }
        if (args.length == 2
                && StructureCategory.fromId(args[0].toLowerCase(Locale.ROOT)).isPresent()) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return "confirm".startsWith(prefix) ? List.of("confirm") : List.of();
        }
        return List.of();
    }
}
