package dev.playmonkeei.allandonlychests.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Controls whether the naturally generated Elytra may be removed from its
 * protected item frame.
 */
public final class ElytraDropCommand implements CommandExecutor, TabCompleter {

    private static final String CONFIG_PATH =
            "gameplay.allow-natural-elytra-drop";

    private final JavaPlugin plugin;

    public ElytraDropCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean(CONFIG_PATH, false);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(
                    "§7Natürlicher Elytra-Drop: "
                            + (isEnabled() ? "§aAN" : "§cAUS")
            );
            return true;
        }

        if (args.length != 1
                || (!args[0].equalsIgnoreCase("on")
                && !args[0].equalsIgnoreCase("off"))) {
            sender.sendMessage("§cVerwendung: /elytradrop <on|off|status>");
            return true;
        }

        boolean enabled = args[0].equalsIgnoreCase("on");
        plugin.getConfig().set(CONFIG_PATH, enabled);
        plugin.saveConfig();
        sender.sendMessage(
                "§7Natürlicher Elytra-Drop wurde "
                        + (enabled ? "§aaktiviert" : "§cdeaktiviert")
                        + "§7."
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase();
        return List.of("on", "off", "status").stream()
                .filter(value -> value.startsWith(prefix))
                .toList();
    }
}
