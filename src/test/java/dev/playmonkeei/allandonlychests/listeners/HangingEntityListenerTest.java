package dev.playmonkeei.allandonlychests.listeners;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HangingEntityListenerTest {

    @Test
    void releasesOnlyElytraWhenOptionIsEnabled() {
        assertTrue(HangingEntityListener.canReleaseNaturalFrameItem(
                true,
                Material.ELYTRA
        ));
        assertFalse(HangingEntityListener.canReleaseNaturalFrameItem(
                false,
                Material.ELYTRA
        ));
        assertFalse(HangingEntityListener.canReleaseNaturalFrameItem(
                true,
                Material.DIAMOND
        ));
        assertFalse(HangingEntityListener.canReleaseNaturalFrameItem(
                true,
                Material.AIR
        ));
    }
}
