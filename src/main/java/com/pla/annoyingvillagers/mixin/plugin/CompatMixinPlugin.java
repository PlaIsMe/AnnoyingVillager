package com.pla.annoyingvillagers.mixin.plugin;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class CompatMixinPlugin implements IMixinConfigPlugin {
    private static final String SMART_NPC_MOD_ID = "smart_npc";
    private static final String SOUL_FIRE_D_MOD_ID = "soul_fire_d";
    private static final String SMART_NPC_COMPAT_PREFIX = "com.pla.annoyingvillagers.mixin.compat.smartnpc.";
    private static final String PUNCHY_COMPAT_PREFIX = "com.pla.annoyingvillagers.mixin.compat.punchy.";
    private static final String END_FIRE_RENDER_MIXIN_PREFIX = "com.pla.annoyingvillagers.mixin.client.EndFire";

    private static boolean isModLoadedEarly(String modId) {
        LoadingModList list = FMLLoader.getLoadingModList();
        return list != null && list.getModFileById(modId) != null;
    }

    private static boolean canApplyCompat(String modId, String targetClassName) {
        return isModLoadedEarly(modId) && isClassAvailable(targetClassName);
    }

    private static boolean isClassAvailable(String className) {
        try {
            MixinService.getService().getBytecodeProvider().getClassNode(className);
            return true;
        } catch (ClassNotFoundException | IOException | RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(PUNCHY_COMPAT_PREFIX)) {
            return canApplyCompat("punchy", targetClassName);
        }
        if (mixinClassName.startsWith(SMART_NPC_COMPAT_PREFIX)) {
            return canApplyCompat(SMART_NPC_MOD_ID, targetClassName);
        }
        if (mixinClassName.startsWith(END_FIRE_RENDER_MIXIN_PREFIX)) {
            return !isModLoadedEarly(SOUL_FIRE_D_MOD_ID);
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
