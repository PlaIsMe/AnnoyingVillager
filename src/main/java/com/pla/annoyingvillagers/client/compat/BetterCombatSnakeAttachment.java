package com.pla.annoyingvillagers.client.compat;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.client.renderer.SnakeBladeRenderer;
import com.pla.annoyingvillagers.entity.SnakeBladeEntity;
import com.pla.annoyingvillagers.item.DemoniacVoltageReaverItem;
import com.pla.annoyingvillagers.mixin.client.SnakeAttachmentGameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-only sockets from the actual held-item pose, including Player Animator's arm bends. */
@Mod.EventBusSubscriber(modid = AnnoyingVillagers.MODID, value = Dist.CLIENT)
public final class BetterCombatSnakeAttachment {
    private static final Map<Player, Vec3> SOCKETS = new HashMap<>();
    private static final List<PendingSnake> PENDING = new ArrayList<>();
    private static final Matrix4f INVERSE_VIEW = new Matrix4f();
    private static final Matrix4f WORLD_PROJECTION = new Matrix4f();
    private static LivingEntity itemOwner;
    private static boolean drawingSnakes;
    private static boolean samplingHand;
    private static Vec3 cameraPosition = Vec3.ZERO;

    private BetterCombatSnakeAttachment() {}

    public static void beginFrame(PoseStack pose, Matrix4f projection, Vec3 camera) {
        SOCKETS.clear();
        PENDING.clear();
        itemOwner = null;
        INVERSE_VIEW.set(pose.last().pose()).invert();
        WORLD_PROJECTION.set(projection);
        cameraPosition = camera;
    }

    public static void setItemOwner(LivingEntity owner) {
        itemOwner = owner;
    }

    public static void capture(ItemStack stack, ItemDisplayContext context, PoseStack pose) {
        if (!(itemOwner instanceof Player player)
                || !(stack.getItem() instanceof DemoniacVoltageReaverItem)
                || !ItemStack.isSameItemSameTags(stack, player.getMainHandItem())
                || !DemoniacVoltageReaverItem.hasSnakeAnimation(stack)) return;
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                && !context.firstPerson()) return;
        boolean leftHand = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        if (leftHand != (player.getMainArm() == HumanoidArm.LEFT)) return;

        // End of the retained blade in custom/demoniac_voltage_reaver_snake.json.
        // Its tip cubes are rotated -45 degrees about Z around (-2, 7, 2).
        float diagonal = Mth.SQRT_OF_TWO * 0.5F;
        Vector3f socket = new Vector3f((-2.0F + 21.0F * diagonal) / 16.0F,
                (7.0F + 5.0F * diagonal) / 16.0F, 8.0F / 16.0F);
        pose.last().pose().transformPosition(socket);
        if (context.firstPerson()) {
            if (!samplingHand) return;
            // The ordinary hand pass uses a different FOV from the world pass.
            Minecraft mc = Minecraft.getInstance();
            var accessor = (SnakeAttachmentGameRendererAccessor) mc.gameRenderer;
            Matrix4f handProjection = mc.gameRenderer.getProjectionMatrix(
                    accessor.av$handFov(mc.gameRenderer.getMainCamera(), mc.getFrameTime(), false));
            new Matrix4f(WORLD_PROJECTION).invert().mul(handProjection).transformProject(socket);
        }
        INVERSE_VIEW.transformPosition(socket);
        SOCKETS.put(player, cameraPosition.add(socket.x, socket.y, socket.z));
    }

    public static Vec3 getToolTipPos(Player player) {
        return SOCKETS.get(player);
    }

    public static boolean defer(SnakeBladeRenderer renderer, SnakeBladeEntity snake, float yaw,
                                float partialTick, int light) {
        if (drawingSnakes || !(snake.getRenderFromEntity() instanceof Player)) return false;
        PENDING.add(new PendingSnake(renderer, snake, yaw, partialTick, light));
        return true;
    }

    @SubscribeEvent
    public static void afterEntities(RenderLevelStageEvent event) {
        if (!ModList.get().isLoaded("bettercombat")
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || PENDING.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        // Better Combat's punch supplies a third-person-model socket even in first person.
        // After the animation fades, sample the normal hand transforms without drawing them.
        if (mc.player != null && mc.options.getCameraType().isFirstPerson()
                && !SOCKETS.containsKey(mc.player) && !mc.options.hideGui
                && !mc.player.isSpectator() && !mc.player.isSleeping()
                && PENDING.stream().anyMatch(p -> p.snake.getRenderFromEntity() == mc.player)) {
            PoseStack handPose = new PoseStack();
            var accessor = (SnakeAttachmentGameRendererAccessor) mc.gameRenderer;
            accessor.av$bobHurt(handPose, event.getPartialTick());
            if (mc.options.bobView().get()) accessor.av$bobView(handPose, event.getPartialTick());
            samplingHand = true;
            try {
                mc.gameRenderer.itemInHandRenderer.renderHandsWithItems(event.getPartialTick(), handPose,
                        SamplingBuffer.INSTANCE, mc.player,
                        mc.getEntityRenderDispatcher().getPackedLightCoords(mc.player, event.getPartialTick()));
            } finally {
                samplingHand = false;
                itemOwner = null;
            }
        }
        // Entity order is arbitrary: wait until every player's held-item layer has run.
        drawingSnakes = true;
        PoseStack pose = event.getPoseStack();
        try {
            for (PendingSnake pending : PENDING) {
                SnakeBladeEntity snake = pending.snake;
                pose.pushPose();
                try {
                    pose.translate(Mth.lerp(pending.partialTick, snake.xo, snake.getX()) - cameraPosition.x,
                            Mth.lerp(pending.partialTick, snake.yo, snake.getY()) - cameraPosition.y,
                            Mth.lerp(pending.partialTick, snake.zo, snake.getZ()) - cameraPosition.z);
                    pending.renderer.render(snake, pending.yaw, pending.partialTick, pose,
                            mc.renderBuffers().bufferSource(), pending.light);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            drawingSnakes = false;
            PENDING.clear();
        }
    }

    private record PendingSnake(SnakeBladeRenderer renderer, SnakeBladeEntity snake, float yaw,
                                float partialTick, int light) {}

    /** Transform-only sampling must never flush the world's buffers or submit duplicate geometry. */
    private static final class SamplingBuffer extends MultiBufferSource.BufferSource {
        private static final SamplingBuffer INSTANCE = new SamplingBuffer();
        private static final VertexConsumer DISCARD = new VertexConsumer() {
            public VertexConsumer vertex(double x, double y, double z) { return this; }
            public VertexConsumer color(int r, int g, int b, int a) { return this; }
            public VertexConsumer uv(float u, float v) { return this; }
            public VertexConsumer overlayCoords(int u, int v) { return this; }
            public VertexConsumer uv2(int u, int v) { return this; }
            public VertexConsumer normal(float x, float y, float z) { return this; }
            public void endVertex() {}
            public void defaultColor(int r, int g, int b, int a) {}
            public void unsetDefaultColor() {}
        };

        private SamplingBuffer() { super(new BufferBuilder(256), Map.of()); }
        @Override public VertexConsumer getBuffer(RenderType type) { return DISCARD; }
        @Override public void endBatch() {}
        @Override public void endBatch(RenderType type) {}
        @Override public void endLastBatch() {}
    }
}
