package dev.playmonkeei.allandonlychests.ui;

import dev.playmonkeei.allandonlychests.challenge.StructureCategory;
import dev.playmonkeei.allandonlychests.challenge.StructureGoalCatalog;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side challenge HUD with a sidebar and BossBar representation.
 * An item icon like the stream overlay would require a client resource pack.
 */
public final class ChallengeSidebar implements Listener, TabExecutor {

    private final ChallengeStateRepository stateRepository;
    private final StructureGoalCatalog goalCatalog;
    private final Map<UUID, HudMode> modes = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

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
        HudMode mode = modes.getOrDefault(player.getUniqueId(), HudMode.SIDEBAR);
        if (mode == HudMode.OFF) {
            clearHud(player);
            return;
        }

        StructureCategory category = stateRepository.activeStructure().orElse(null);
        if (category == null) {
            clearHud(player);
            return;
        }

        int found = stateRepository.foundCount(category);
        int total = goalCatalog.goalsFor(category).size();
        int sources = stateRepository.openedSourceCount();

        if (mode == HudMode.BOSSBAR) {
            clearSidebar(player);
            refreshBossBar(player, category, found, total, sources);
            return;
        }

        hideBossBar(player);
        refreshSidebar(player, category, found, total, sources);
    }

    private void refreshSidebar(
            Player player,
            StructureCategory category,
            int found,
            int total,
            int sources
    ) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                "aoc_status",
                Criteria.DUMMY,
                Component.text("All & Only Chests", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD)
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

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
                        .append(Component.text(sources, NamedTextColor.WHITE))
        );

        player.setScoreboard(scoreboard);
    }

    private void refreshBossBar(
            Player player,
            StructureCategory category,
            int found,
            int total,
            int sources
    ) {
        float progress = total == 0
                ? 0.0f
                : Math.clamp((float) found / total, 0.0f, 1.0f);
        Component title = Component.text(category.displayName(), NamedTextColor.YELLOW)
                .append(Component.text("  •  ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Items ", NamedTextColor.AQUA))
                .append(Component.text(found + "/" + total, NamedTextColor.WHITE))
                .append(Component.text("  •  ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Kisten ", NamedTextColor.GOLD))
                .append(Component.text(sources, NamedTextColor.WHITE));

        BossBar bossBar = bossBars.get(player.getUniqueId());
        if (bossBar == null) {
            bossBar = BossBar.bossBar(
                    title,
                    progress,
                    BossBar.Color.YELLOW,
                    BossBar.Overlay.PROGRESS
            );
            bossBars.put(player.getUniqueId(), bossBar);
            player.showBossBar(bossBar);
            return;
        }

        bossBar.name(title);
        bossBar.progress(progress);
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        hideBossBar(event.getPlayer());
        modes.remove(event.getPlayer().getUniqueId());
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

        HudMode mode;
        if (args.length == 0) {
            mode = modes.getOrDefault(player.getUniqueId(), HudMode.SIDEBAR).next();
        } else if (args.length == 1) {
            mode = HudMode.fromArgument(args[0]);
            if (mode == null) {
                player.sendMessage(Component.text(
                        "Verwendung: /chesthud <sidebar|bossbar|off>",
                        NamedTextColor.RED
                ));
                return true;
            }
        } else {
            player.sendMessage(Component.text(
                    "Verwendung: /chesthud <sidebar|bossbar|off>",
                    NamedTextColor.RED
            ));
            return true;
        }

        modes.put(player.getUniqueId(), mode);
        refresh(player);
        player.sendMessage(mode.confirmation());
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
        return List.of("sidebar", "bossbar", "off").stream()
                .filter(option -> option.startsWith(prefix))
                .toList();
    }

    private void clearHud(Player player) {
        clearSidebar(player);
        hideBossBar(player);
    }

    private void clearSidebar(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard.getObjective("aoc_status") != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void hideBossBar(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    private enum HudMode {
        SIDEBAR,
        BOSSBAR,
        OFF;

        private HudMode next() {
            return switch (this) {
                case SIDEBAR -> BOSSBAR;
                case BOSSBAR -> OFF;
                case OFF -> SIDEBAR;
            };
        }

        private static @Nullable HudMode fromArgument(String argument) {
            return switch (argument.toLowerCase(Locale.ROOT)) {
                case "sidebar" -> SIDEBAR;
                case "bossbar" -> BOSSBAR;
                case "off", "aus" -> OFF;
                default -> null;
            };
        }

        private Component confirmation() {
            return switch (this) {
                case SIDEBAR -> Component.text(
                        "Challenge-Anzeige: Sidebar",
                        NamedTextColor.GREEN
                );
                case BOSSBAR -> Component.text(
                        "Challenge-Anzeige: BossBar",
                        NamedTextColor.GREEN
                );
                case OFF -> Component.text(
                        "Challenge-Anzeige ausgeblendet.",
                        NamedTextColor.GRAY
                );
            };
        }
    }
}
