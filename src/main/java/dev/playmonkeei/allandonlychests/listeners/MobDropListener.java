package dev.playmonkeei.allandonlychests.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;

/**
 * Suppresses mob item drops except for progression-critical blaze rods and
 * ender pearls. Experience drops remain unchanged.
 */
public final class MobDropListener implements Listener {

    private final NamespacedKey playerPlacedContainerEntityKey;

    public MobDropListener(NamespacedKey playerPlacedContainerEntityKey) {
        this.playerPlacedContainerEntityKey = playerPlacedContainerEntityKey;
    }

    /**
     * Prevents non-death item sources from living entities, such as naturally
     * laid eggs or items released from an entity's inventory.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(
                playerPlacedContainerEntityKey,
                PersistentDataType.BYTE
        )) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainerVehicleDestroy(VehicleDestroyEvent event) {
        if (!(event.getVehicle() instanceof InventoryHolder holder)
                || event.getVehicle().getPersistentDataContainer().has(
                        playerPlacedContainerEntityKey,
                        PersistentDataType.BYTE
                )) {
            return;
        }

        holder.getInventory().clear();
    }

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
