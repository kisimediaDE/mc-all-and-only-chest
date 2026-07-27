package dev.playmonkeei.allandonlychests.ui;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.challenge.StructureGoalCatalog;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Vanilla sidebar representation of the active challenge. An item icon like
 * the stream overlay would require a client resource pack, so this keeps the
 * same information entirely server-side.
 */
public final class ChallengeSidebar implements Listener, CommandExecutor {

    private final ChallengeStateRepository stateRepository;
    private final StructureGoalCatalog goalCatalog;
    private final Set<UUID> hiddenForPlayers = new HashSet<>();

    public ChallengeSidebar(
            ChallengeStateRepository stateRepository,
            StructureGoalCatalog goalCatalog
    ) {
        this.stateRepository = stateRepository;
        this.goalCatalog = goalCatalog;
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void refresh(Player player) {
        if (hiddenForPlayers.contains(player.getUniqueId())) {
            clear(player);
            return;
        }

        StructureCategory category = stateRepository.activeStructure().orElse(null);
        if (category == null) {
            clear(player);
            return;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                "aoc_status",
                Criteria.DUMMY,
                Component.text("All & Only Chests", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD)
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        int found = stateRepository.foundCount(category);
        int total = goalCatalog.goalsFor(category).size();

        setLine(
                objective,
                "aoc_structure_label",
                4,
                Component.text("Struktur:", NamedTextColor.YELLOW)
        );
        setLine(
                objective,
                "aoc_structure",
                3,
                Component.text(category.displayName(), NamedTextColor.WHITE)
        );
        setLine(
                objective,
                "aoc_items",
                2,
                Component.text("Items: ", NamedTextColor.AQUA)
                        .append(Component.text(found, NamedTextColor.WHITE))
                        .append(Component.text("/", NamedTextColor.GRAY))
                        .append(Component.text(total, NamedTextColor.WHITE))
        );
        setLine(
                objective,
                "aoc_sources",
                1,
                Component.text("Kisten: ", NamedTextColor.GOLD)
                        .append(Component.text(
                                stateRepository.openedSourceCount(),
                                NamedTextColor.WHITE
                        ))
        );

        player.setScoreboard(scoreboard);
    }

    private void setLine(
            Objective objective,
            String entry,
            int value,
            Component displayName
    ) {
        Score score = objective.getScore(entry);
        score.setScore(value);
        score.customName(displayName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur im Spiel verwendet werden.");
            return true;
        }

        if (!hiddenForPlayers.add(player.getUniqueId())) {
            hiddenForPlayers.remove(player.getUniqueId());
            refresh(player);
            player.sendMessage("§aChallenge-Anzeige eingeblendet.");
        } else {
            clear(player);
            player.sendMessage("§7Challenge-Anzeige ausgeblendet.");
        }
        return true;
    }

    private void clear(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
