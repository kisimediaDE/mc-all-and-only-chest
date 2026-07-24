package dev.playmonkeei.allandonlychests.commands;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.challenge.StructureGoal;
import dev.playmonkeei.allandonlychests.challenge.StructureGoalCatalog;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import dev.playmonkeei.allandonlychests.ui.ChallengeSidebar;
import dev.playmonkeei.allandonlychests.ui.ChallengeVictoryNotifier;
import org.bukkit.Sound;
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
import java.util.stream.Stream;

/**
 * OP-only development command for testing the completion transition.
 */
public final class StructureCompleteCommand implements CommandExecutor, TabCompleter {

    private final ChallengeStateRepository stateRepository;
    private final StructureGoalCatalog goalCatalog;
    private final ChallengeSidebar sidebar;

    public StructureCompleteCommand(
            ChallengeStateRepository stateRepository,
            StructureGoalCatalog goalCatalog,
            ChallengeSidebar sidebar
    ) {
        this.stateRepository = stateRepository;
        this.goalCatalog = goalCatalog;
        this.sidebar = sidebar;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length != 1) {
            sender.sendMessage("§cVerwendung: /structurecomplete <Kategorie|all>");
            return true;
        }

        String requestedId = args[0].toLowerCase(Locale.ROOT);
        if (requestedId.equals("all")) {
            completeAll(sender);
            return true;
        }

        StructureCategory category = StructureCategory.fromId(requestedId).orElse(null);
        if (category == null) {
            sender.sendMessage("§cUnbekannte Struktur-Kategorie: §f" + args[0]);
            sender.sendMessage("§7Nutze die Tab-Vervollständigung für gültige Kategorien.");
            return true;
        }

        if (stateRepository.isCompleted(category)) {
            sender.sendMessage(
                    "§a" + category.displayName() + " ist bereits abgeschlossen."
            );
            return true;
        }

        List<StructureGoal> allGoals = goalCatalog.goalsFor(category);
        ChallengeStateRepository.ProgressUpdate update =
                stateRepository.recordFoundGoals(category, allGoals, allGoals);
        sidebar.refreshAll();

        sender.sendMessage(
                "§aTestabschluss gesetzt: §f" + category.displayName()
                        + " §7(" + update.foundCount() + "/" + update.totalCount() + ")"
        );
        if (update.completedNow()) {
            if (update.challengeCompletedNow()) {
                ChallengeVictoryNotifier.announce();
            } else {
                sender.sendMessage("§6Die nächste Struktur kann jetzt ausgewählt werden.");
            }
            if (!update.challengeCompletedNow() && sender instanceof Player player) {
                player.playSound(
                        player.getLocation(),
                        Sound.UI_TOAST_CHALLENGE_COMPLETE,
                        1.0f,
                        1.0f
                );
            }
        }
        return true;
    }

    private void completeAll(CommandSender sender) {
        int newlyCompleted = 0;
        int alreadyCompleted = 0;
        boolean challengeCompletedNow = false;

        for (StructureCategory category : StructureCategory.values()) {
            if (stateRepository.isCompleted(category)) {
                alreadyCompleted++;
                continue;
            }

            List<StructureGoal> allGoals = goalCatalog.goalsFor(category);
            ChallengeStateRepository.ProgressUpdate update =
                    stateRepository.recordFoundGoals(category, allGoals, allGoals);
            if (update.completedNow()) {
                newlyCompleted++;
            }
            if (update.challengeCompletedNow()) {
                challengeCompletedNow = true;
            }
        }

        sidebar.refreshAll();
        if (newlyCompleted == 0) {
            sender.sendMessage("§aAlle Strukturen sind bereits abgeschlossen.");
            return;
        }

        sender.sendMessage(
                "§aTestabschluss gesetzt: §f" + newlyCompleted
                        + " §aStrukturen neu abgeschlossen."
        );
        sender.sendMessage(
                "§7Bereits abgeschlossen: " + alreadyCompleted
        );
        if (challengeCompletedNow) {
            ChallengeVictoryNotifier.announce();
        }
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
        return Stream.concat(
                        Stream.of("all"),
                        Arrays.stream(StructureCategory.values())
                                .filter(category -> !stateRepository.isCompleted(category))
                                .map(StructureCategory::id)
                )
                .filter(id -> id.startsWith(prefix))
                .toList();
    }
}
