package dev.playmonkeei.allandonlychests.listeners;

import dev.playmonkeei.allandonlychests.challenge.StructureGoalCatalog;
import dev.playmonkeei.allandonlychests.storage.ChallengeStateRepository;
import dev.playmonkeei.allandonlychests.storage.PlacedBlockRepository;
import dev.playmonkeei.allandonlychests.ui.ChallengeSidebar;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StructureLootListenerTest {

    @Test
    void allowsPlayersToOpenHorseEquipmentInventories() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.namespace()).thenReturn("allandonlychests");
        StructureLootListener listener = new StructureLootListener(
                plugin,
                mock(ChallengeStateRepository.class),
                mock(PlacedBlockRepository.class),
                mock(StructureGoalCatalog.class),
                mock(ChallengeSidebar.class),
                mock(Logger.class)
        );

        Horse horse = mock(Horse.class);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(horse);
        when(inventory.getContents()).thenReturn(new ItemStack[0]);

        InventoryOpenEvent event = mock(InventoryOpenEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getPlayer()).thenReturn(mock(Player.class));

        listener.onInventoryOpen(event);

        verify(event, never()).setCancelled(true);
    }
}
