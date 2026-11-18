package com.sorrowmist.useless.mixin.neoeco;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Keeps Neo Eco integration optional on installations without the mod. */
public final class NeoEcoMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var mods = FMLLoader.getLoadingModList();
        var eco = mods.getModFileById("neoecoae");
        if (eco == null) return false;
        if (mixinClassName.endsWith(".CrazyAeEcoOutputMixin")) {
            // The early insertion bridge was removed in ECO 20.5.8.
            if (eco.getMods().stream().noneMatch(mod ->
                    mod.getModId().equals("neoecoae") && mod.getVersion().toString().equals("20.5.7"))) {
                return false;
            }
            var file = mods.getModFileById("crazyae2addons");
            return file != null && file.getMods().stream().anyMatch(mod ->
                    mod.getModId().equals("crazyae2addons") && mod.getVersion().toString().equals("2.6.2"));
        }
        return true;
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String name, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String name, IMixinInfo info) {}
}
