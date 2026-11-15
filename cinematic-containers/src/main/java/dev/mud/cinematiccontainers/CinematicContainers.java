package dev.mud.cinematiccontainers;

import net.fabricmc.api.ModInitializer;

public final class CinematicContainers implements ModInitializer {
    @Override
    public void onInitialize() {
        ContainerDefs.registerAll();
    }
}
