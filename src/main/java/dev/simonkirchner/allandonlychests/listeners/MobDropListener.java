package dev.simonkirchner.allandonlychests.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Suppresses mob item drops except for progression-critical blaze rods and
 * ender pearls. Experience drops remain unchanged.
 */
public final class MobDropListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob)) {
            return;
        }

        if (event.getEntity() instanceof Blaze) {
            event.getDrops().removeIf(item -> item.getType() != Material.BLAZE_ROD);
            return;
        }

        if (event.getEntity() instanceof Enderman) {
            event.getDrops().removeIf(item -> item.getType() != Material.ENDER_PEARL);
            return;
        }

        event.getDrops().clear();
    }
}
