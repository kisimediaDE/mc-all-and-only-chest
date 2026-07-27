package dev.playmonkeei.allandonlychests.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

/**
 * Keeps naturally generated item frames from becoming an item source while
 * preserving full Vanilla behavior for player-placed normal and glow frames.
 */
public final class ItemFrameListener implements Listener {

    private static final String NATURAL_FRAME_MESSAGE =
            "§cNatürlich generierte Item Frames dürfen nicht benutzt werden.";

    private final NamespacedKey playerPlacedKey;

    public ItemFrameListener(NamespacedKey playerPlacedKey) {
        this.playerPlacedKey = playerPlacedKey;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() != null && event.getEntity() instanceof ItemFrame itemFrame) {
            itemFrame.getPersistentDataContainer().set(
                    playerPlacedKey,
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame itemFrame)
                || isPlayerPlaced(itemFrame)) {
            return;
        }

        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) {
            event.getPlayer().sendMessage(NATURAL_FRAME_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame itemFrame)
                || isPlayerPlaced(itemFrame)) {
            return;
        }

        event.setCancelled(true);
        if (event.getDamager() instanceof Player player) {
            player.sendMessage(NATURAL_FRAME_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (event.getEntity() instanceof ItemFrame itemFrame
                && !isPlayerPlaced(itemFrame)) {
            event.setCancelled(true);
        }
    }

    private boolean isPlayerPlaced(ItemFrame itemFrame) {
        return itemFrame.getPersistentDataContainer().has(
                playerPlacedKey,
                PersistentDataType.BYTE
        );
    }
}
