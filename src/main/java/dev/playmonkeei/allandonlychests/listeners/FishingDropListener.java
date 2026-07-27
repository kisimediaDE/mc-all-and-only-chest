package dev.playmonkeei.allandonlychests.listeners;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * Prevents fishing from becoming an item source while leaving fishing
 * mechanics, hooked entities, rod durability, and experience untouched.
 */
public final class FishingDropListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCatch(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH
                && event.getCaught() instanceof Item caughtItem) {
            caughtItem.remove();
        }
    }
}
