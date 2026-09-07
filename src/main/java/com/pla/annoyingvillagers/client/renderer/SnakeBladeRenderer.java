package com.pla.annoyingvillagers.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pla.annoyingvillagers.AnnoyingVillagers;
import com.pla.annoyingvillagers.client.compat.BetterCombatSnakeAttachment;
import com.pla.annoyingvillagers.client.model.ModelSnakeBlade;
import com.pla.annoyingvillagers.client.model.ModelSnakeBladeFragment;
import com.pla.annoyingvillagers.entity.SnakeBladeEntity;
import com.pla.annoyingvillagers.init.AnnoyingVillagersModItems;
import com.pla.annoyingvillagers.item.DemoniacVoltageReaverItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleFunction;

public class SnakeBladeRenderer extends EntityRenderer<SnakeBladeEntity> {
    private static final ResourceLocation SNAKE_BLADE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AnnoyingVillagers.MODID, "textures/entities/snake_blade.png");
    private static final ResourceLocation FRAGMENT_CHAIN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AnnoyingVillagers.MODID, "textures/entities/fragment_chain.png");

    public static final int MAX_NECK_SEGMENTS = 128;
    private static final float FRAGMENT_LENGTH = 0.45F;
    private static final float HEAD_CLEAR = 0.35F;

    private final ModelSnakeBlade<SnakeBladeEntity> snakeBladeModel;
    private final ModelSnakeBladeFragment<SnakeBladeEntity> fragmentModel;

    public SnakeBladeRenderer(EntityRendererProvider.Context context) {
        super(context);

        ModelPart fragmentRoot = context.bakeLayer(ModelSnakeBladeFragment.LAYER_LOCATION);
        this.fragmentModel = new ModelSnakeBladeFragment<>(fragmentRoot);

        ModelPart bladeRoot = context.bakeLayer(ModelSnakeBlade.LAYER_LOCATION);
        this.snakeBladeModel = new ModelSnakeBlade<>(bladeRoot);
    }

    private static float tipClear(SnakeBladeEntity snakeBladeEntity) {
        return (snakeBladeEntity.hasBlade() || snakeBladeEntity.isRetracting()) ? HEAD_CLEAR : 0.0F;
    }

    private static VertexConsumer getEntityConsumer(MultiBufferSource buffer, ResourceLocation texture, boolean enchanted) {
        RenderType renderType = RenderType.entityCutoutNoCull(texture);
        return enchanted
                ? ItemRenderer.getFoilBuffer(buffer, renderType, true, true)
                : buffer.getBuffer(renderType);
    }

    @Override
    public boolean shouldRender(SnakeBladeEntity snakeBladeEntity, @NotNull Frustum frustum, double x, double y, double z) {
        Entity fromEntity = snakeBladeEntity.getRenderFromEntity();
        if (fromEntity != null && frustum.isVisible(snakeBladeEntity.getBoundingBox().minmax(fromEntity.getBoundingBox()))) {
            return true;
        }
        return super.shouldRender(snakeBladeEntity, frustum, x, y, z);
    }

    @Override
    public void render(
            @NotNull SnakeBladeEntity snakeBladeEntity,
            float entityYaw,
            float partialTicks,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight
    ) {
        if (ModList.get().isLoaded("bettercombat")
                && BetterCombatSnakeAttachment.defer(this, snakeBladeEntity, entityYaw, partialTicks, packedLight)) return;
        super.render(snakeBladeEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.pushPose();
        try {
            Entity fromEntity = snakeBladeEntity.getRenderFromEntity();
            if (fromEntity == null) return;

            double x = Mth.lerp(partialTicks, snakeBladeEntity.xo, snakeBladeEntity.getX());
            double y = Mth.lerp(partialTicks, snakeBladeEntity.yo, snakeBladeEntity.getY());
            double z = Mth.lerp(partialTicks, snakeBladeEntity.zo, snakeBladeEntity.getZ());

            float progress =
                    (snakeBladeEntity.prevProgress + (snakeBladeEntity.getProgress() - snakeBladeEntity.prevProgress) * partialTicks)
                            / SnakeBladeEntity.MAX_EXTEND_TIME;

            float tipOffset = snakeBladeEntity.isGuard() ? 1.8F : 2.2F;
            Vec3 swordPos = DemoniacVoltageReaverItem.getToolTipPos(fromEntity, partialTicks, tipOffset);
            boolean animatedPlayerSocket = ModList.get().isLoaded("bettercombat")
                    && fromEntity instanceof Player player
                    && BetterCombatSnakeAttachment.getToolTipPos(player) != null;

            Vec3 distVec = (swordPos != null)
                    ? swordPos.subtract(x, animatedPlayerSocket ? y : y + 1.2F, z)
                    : getPositionOfPriorMob(snakeBladeEntity, fromEntity, partialTicks).subtract(x, y, z);

            Vec3 to = distVec.scale(1.0F - progress);

            VertexConsumer fragmentConsumer = getEntityConsumer(buffer, FRAGMENT_CHAIN_TEXTURE, snakeBladeEntity.isEnchanted());

            int segmentCount = 0;
            Vec3 currentNeckButt = distVec;

            if (snakeBladeEntity.isGuard()) {
                double distanceLeft = distVec.distanceTo(to);
                double buildUpTo = Math.max(0.0, distanceLeft - tipClear(snakeBladeEntity));

                while (segmentCount < MAX_NECK_SEGMENTS && buildUpTo > 1.0e-3) {
                    double step = Math.min(buildUpTo, FRAGMENT_LENGTH);

                    Vec3 dir = to.subtract(currentNeckButt);
                    Vec3 next = dir.normalize().scale(step).add(currentNeckButt);

                    int neckLight = getLightColor(snakeBladeEntity, next.add(x, y, z));
                    renderNeckCube(currentNeckButt, next, poseStack, fragmentConsumer, neckLight, 0.0F, animatedPlayerSocket);

                    currentNeckButt = next;
                    buildUpTo -= step;
                    segmentCount++;
                }
            } else {
                double distanceLeft = distVec.distanceTo(to);
                double buildUpTo = Math.max(0.0, distanceLeft - tipClear(snakeBladeEntity));

                if (distanceLeft > 1.0e-4) {
                    Vec3 fromW = new Vec3(x, y, z).add(distVec);
                    Vec3 toW = new Vec3(x, y, z).add(to);

                    Vec3 fwd = toW.subtract(fromW).normalize();
                    Vec3 right = new Vec3(fwd.z, 0, -fwd.x);
                    if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
                    right = right.normalize();

                    double ampSide = Mth.clamp(distanceLeft * 0.18, 0.25, 2.0);
                    double ampUp = Mth.clamp(distanceLeft * 0.10, 0.00, 1.0);

                    long seed =
                            (((long) snakeBladeEntity.getId()) << 32)
                                    ^ (((long) snakeBladeEntity.getFromEntityID()) << 16)
                                    ^ (long) snakeBladeEntity.getToEntityID()
                                    ^ 0x9E3779B97F4A7C15L;

                    RandomSource rand = RandomSource.create(seed);
                    double sideSign = rand.nextBoolean() ? 1.0 : -1.0;

                    double time = (double) snakeBladeEntity.tickCount + (double) partialTicks;
                    double phase1 = rand.nextDouble() * Math.PI * 2.0;
                    double phase2 = rand.nextDouble() * Math.PI * 2.0;
                    double phaseYaw = rand.nextDouble() * Math.PI * 2.0;

                    double jitterSideBase = 0.05;
                    double jitterUpBase = 0.03;

                    Vec3 finalRight = right;

                    DoubleFunction<Vec3> wavePoint = (s) -> {
                        double u = s / distanceLeft;
                        double sin = Math.sin(Math.PI * u);

                        Vec3 base = fromW
                                .add(fwd.scale(s))
                                .add(finalRight.scale(ampSide * sideSign * sin))
                                .add(0, ampUp * sin, 0);

                        double w1 = 20.0;
                        double w2 = 17.0;

                        double headBias = Math.pow(sin, 0.8);
                        double rootFade = animatedPlayerSocket ? Math.min(1.0D, s / FRAGMENT_LENGTH) : 1.0D;
                        double jitterSide = rootFade * (jitterSideBase * (0.6 + 0.8 * headBias))
                                * Math.sin(w1 * time + 28.0 * u + phase1);
                        double jitterUp = rootFade * (jitterUpBase * (0.5 + 0.7 * headBias))
                                * Math.sin(w2 * time + 19.0 * u + phase2);

                        return base.add(finalRight.scale(jitterSide)).add(0, jitterUp, 0);
                    };

                    double s = 0.0;
                    Vec3 prevW = wavePoint.apply(s);

                    while (segmentCount < MAX_NECK_SEGMENTS && buildUpTo > 1.0e-3) {
                        double step = Math.min(buildUpTo, FRAGMENT_LENGTH);
                        s += step;

                        Vec3 nextW = wavePoint.apply(Math.min(s, distanceLeft - HEAD_CLEAR));

                        Vec3 prevLocal = prevW.subtract(x, y, z);
                        Vec3 nextLocal = nextW.subtract(x, y, z);

                        int neckLight = getLightColor(snakeBladeEntity, nextW);

                        float yawShake = (float) (4.0 * Math.sin(18.0 * time + 0.9 * segmentCount + phaseYaw));
                        renderNeckCube(prevLocal, nextLocal, poseStack, fragmentConsumer, neckLight, yawShake, animatedPlayerSocket);

                        prevW = nextW;
                        buildUpTo -= step;
                        segmentCount++;
                    }

                    currentNeckButt = prevW.subtract(x, y, z);
                } else {
                    double time = (double) snakeBladeEntity.tickCount + (double) partialTicks;

                    while (segmentCount < MAX_NECK_SEGMENTS && buildUpTo > 1.0e-3) {
                        double step = Math.min(buildUpTo, FRAGMENT_LENGTH);

                        Vec3 dir = to.subtract(currentNeckButt);
                        Vec3 next = dir.normalize().scale(step).add(currentNeckButt);

                        int neckLight = getLightColor(snakeBladeEntity, next.add(x, y, z));
                        float yawShake = (float) (3.0 * Math.sin(16.0 * time + 0.7 * segmentCount));

                        renderNeckCube(currentNeckButt, next, poseStack, fragmentConsumer, neckLight, yawShake, animatedPlayerSocket);

                        currentNeckButt = next;
                        buildUpTo -= step;
                        segmentCount++;
                    }
                }
            }

            VertexConsumer bladeConsumer = getEntityConsumer(buffer, SNAKE_BLADE_TEXTURE, snakeBladeEntity.isEnchanted());

            if (snakeBladeEntity.hasBlade() || snakeBladeEntity.isRetracting()) {
                poseStack.pushPose();
                poseStack.translate(to.x, to.y, to.z);

                Vec3 headDir = to.subtract(currentNeckButt);
                float rotY = (float) (Mth.atan2(headDir.x, headDir.z) * 180F / Math.PI);
                float rotX = (float) (-Mth.atan2(headDir.y, headDir.horizontalDistance()) * 180F / Math.PI);

                poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
                poseStack.mulPose(Axis.XP.rotationDegrees(rotX));

                double time = (double) snakeBladeEntity.tickCount + (double) partialTicks;
                float headYawWobble = (float) (1.6 * Math.sin(22.0 * time + 0.5));
                float headPitchWobble = (float) (Math.sin(27.0 * time + 1.2));
                poseStack.mulPose(Axis.YP.rotationDegrees(headYawWobble));
                poseStack.mulPose(Axis.XP.rotationDegrees(headPitchWobble));

                int headLight = getLightColor(snakeBladeEntity, to.add(x, y, z));
                if (animatedPlayerSocket) poseStack.translate(0.0D, -1.2D, 0.0D);
                snakeBladeModel.renderToBuffer(poseStack, bladeConsumer, headLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

                poseStack.popPose();
            }
        } finally {
            poseStack.popPose();
        }
    }

    private void renderNeckCube(Vec3 from, Vec3 to, PoseStack poseStack, VertexConsumer buffer,
                                int packedLightIn, float additionalYaw, boolean animatedPlayerSocket) {
        Vec3 dir = to.subtract(from);

        float yaw = (float) (Mth.atan2(dir.x, dir.z) * (180F / Math.PI));
        float pitch = (float) (-Mth.atan2(dir.y, dir.horizontalDistance()) * (180F / Math.PI));

        poseStack.pushPose();
        poseStack.translate(from.x, from.y, from.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + additionalYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // These models were authored 1.2 blocks above their origin. For an animated
        // socket, center them locally so pitching the chain cannot move its root.
        if (animatedPlayerSocket) poseStack.translate(0.0D, -1.2D, 0.0D);
        fragmentModel.renderToBuffer(poseStack, buffer, packedLightIn, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        poseStack.popPose();
    }

    private Vec3 getPositionOfPriorMob(SnakeBladeEntity snakeBladeEntity, Entity fromEntity, float partialTicks) {
        if (fromEntity instanceof com.pla.annoyingvillagers.entity.PortalEntity portalEntity) {
            Entity creator = snakeBladeEntity.getCreatorEntity();
            if (creator instanceof Player) return portalEntity.getSnakeBladeAnchor();
            return portalEntity.getPortalCenter();
        }

        double x = Mth.lerp(partialTicks, fromEntity.xo, fromEntity.getX());
        double y = Mth.lerp(partialTicks, fromEntity.yo, fromEntity.getY());
        double z = Mth.lerp(partialTicks, fromEntity.zo, fromEntity.getZ());

        float yOffset = 0.0F;

        if (fromEntity instanceof Player player && snakeBladeEntity.isCreator(fromEntity)) {
            float swing = player.getAttackAnim(partialTicks);
            float swingSin = Mth.sin(Mth.sqrt(swing) * (float) Math.PI);
            float bodyYaw = Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;

            int armSign = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;

            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.is(AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER.get())) {
                armSign = -armSign;
            }

            double sin = Mth.sin(bodyYaw);
            double cos = Mth.cos(bodyYaw);
            double armOffset = (double) armSign * 0.35D;

            if (entityRenderDispatcher.options.getCameraType().isFirstPerson() && player == Minecraft.getInstance().player) {
                double fovScale = 960.0D / (double) entityRenderDispatcher.options.fov().get();

                Vec3 nearPlane = entityRenderDispatcher.camera.getNearPlane().getPointOnPlane((float) armSign * 0.6F, -1);
                nearPlane = nearPlane.scale(fovScale);
                nearPlane = nearPlane.yRot(swingSin * 0.25F);
                nearPlane = nearPlane.xRot(-swingSin * 0.35F);

                x = Mth.lerp(partialTicks, player.xo, player.getX()) + nearPlane.x;
                y = Mth.lerp(partialTicks, player.yo, player.getY()) + nearPlane.y;
                z = Mth.lerp(partialTicks, player.zo, player.getZ()) + nearPlane.z;
                yOffset = player.getEyeHeight() * 0.5F;
            } else {
                x = Mth.lerp(partialTicks, player.xo, player.getX()) - cos * armOffset - sin * 0.2D;
                y = player.yo + (double) player.getEyeHeight() + (player.getY() - player.yo) * (double) partialTicks - 1D;
                z = Mth.lerp(partialTicks, player.zo, player.getZ()) - sin * armOffset + cos * 0.2D;
                yOffset = (player.isCrouching() ? -0.1875F : 0.0F) - player.getEyeHeight() * 0.4F;
            }
        }

        return new Vec3(x, y + yOffset, z);
    }

    private int getLightColor(Entity entity, Vec3 pos) {
        Vec3i blockPosInt = new Vec3i(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z));
        BlockPos blockPos = new BlockPos(blockPosInt);

        if (!entity.level().hasChunkAt(blockPos)) return 0;

        int packedBelow = LevelRenderer.getLightColor(entity.level(), blockPos);
        int packedAbove = LevelRenderer.getLightColor(entity.level(), blockPos.above());

        int block = Math.max(packedBelow & 255, packedAbove & 255);
        int sky = Math.max((packedBelow >> 16) & 255, (packedAbove >> 16) & 255);

        return block | (sky << 16);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SnakeBladeEntity snakeBladeEntity) {
        return SNAKE_BLADE_TEXTURE;
    }
}
