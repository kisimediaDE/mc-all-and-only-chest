package dev.playmonkeei.allandonlychests.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

/**
 * Keeps naturally generated hanging entities from becoming item sources while
 * preserving full Vanilla behavior for player-placed frames, paintings, and
 * leash knots.
 */
public final class HangingEntityListener implements Listener {

    private final NamespacedKey playerPlacedKey;
    private final NamespacedKey legacyItemFrameKey;

    public HangingEntityListener(
            NamespacedKey playerPlacedKey,
            NamespacedKey legacyItemFrameKey
    ) {
        this.playerPlacedKey = playerPlacedKey;
        this.legacyItemFrameKey = legacyItemFrameKey;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        event.getEntity().getPersistentDataContainer().set(
                playerPlacedKey,
                PersistentDataType.BYTE,
                (byte) 1
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Hanging hanging)
                || isPlayerPlaced(hanging)) {
            return;
        }

        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) {
            sendNaturalMessage(event.getPlayer(), hanging);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Hanging hanging)
                || isPlayerPlaced(hanging)) {
            return;
        }

        event.setCancelled(true);
        if (event.getDamager() instanceof Player player) {
            sendNaturalMessage(player, hanging);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        Hanging hanging = event.getEntity();
        if (isPlayerPlaced(hanging)) {
            return;
        }

        event.setCancelled(true);
        if (!(hanging instanceof ItemFrame)
                && event instanceof HangingBreakByEntityEvent byEntityEvent
                && byEntityEvent.getRemover() instanceof Player player) {
            sendNaturalMessage(player, hanging);
        }
    }

    private boolean isPlayerPlaced(Hanging hanging) {
        if (hanging.getPersistentDataContainer().has(
                playerPlacedKey,
                PersistentDataType.BYTE
        )) {
            return true;
        }
        return hanging instanceof ItemFrame
                && hanging.getPersistentDataContainer().has(
                        legacyItemFrameKey,
                        PersistentDataType.BYTE
                );
    }

    private static void sendNaturalMessage(Player player, Hanging hanging) {
        if (hanging instanceof ItemFrame) {
            player.sendMessage(
                    "§cNatürlich generierte Item Frames dürfen nicht benutzt werden."
            );
        } else if (hanging instanceof Painting) {
            player.sendMessage(
                    "§cNatürlich generierte Gemälde dürfen nicht abgebaut werden."
            );
        } else {
            player.sendMessage(
                    "§cDieses natürlich generierte hängende Entity ist geschützt."
            );
        }
    }
}
