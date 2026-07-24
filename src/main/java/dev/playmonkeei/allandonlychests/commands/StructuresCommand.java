package dev.playmonkeei.allandonlychests.commands;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.challenge.StructureGoalCatalog;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Displays the complete challenge state in the same order as the GUI.
 */
public final class StructuresCommand implements CommandExecutor {

    private final ChallengeStateRepository stateRepository;
    private final StructureGoalCatalog goalCatalog;

    public StructuresCommand(
            ChallengeStateRepository stateRepository,
            StructureGoalCatalog goalCatalog
    ) {
        this.stateRepository = stateRepository;
        this.goalCatalog = goalCatalog;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length != 0) {
            sender.sendMessage("§cVerwendung: /structures");
            return true;
        }

        List<StructureCategory> completed = Arrays.stream(StructureCategory.values())
                .filter(stateRepository::isCompleted)
                .toList();
        Optional<StructureCategory> active = stateRepository.activeStructure();
        List<StructureCategory> open = Arrays.stream(StructureCategory.values())
                .filter(category -> !stateRepository.isCompleted(category))
                .filter(category -> active.isEmpty() || category != active.get())
                .toList();

        sender.sendMessage("§6§lAll & Only Chests");
        sender.sendMessage(
                "§eGesamtfortschritt: §f" + completed.size()
                        + "§7/§f" + StructureCategory.values().length
        );
        if (active.isPresent()) {
            StructureCategory category = active.get();
            sender.sendMessage(
                    "§6Aktiv: §f" + category.displayName()
                            + " §7(Items "
                            + stateRepository.foundCount(category)
                            + "/"
                            + goalCatalog.goalsFor(category).size()
                            + ", Kisten "
                            + stateRepository.openedSourceCount()
                            + ")"
            );
        } else {
            sender.sendMessage("§6Aktiv: §7Keine Struktur ausgewählt");
        }
        sender.sendMessage(
                "§aAbgeschlossen (" + completed.size() + "): §f"
                        + categoryNames(completed, "Noch keine")
        );
        sender.sendMessage(
                "§cOffen (" + open.size() + "): §f"
                        + categoryNames(open, "Keine")
        );
        sender.sendMessage("§7Details und Auswahl: §f/gui");
        return true;
    }

    private static String categoryNames(
            List<StructureCategory> categories,
            String emptyText
    ) {
        if (categories.isEmpty()) {
            return emptyText;
        }
        return String.join(
                "§7, §f",
                categories.stream().map(StructureCategory::displayName).toList()
        );
    }
}
