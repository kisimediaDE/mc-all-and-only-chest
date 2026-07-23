package dev.playmonkeei.allandonlychests.ui;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.challenge.StructureGoalCatalog;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
                ChatColor.GOLD + "" + ChatColor.BOLD + "All & Only Chests"
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int found = stateRepository.foundCount(category);
        int total = goalCatalog.goalsFor(category).size();

        objective.getScore(ChatColor.YELLOW + "Struktur:").setScore(4);
        objective.getScore(ChatColor.WHITE + category.displayName()).setScore(3);
        objective.getScore(
                ChatColor.AQUA + "Items: " + ChatColor.WHITE + found
                        + ChatColor.GRAY + "/" + ChatColor.WHITE + total
        ).setScore(2);
        objective.getScore(
                ChatColor.GOLD + "Kisten: " + ChatColor.WHITE
                        + stateRepository.openedSourceCount()
        ).setScore(1);

        player.setScoreboard(scoreboard);
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
