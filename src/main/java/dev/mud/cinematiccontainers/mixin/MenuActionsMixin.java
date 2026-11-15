package dev.mud.cinematiccontainers.mixin;

import dev.dyslexic.cinematic.station.MenuActions;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * O bau do Ender nao tem um "menu de bloco" como o bau comum (o Minecraft abre o inventario
 * dele de outro jeito), entao o Cinematic nao conseguia abri-lo. Aqui ensinamos ele.
 */
@Mixin(value = MenuActions.class, remap = false)
public abstract class MenuActionsMixin {
    @Inject(method = "provider", at = @At("HEAD"), cancellable = true)
    private static void cinematicContainers$enderChest(ServerPlayer player, BlockPos pos, StationKind kind,
                                                       CallbackInfoReturnable<MenuProvider> cir) {
        BlockState state = player.level().getBlockState(pos);
        if (!state.is(Blocks.ENDER_CHEST)) {
            return;
        }
        if (!(player.level().getBlockEntity(pos) instanceof EnderChestBlockEntity chest)) {
            return;
        }
        BlockPos above = pos.above();
        if (player.level().getBlockState(above).isRedstoneConductor(player.level(), above)) {
            cir.setReturnValue(null);
            return;
        }
        PlayerEnderChestContainer container = player.getEnderChestInventory();
        container.setActiveChest(chest);
        cir.setReturnValue(new SimpleMenuProvider(
                (id, inventory, p) -> ChestMenu.threeRows(id, inventory, container),
                Component.translatable("container.enderchest")));
    }
}
