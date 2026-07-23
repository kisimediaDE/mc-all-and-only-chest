package dev.simonkirchner.allandonlychests;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for the All and Only Chests challenge plugin.
 */
public final class AllAndOnlyChestsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("All and Only Chests wurde erfolgreich aktiviert.");
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
