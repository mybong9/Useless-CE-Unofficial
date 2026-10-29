package com.sorrowmist.useless.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Keeps optional integration targets out of Mixin's preparation phase when their mod is absent. */
public final class UselessMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.contains(".gtceu.")) return loaded("gtceu");
        if (mixin.contains(".tconstruct.")) return loaded("tconstruct");
        if (mixin.contains(".ftbultimine.")) return loaded("ftbultimine");
        if (mixin.contains(".emi.")) return loaded("emi");
        if (mixin.endsWith(".EaepBookmarkPatternMixin") || mixin.endsWith(".EaepBookmarkClientMixin")) return loaded("extendedae_plus");
        if (mixin.contains(".advancedae.")) return loaded("advanced_ae") || loaded("advancedae");
        if (mixin.contains(".eae.")) return loaded("expatternprovider");
        if (mixin.contains(".botanypots.") || mixin.endsWith(".BlockEntityBotanyPotAccessor")) return loaded("botanypots");
        if (mixin.contains(".create.")) return loaded("create");
        if (mixin.contains(".jade.")) return loaded("jade");
        if (mixin.contains(".malum.")) return loaded("malum");
        if (mixin.contains(".pneumaticcraft.")) return loaded("pneumaticcraft");
        if (mixin.contains(".itemobliterator.")) return loaded("item_obliterator");
        if (mixin.contains(".mek.")) return loaded("mekanism");
        return true;
    }
    private static boolean loaded(String id) {
        var list = FMLLoader.getLoadingModList();
        return list != null && list.getMods().stream().anyMatch(mod -> id.equals(mod.getModId()));
    }
    @Override public void onLoad(String pkg) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
}
