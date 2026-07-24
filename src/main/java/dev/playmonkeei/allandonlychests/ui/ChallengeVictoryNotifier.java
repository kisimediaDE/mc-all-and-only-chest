package dev.playmonkeei.allandonlychests.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Announces the one-time transition to a completed challenge.
 */
public final class ChallengeVictoryNotifier {

    private ChallengeVictoryNotifier() {
    }

    public static void announce() {
        Component broadcast = Component.text(
                "All & Only Chests wurde abgeschlossen!",
                NamedTextColor.GOLD
        ).decorate(TextDecoration.BOLD);
        Bukkit.broadcast(broadcast);

        Title title = Title.title(
                Component.text("Geschafft!", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD),
                Component.text(
                        "Alle 18 Strukturen abgeschlossen",
                        NamedTextColor.WHITE
                ),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(1)
                )
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
            player.playSound(
                    player.getLocation(),
                    Sound.UI_TOAST_CHALLENGE_COMPLETE,
                    1.0f,
                    1.0f
            );
        }
    }
}
