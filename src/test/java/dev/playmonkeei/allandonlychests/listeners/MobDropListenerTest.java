package dev.playmonkeei.allandonlychests.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobDropListenerTest {

    private static final NamespacedKey CONTAINER_KEY =
            new NamespacedKey("allandonlychests", "player_placed_container_entity");
    private static final NamespacedKey VEHICLE_KEY =
            new NamespacedKey("allandonlychests", "player_placed_vehicle");

    private final MobDropListener listener = new MobDropListener(
            CONTAINER_KEY,
            VEHICLE_KEY
    );

    @Test
    void marksBoatsPlacedByPlayers() {
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        Boat boat = mock(Boat.class);
        when(boat.getPersistentDataContainer()).thenReturn(data);

        EntityPlaceEvent event = mock(EntityPlaceEvent.class);
        when(event.getPlayer()).thenReturn(mock(Player.class));
        when(event.getEntity()).thenReturn(boat);

        listener.onBoatPlace(event);

        verify(data).set(VEHICLE_KEY, PersistentDataType.BYTE, (byte) 1);
    }

    @Test
    void doesNotMarkBoatsWithoutAPlacingPlayer() {
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        Boat boat = mock(Boat.class);
        when(boat.getPersistentDataContainer()).thenReturn(data);

        EntityPlaceEvent event = mock(EntityPlaceEvent.class);
        when(event.getEntity()).thenReturn(boat);
        when(event.getPlayer()).thenReturn(null);

        listener.onBoatPlace(event);

        verify(data, never()).set(VEHICLE_KEY, PersistentDataType.BYTE, (byte) 1);
    }

    @Test
    void allowsDropsFromPlayerPlacedBoats() {
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        Boat boat = mock(Boat.class);
        when(boat.getPersistentDataContainer()).thenReturn(data);
        when(data.has(VEHICLE_KEY, PersistentDataType.BYTE)).thenReturn(true);

        EntityDropItemEvent event = mock(EntityDropItemEvent.class);
        when(event.getEntity()).thenReturn(boat);

        listener.onEntityDropItem(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void continuesToSuppressDropsFromUnmarkedLivingEntities() {
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        Chicken chicken = mock(Chicken.class);
        when(chicken.getPersistentDataContainer()).thenReturn(data);

        EntityDropItemEvent event = mock(EntityDropItemEvent.class);
        when(event.getEntity()).thenReturn(chicken);

        listener.onEntityDropItem(event);

        verify(event).setCancelled(true);
    }
}
