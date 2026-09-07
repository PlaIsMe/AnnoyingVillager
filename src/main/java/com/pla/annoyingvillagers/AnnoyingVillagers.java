package com.pla.annoyingvillagers;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.pla.annoyingvillagers.client.gui.InventoryViewerScreen;
import com.pla.annoyingvillagers.client.engine.CameraEngine;
import com.pla.annoyingvillagers.client.engine.SpriteArrowsCommonEntrypoint;
import com.pla.annoyingvillagers.config.AnnoyingVillagersClientConfig;
import com.pla.annoyingvillagers.config.AnnoyingVillagersSpawnConfig;
import com.pla.annoyingvillagers.config.AnnoyingVillagersConfig;
import com.pla.annoyingvillagers.init.*;
import com.pla.annoyingvillagers.item.FishingRodGrappleUtil;
import com.pla.annoyingvillagers.item.HookGunItem;
import com.pla.annoyingvillagers.network.*;
import com.pla.annoyingvillagers.world.AVMobSpawnBiomeModifier;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AnnoyingVillagers.MODID)
public class AnnoyingVillagers {
    public static final Logger LOGGER = LogManager.getLogger(AnnoyingVillagers.class);
    public static final String MODID = "annoyingvillagers";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry
            .newSimpleChannel(ResourceLocation.fromNamespaceAndPath(AnnoyingVillagers.MODID, "main"), () -> "1", "1"::equals, "1"::equals);
    private static int messageID = 0;

    public AnnoyingVillagers(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);

        AnnoyingVillagersModBlocks.REGISTRY.register(modEventBus);
        AnnoyingVillagersModBlockEntities.REGISTRY.register(modEventBus);
        AnnoyingVillagersModItems.REGISTRY.register(modEventBus);
        AnnoyingVillagersModMenus.REGISTRY.register(modEventBus);
        AnnoyingVillagersModEntities.REGISTRY.register(modEventBus);
        AnnoyingVillagersModMobEffects.REGISTRY.register(modEventBus);
        AnnoyingVillagersModParticleTypes.REGISTRY.register(modEventBus);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, AnnoyingVillagersModCapabilities::attachEntityCapability);
        modEventBus.addListener(AnnoyingVillagersModCapabilities::registerCapabilities);
        AnnoyingVillagersModCreativeTabs.register(modEventBus);
        AnnoyingVillagersModSounds.register(modEventBus);
        context.registerConfig(ModConfig.Type.COMMON, AnnoyingVillagersConfig.SPEC, "annoyingvillagers-server.toml");
        DeferredRegister<Codec<? extends BiomeModifier>> biomeModifiers =
                DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MODID);
        biomeModifiers.register(modEventBus);
        biomeModifiers.register("av_mob_spawns", AVMobSpawnBiomeModifier::makeCodec);
        context.registerConfig(ModConfig.Type.COMMON, AnnoyingVillagersSpawnConfig.SPEC, "annoyingvillagers-spawns.toml");

        if (FMLEnvironment.dist.isClient()) {
            context.registerConfig(ModConfig.Type.CLIENT, AnnoyingVillagersClientConfig.SPEC, "annoyingvillagers-client.toml");
            modEventBus.addListener(this::clientSetup);
            modEventBus.addListener(EventPriority.LOWEST, ClassLoadingProtection::listen);
        }
    }

    private static class ClassLoadingProtection {
        private static void listen(FMLClientSetupEvent event) {
            event.enqueueWork(SpriteArrowsCommonEntrypoint::replace);
        }
    }

    public static <T> void addNetworkMessage(Class<T> oclass, BiConsumer<T, FriendlyByteBuf> biconsumer, Function<FriendlyByteBuf, T> function, BiConsumer<T, Supplier<Context>> biconsumer1) {
        AnnoyingVillagers.PACKET_HANDLER.registerMessage(AnnoyingVillagers.messageID, oclass, biconsumer, function, biconsumer1);
        ++AnnoyingVillagers.messageID;
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(AnnoyingVillagersModMenus.INVENTORY_VIEWER.get(), InventoryViewerScreen::new));
        new CameraEngine();
    }

    @EventBusSubscriber(bus = Bus.MOD)
    public static class initer {
        @SubscribeEvent
        public static void init(FMLCommonSetupEvent fmlCommonSetupEvent) {
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundMuteExplosionAtPos.class,
                    ClientboundMuteExplosionAtPos::encode,
                    ClientboundMuteExplosionAtPos::decode,
                    ClientboundMuteExplosionAtPos::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundHerobrinePortalFx.class,
                    ClientboundHerobrinePortalFx::encode,
                    ClientboundHerobrinePortalFx::decode,
                    ClientboundHerobrinePortalFx::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundWoopieSwordWindFx.class,
                    ClientboundWoopieSwordWindFx::encode,
                    ClientboundWoopieSwordWindFx::decode,
                    ClientboundWoopieSwordWindFx::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundBlackFireFx.class,
                    ClientboundBlackFireFx::encode,
                    ClientboundBlackFireFx::decode,
                    ClientboundBlackFireFx::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    CPApplyShake.class,
                    CPApplyShake::encode,
                    CPApplyShake::new,
                    CPApplyShake::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundDiamondAttractorFx.class,
                    ClientboundDiamondAttractorFx::encode,
                    ClientboundDiamondAttractorFx::decode,
                    ClientboundDiamondAttractorFx::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundHerobrineAssistanceFx.class,
                    ClientboundHerobrineAssistanceFx::encode,
                    ClientboundHerobrineAssistanceFx::decode,
                    ClientboundHerobrineAssistanceFx::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundEnderAegisSparkFx.class,
                    ClientboundEnderAegisSparkFx::encode,
                    ClientboundEnderAegisSparkFx::decode,
                    ClientboundEnderAegisSparkFx::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundEliteHerobrineFx.class,
                    ClientboundEliteHerobrineFx::encode,
                    ClientboundEliteHerobrineFx::decode,
                    ClientboundEliteHerobrineFx::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundBlueDemonEffectFx.class,
                    ClientboundBlueDemonEffectFx::encode,
                    ClientboundBlueDemonEffectFx::decode,
                    ClientboundBlueDemonEffectFx::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundTeleportPortalFx.class,
                    ClientboundTeleportPortalFx::encode,
                    ClientboundTeleportPortalFx::decode,
                    ClientboundTeleportPortalFx::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundPlayerGroundTransitionPosition.class,
                    ClientboundPlayerGroundTransitionPosition::encode,
                    ClientboundPlayerGroundTransitionPosition::decode,
                    ClientboundPlayerGroundTransitionPosition::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundRigAnimation.class,
                    ClientboundRigAnimation::encode,
                    ClientboundRigAnimation::decode,
                    ClientboundRigAnimation::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundBetterCombatAnimation.class,
                    ClientboundBetterCombatAnimation::encode,
                    ClientboundBetterCombatAnimation::decode,
                    ClientboundBetterCombatAnimation::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundGroundFracture.class,
                    ClientboundGroundFracture::encode,
                    ClientboundGroundFracture::decode,
                    ClientboundGroundFracture::handle
            );
            AnnoyingVillagers.addNetworkMessage(
                    ClientboundGroundStuckKnockoutFx.class,
                    ClientboundGroundStuckKnockoutFx::encode,
                    ClientboundGroundStuckKnockoutFx::decode,
                    ClientboundGroundStuckKnockoutFx::handle
            );
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ItemProperties.register(
                        AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER.get(),
                        ResourceLocation.fromNamespaceAndPath(MODID, "second_form"),
                        (stack, level, entity, seed) -> {
                            if (stack.hasTag() && stack.getTag().getBoolean("SecondForm")) {
                                return 1.0F;
                            }
                            return 0.0F;
                        }
                );
                ItemProperties.register(
                        AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER.get(),
                        ResourceLocation.fromNamespaceAndPath(MODID, "snake_animation"),
                        (stack, level, entity, seed) -> {
                            if (stack.hasTag() && stack.getTag().getBoolean("SnakeAnimation")) {
                                return 1.0F;
                            }
                            return 0.0F;
                        }
                );
                ItemProperties.register(
                        AnnoyingVillagersModItems.DEMONIAC_VOLTAGE_REAVER.get(),
                        ResourceLocation.fromNamespaceAndPath(MODID, "snake_animation_ready"),
                        (stack, level, entity, seed) -> {
                            if (stack.hasTag() && stack.getTag().getInt("HitCount") == 5) {
                                return 1.0F;
                            }
                            return 0.0F;
                        }
                );
                ItemProperties.register(
                        AnnoyingVillagersModItems.ADVANCED_FISHING_ROD.get(),
                        ResourceLocation.fromNamespaceAndPath("minecraft", "cast"),
                        (stack, level, entity, seed) -> FishingRodGrappleUtil.getCastProperty(stack, entity)
                );
                ItemProperties.register(
                        AnnoyingVillagersModItems.TONY_THE_FISHING_ROD.get(),
                        ResourceLocation.fromNamespaceAndPath("minecraft", "cast"),
                        (stack, level, entity, seed) -> FishingRodGrappleUtil.getCastProperty(stack, entity)
                );
                ItemProperties.register(
                        AnnoyingVillagersModItems.HOOK_GUN.get(),
                        ResourceLocation.fromNamespaceAndPath(MODID, "hook"),
                        (stack, level, entity, seed) -> stack.hasTag() && stack.getTag().contains("hook") ? 1.0F : 0.0F
                );
                ItemProperties.register(
                        AnnoyingVillagersModItems.HOOK_GUN.get(),
                        ResourceLocation.fromNamespaceAndPath(MODID, "attached"),
                        (stack, level, entity, seed) -> entity != null
                                && HookGunItem.hasAttachedHook(entity.level(), entity) ? 1.0F : 0.0F
                );
            });
        }
    }
}
