package dev.mud.cinematiccontainers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dyslexic.cinematic.api.AddonStations;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lista dos containers que ganham a cena cinematografica e o registro deles no Cinematic.
 *
 * A API oficial de addons do Cinematic recusa blocos do "minecraft:" (ela foi feita para
 * blocos de outros mods). Por isso colocamos as definicoes direto nos mapas internos dela.
 *
 * Tipos: chest, barrel, shulker, hopper, dispenser (dispenser vale tambem para dropper).
 */
public final class ContainerDefs {
    public static final String MOD_ID = "cinematic_containers";
    private static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static final List<Direction> SIDES =
            List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    private static final String[] COLORS = {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    private static final String DEFAULT_CONFIG = """
            {
              "_help": "Para blocos de OUTROS mods que abrem um inventario igual ao de um bau, barril, shulker, funil ou dispenser. Exemplo: { \\"block\\": \\"outromod:bau_de_ouro\\", \\"type\\": \\"chest\\" }. Tipos: chest, barrel, shulker, hopper, dispenser. Reinicie o jogo depois de mudar. So funciona se o bloco usar o menu padrao do tipo escolhido.",
              "extra_containers": []
            }
            """;

    private static final List<AddonStations.Definition> ALL = new ArrayList<>();
    private static final Map<Identifier, String> TYPES = new HashMap<>();

    private ContainerDefs() {
    }

    public static List<AddonStations.Definition> all() {
        return ALL;
    }

    public static String typeOf(Identifier definitionId) {
        return TYPES.getOrDefault(definitionId, "chest");
    }

    private static Class<? extends AbstractContainerMenu> menuFor(String type) {
        return switch (type) {
            case "shulker" -> ShulkerBoxMenu.class;
            case "hopper" -> HopperMenu.class;
            case "dispenser" -> DispenserMenu.class;
            default -> ChestMenu.class;
        };
    }

    private static void add(Identifier block, String type, String idPath) {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, idPath);
        Function<BlockState, List<Direction>> sides =
                type.equals("barrel") ? ContainerDefs::barrelSides : state -> SIDES;
        ALL.add(new AddonStations.Definition(id, block, menuFor(type), sides));
        TYPES.put(id, type);
    }

    private static void addVanilla(String blockPath, String type) {
        add(Identifier.withDefaultNamespace(blockPath), type, blockPath);
    }

    /** Barril virado de lado: so da pra mexer pela face aberta (a que ele esta olhando). */
    private static List<Direction> barrelSides(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            if (facing.getAxis().isHorizontal()) {
                return List.of(facing);
            }
        }
        return SIDES;
    }

    /** Roda nos dois lados (cliente e servidor). */
    public static synchronized void registerAll() {
        if (!ALL.isEmpty()) {
            return;
        }
        addVanilla("chest", "chest");
        addVanilla("trapped_chest", "chest");
        addVanilla("ender_chest", "chest");
        for (String prefix : new String[] {"", "exposed_", "weathered_", "oxidized_"}) {
            addVanilla(prefix + "copper_chest", "chest");
            addVanilla("waxed_" + prefix + "copper_chest", "chest");
        }
        addVanilla("barrel", "barrel");
        addVanilla("shulker_box", "shulker");
        for (String color : COLORS) {
            addVanilla(color + "_shulker_box", "shulker");
        }
        addVanilla("hopper", "hopper");
        addVanilla("dispenser", "dispenser");
        addVanilla("dropper", "dispenser");
        loadExtras();
        for (AddonStations.Definition def : ALL) {
            force(def);
        }
    }

    private static void loadExtras() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("cinematic_containers.json");
        try {
            if (!Files.exists(file)) {
                Files.writeString(file, DEFAULT_CONFIG);
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            JsonArray extras = root.getAsJsonArray("extra_containers");
            if (extras == null) {
                return;
            }
            for (JsonElement element : extras) {
                JsonObject entry = element.getAsJsonObject();
                Identifier block = Identifier.tryParse(entry.get("block").getAsString());
                String type = entry.has("type") ? entry.get("type").getAsString() : "chest";
                if (block == null) {
                    LOG.warn("Bloco invalido no cinematic_containers.json: {}", entry);
                    continue;
                }
                add(block, type, "extra/" + block.getNamespace() + "/" + block.getPath());
                LOG.info("Container extra registrado: {} ({})", block, type);
            }
        } catch (Exception e) {
            LOG.warn("Nao consegui ler config/cinematic_containers.json; ignorando os extras.", e);
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
