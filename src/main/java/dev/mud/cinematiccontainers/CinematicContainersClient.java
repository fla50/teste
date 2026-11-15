package dev.mud.cinematiccontainers;

import dev.dyslexic.cinematic.api.AddonStations;
import dev.dyslexic.cinematic.api.client.AddonScenes;
import net.fabricmc.api.ClientModInitializer;

public final class CinematicContainersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Garante que o registro comum ja aconteceu (a ordem dos entrypoints nao importa).
        ContainerDefs.registerAll();
        for (AddonStations.Definition def : ContainerDefs.all()) {
            AddonScenes.register(def.id(),
                    (pos, state, player, side) -> new ContainerStation(pos, state, side, def.menuClass()));
        }
    }
}
