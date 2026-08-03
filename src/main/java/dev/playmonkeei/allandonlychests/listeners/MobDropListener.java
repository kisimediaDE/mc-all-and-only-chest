package dev.playmonkeei.allandonlychests.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;

/**
 * Suppresses mob item drops except for progression-critical blaze rods and
 * ender pearls. Experience drops remain unchanged.
 */
public final class MobDropListener implements Listener {

    private final NamespacedKey playerPlacedContainerEntityKey;
    private final NamespacedKey playerPlacedVehicleKey;

    public MobDropListener(
            NamespacedKey playerPlacedContainerEntityKey,
            NamespacedKey playerPlacedVehicleKey
    ) {
        this.playerPlacedContainerEntityKey = playerPlacedContainerEntityKey;
        this.playerPlacedVehicleKey = playerPlacedVehicleKey;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBoatPlace(EntityPlaceEvent event) {
        if (event.getPlayer() == null || !(event.getEntity() instanceof Boat boat)) {
            return;
        }

        boat.getPersistentDataContainer().set(
                playerPlacedVehicleKey,
                PersistentDataType.BYTE,
                (byte) 1
        );
    }

    /**
     * Prevents non-death item sources from living entities, such as naturally
     * laid eggs or items released from an entity's inventory.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (isPlayerPlaced(event.getEntity())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainerVehicleDestroy(VehicleDestroyEvent event) {
        if (!(event.getVehicle() instanceof InventoryHolder holder)
                || isPlayerPlaced(event.getVehicle())) {
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

    private boolean isPlayerPlaced(Entity entity) {
        return entity.getPersistentDataContainer().has(
                playerPlacedContainerEntityKey,
                PersistentDataType.BYTE
        ) || entity.getPersistentDataContainer().has(
                playerPlacedVehicleKey,
                PersistentDataType.BYTE
        );
    }
}
