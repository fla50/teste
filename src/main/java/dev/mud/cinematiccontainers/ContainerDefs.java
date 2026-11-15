package dev.mud.cinematiccontainers;

import dev.dyslexic.cinematic.api.AddonStations;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

/**
 * Lista dos containers que ganham a cena cinematografica e o registro deles no Cinematic.
 *
 * A API oficial de addons do Cinematic recusa blocos do "minecraft:" (ela foi feita para
 * blocos de outros mods). Por isso colocamos as definicoes direto nos mapas internos dela.
 */
public final class ContainerDefs {
    public static final String MOD_ID = "cinematic_containers";

    private static final List<Direction> SIDES =
            List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    private static final String[] COLORS = {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    private static final List<AddonStations.Definition> ALL = new ArrayList<>();

    private ContainerDefs() {
    }

    public static List<AddonStations.Definition> all() {
        return ALL;
    }

    private static void add(String blockPath, Class<? extends AbstractContainerMenu> menu) {
        Identifier block = Identifier.withDefaultNamespace(blockPath);
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, blockPath);
        ALL.add(new AddonStations.Definition(id, block, menu, state -> SIDES));
    }

    /** Roda nos dois lados (cliente e servidor). */
    public static synchronized void registerAll() {
        if (!ALL.isEmpty()) {
            return;
        }
        add("chest", ChestMenu.class);
        add("trapped_chest", ChestMenu.class);
        add("barrel", ChestMenu.class);
        add("ender_chest", ChestMenu.class);
        for (String prefix : new String[] {"", "exposed_", "weathered_", "oxidized_"}) {
            add(prefix + "copper_chest", ChestMenu.class);
            add("waxed_" + prefix + "copper_chest", ChestMenu.class);
        }
        add("shulker_box", ShulkerBoxMenu.class);
        for (String color : COLORS) {
            add(color + "_shulker_box", ShulkerBoxMenu.class);
        }
        for (AddonStations.Definition def : ALL) {
            force(def);
        }
    }

    @SuppressWarnings("unchecked")
    private static void force(AddonStations.Definition def) {
        try {
            Field byId = AddonStations.class.getDeclaredField("BY_ID");
            Field byBlock = AddonStations.class.getDeclaredField("BY_BLOCK");
            byId.setAccessible(true);
            byBlock.setAccessible(true);
            Map<Identifier, AddonStations.Definition> ids =
                    (Map<Identifier, AddonStations.Definition>) byId.get(null);
            Map<Identifier, AddonStations.Definition> blocks =
                    (Map<Identifier, AddonStations.Definition>) byBlock.get(null);
            synchronized (AddonStations.class) {
                ids.put(def.id(), def);
                blocks.put(def.blockId(), def);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cinematic Containers: nao consegui registrar " + def.id(), e);
        }
    }
}
