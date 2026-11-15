package dev.mud.cinematiccontainers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.api.client.AddonMenuStation;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneBuffers;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Cena em primeira pessoa para baus, barris, shulkers e bau do Ender: a camera se inclina
 * sobre o container e os itens aparecem flutuando em grade logo acima dele.
 */
public final class ContainerStation extends AddonMenuStation<AbstractContainerMenu> {
    private static final int COLUMNS = 9;
    private static final double SPACING = 0.15;
    private static final double FLOAT_HEIGHT = 0.30;
    private static final int PLAYER_SLOTS = 36;

    private final Vec3 anchor;
    private final boolean doubleChest;
    private int hoverSlot = -1;

    @SuppressWarnings("unchecked")
    public ContainerStation(BlockPos pos, BlockState state, Direction side,
                            Class<? extends AbstractContainerMenu> menuClass) {
        super(pos, side, (Class<AbstractContainerMenu>) menuClass, 0);
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        boolean dbl = false;
        if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            x = (x + other.getX() + 0.5) / 2.0;
            z = (z + other.getZ() + 0.5) / 2.0;
            dbl = true;
        }
        this.anchor = new Vec3(x, pos.getY() + 1.0, z);
        this.doubleChest = dbl;
    }

    // ---------- geometria ----------

    private int containerSlots() {
        AbstractContainerMenu menu = this.menu();
        return menu == null ? 0 : Math.max(0, menu.slots.size() - PLAYER_SLOTS);
    }

    private Vec3 slotPoint(int index, int total) {
        int rows = Math.max(1, (total + COLUMNS - 1) / COLUMNS);
        int col = index % COLUMNS;
        int row = index / COLUMNS;
        return this.anchor
                .add(0.0, FLOAT_HEIGHT, 0.0)
                .add(this.right().scale((col - (COLUMNS - 1) / 2.0) * SPACING))
                .add(this.side().scale((row - (rows - 1) / 2.0) * SPACING));
    }

    @Override
    public Vec3 center() {
        return this.anchor;
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return this.slotPoint(slot, Math.max(containerSlots(), slot + 1));
    }

    // ---------- camera e jogador ----------

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        double dist = this.doubleChest ? 1.2 : 0.9;
        Vec3 stand = new Vec3(
                this.anchor.x + this.side.getStepX() * dist,
                player.getY(),
                this.anchor.z + this.side.getStepZ() * dist);
        boolean far = Math.hypot(stand.x - player.getX(), stand.z - player.getZ()) > 0.06;
        return far ? List.of(stand) : List.of();
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = this.anchor.add(this.side().scale(1.05)).add(0.0, 1.25, 0.0);
        Vec3 look = this.anchor.add(0.0, 0.25, 0.0).subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) (-Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z))));
        return new CameraRig.Pose(eye, yaw, pitch, 0.0f);
    }

    @Override
    public double leanFov() {
        return 68.0;
    }

    @Override
    public float bodyPitch() {
        return 40.0f;
    }

    @Override
    public Vec3 rest(boolean right) {
        return this.anchor
                .add(this.right().scale(right ? 0.45 : -0.45))
                .add(this.side().scale(0.5))
                .add(0.0, 0.15, 0.0);
    }

    // ---------- cursor e maos ----------

    @Override
    public void updateCursor(boolean overPanel) {
        this.clearCursor();
        this.hoverSlot = -1;
        AbstractContainerMenu menu = this.menu();
        if (overPanel || menu == null || this.moving()) {
            return;
        }
        boolean carrying = !menu.getCarried().isEmpty();
        int total = containerSlots();
        double[] mouse = Picking.mouseGui();
        double best = Double.MAX_VALUE;
        for (int i = 0; i < total; i++) {
            if (!carrying && this.slot(i).isEmpty()) {
                continue;
            }
            Vec3 p = this.slotPoint(i, total);
            double[] at = Picking.toGui(p);
            double[] edge = Picking.toGui(p.add(0.0, 0.07, 0.0));
            if (at == null || edge == null) {
                continue;
            }
            double radius = Math.max(9.0, Math.hypot(at[0] - edge[0], at[1] - edge[1]));
            double d = Math.hypot(mouse[0] - at[0], mouse[1] - at[1]);
            if (d < radius && d < best) {
                best = d;
                this.hoverSlot = i;
            }
        }
        if (this.hoverSlot >= 0) {
            this.aim = this.slotPoint(this.hoverSlot, total);
        }
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        this.hoverSlot = -1;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        Vec3 reach = new Vec3(aim.x - shoulder.x, 0.0, aim.z - shoulder.z).normalize();
        double hover = carrying ? 0.075 : 0.045;
        return aim.add(0.0, 0.062 + hover - 0.06 * tap, 0.0).subtract(reach.scale(0.07));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.x - shoulder.x, 0.0, hand.z - shoulder.z).normalize();
        return hand.add(reach.scale(0.07)).add(0.0, -0.05, 0.0);
    }

    @Override
    protected void act(int button, boolean shift) {
        if (this.hoverSlot < 0) {
            return;
        }
        this.slotClick(this.hoverSlot, button, shift);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Vec3 at = this.anchor;
            mc.level.playLocalSound(at.x, at.y, at.z, SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, 0.35f, 1.1f, false);
        }
    }

    // ---------- desenho ----------

    @Override
    public void renderScene(PoseStack pose, SceneBuffers buffers, Camera camera, int light) {
        AbstractContainerMenu menu = this.menu();
        if (menu == null || this.outro()) {
            return;
        }
        Vec3 cam = camera.position();
        int total = containerSlots();
        Vec3 origin = this.anchor.add(0.0, 0.05, 0.0);
        for (int i = 0; i < total; i++) {
            ItemStack stack = this.slot(i);
            if (stack.isEmpty()) {
                continue;
            }
            double k = Anim.outCubic(Anim.window(Scene.time, 0.15 + i * 0.012, 0.6 + i * 0.012));
            boolean hover = i == this.hoverSlot;
            Vec3 at = Anim.lerp(origin, this.slotPoint(i, total), k)
                    .add(0.0, hover ? 0.05 : 0.012 * Math.sin(Scene.clock() * 2.0 + i), 0.0);
            float base = ItemPlacer.isBlock(stack) ? 0.09f : 0.14f;
            float size = (float) (base * (0.4 + 0.6 * k) * (hover ? 1.25 : 1.0));
            ItemPlacer.drawOriented(pose, buffers, cam, stack, at,
                    ItemPlacer.lying(ItemPlacer.awayYaw(this.side), 0.0f), size, light);
        }
    }

    @Override
    public void renderWorld(PoseStack pose, SceneBuffers buffers, Vec3 camera, int light) {
        if (this.hoverSlot >= 0 && this.aim != null) {
            float p = (float) (0.18 + 0.06 * Math.sin(Scene.clock() * 5.0));
            EnchantFx.disc(pose, buffers, camera, this.aim.add(0.0, -0.01, 0.0), 0.075, 0.0,
                    EnchantFx.GLOW, p, p * 0.8f, p * 0.55f);
        }
    }

    @Override
    public Component hint() {
        return Component.translatable("cinematic_containers.hint");
    }
}
