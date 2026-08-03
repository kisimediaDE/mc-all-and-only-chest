package dev.playmonkeei.allandonlychests.listeners;

import dev.playmonkeei.allandonlychests.storage.BlockPosition;
import dev.playmonkeei.allandonlychests.storage.PlacedBlockRepository;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.world.StructureGrowEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;
import java.util.List;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChallengeBlockListenerTest {

    private final PlacedBlockRepository repository = mock(PlacedBlockRepository.class);
    private final ChallengeBlockListener listener = new ChallengeBlockListener(
            mock(Plugin.class),
            repository,
            mock(Logger.class)
    );

    @Test
    void removesPlayerPlacedMarkersReplacedByGrowingTree() {
        Block firstSaplingPosition = blockAt(Material.OAK_LOG, 11, 64, -8);
        Block secondSaplingPosition = blockAt(Material.OAK_LOG, 12, 64, -8);
        BlockState firstState = mock(BlockState.class);
        BlockState secondState = mock(BlockState.class);
        when(firstState.getBlock()).thenReturn(firstSaplingPosition);
        when(secondState.getBlock()).thenReturn(secondSaplingPosition);

        StructureGrowEvent event = mock(StructureGrowEvent.class);
        when(event.getBlocks()).thenReturn(List.of(firstState, secondState));

        listener.onStructureGrow(event);

        verify(repository).untrackAll(List.of(
                BlockPosition.from(firstSaplingPosition),
                BlockPosition.from(secondSaplingPosition)
        ));
        verify(event, never()).setCancelled(true);
    }

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {
            "WHEAT",
            "CARROTS",
            "POTATOES",
            "BEETROOTS",
            "SHORT_GRASS",
            "DANDELION",
            "LEAF_LITTER"
    })
    void removesNaturalVegetationWithoutDropsBeforeWaterArrives(Material crop) {
        Block source = mock(Block.class);
        when(source.getType()).thenReturn(Material.WATER);
        Block target = blockAt(crop);

        BlockFromToEvent event = mock(BlockFromToEvent.class);
        when(event.getBlock()).thenReturn(source);
        when(event.getToBlock()).thenReturn(target);

        listener.onWaterFlowIntoVegetation(event);

        verify(target).setType(Material.AIR, false);
        verify(event, never()).setCancelled(true);
    }

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {
            "WHEAT",
            "CARROTS",
            "POTATOES",
            "BEETROOTS"
    })
    void leavesPlayerPlacedCropToExistingVanillaRules(Material crop) {
        Block source = mock(Block.class);
        when(source.getType()).thenReturn(Material.WATER);
        Block target = blockAt(crop);
        when(repository.isTracked(BlockPosition.from(target))).thenReturn(true);

        BlockFromToEvent event = mock(BlockFromToEvent.class);
        when(event.getBlock()).thenReturn(source);
        when(event.getToBlock()).thenReturn(target);

        listener.onWaterFlowIntoVegetation(event);

        verify(target, never()).setType(Material.AIR, false);
    }

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {
            "WHEAT",
            "CARROTS",
            "POTATOES",
            "BEETROOTS",
            "SHORT_GRASS",
            "DANDELION",
            "LEAF_LITTER"
    })
    void removesNaturalVegetationWithoutDropsDuringWaterPhysics(Material crop) {
        Block source = mock(Block.class);
        when(source.getType()).thenReturn(Material.WATER);
        Block target = blockAt(crop);

        BlockPhysicsEvent event = mock(BlockPhysicsEvent.class);
        when(event.getBlock()).thenReturn(target);
        when(event.getSourceBlock()).thenReturn(source);

        listener.onVegetationPhysicsFromWater(event);

        verify(event).setCancelled(true);
        verify(target).setType(Material.AIR, false);
    }

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {
            "WHEAT",
            "CARROTS",
            "POTATOES",
            "BEETROOTS"
    })
    void leavesPlayerPlacedCropToExistingRulesDuringWaterPhysics(Material crop) {
        Block source = mock(Block.class);
        when(source.getType()).thenReturn(Material.WATER);
        Block target = blockAt(crop);
        when(repository.isTracked(BlockPosition.from(target))).thenReturn(true);

        BlockPhysicsEvent event = mock(BlockPhysicsEvent.class);
        when(event.getBlock()).thenReturn(target);
        when(event.getSourceBlock()).thenReturn(source);

        listener.onVegetationPhysicsFromWater(event);

        verify(event, never()).setCancelled(true);
        verify(target, never()).setType(Material.AIR, false);
    }

    private Block blockAt(Material material) {
        return blockAt(material, 11, 64, -8);
    }

    private Block blockAt(Material material, int x, int y, int z) {
        World world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.fromString("0e94f6a5-21df-4d58-b929-024b2b3b2971"));

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(material);
        when(block.getWorld()).thenReturn(world);
        when(block.getX()).thenReturn(x);
        when(block.getY()).thenReturn(y);
        when(block.getZ()).thenReturn(z);
        return block;
    }
}
