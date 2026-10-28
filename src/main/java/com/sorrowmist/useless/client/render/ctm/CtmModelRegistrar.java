package com.sorrowmist.useless.client.render.ctm;

import com.sorrowmist.useless.UselessMod;
import com.sorrowmist.useless.content.blocks.GlowPlasticBlock;
import com.sorrowmist.useless.content.blocks.multiblock.UselessCoilBlock;
import com.sorrowmist.useless.init.ModBlocks;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.ModelEvent;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class CtmModelRegistrar {
    private static final String FURNACE_TEXTURE_ROOT =
            "block/multiblock_alloy_furnace/";
    private static final String COIL_TEXTURE_ROOT = FURNACE_TEXTURE_ROOT + "coils/";
    private static final String PLASTIC_TEXTURE = "block/plastic_glow_block";
    private static final String PLASTIC_CTM_TEXTURE = "block/plastic_glow_block_ctm";

    private CtmModelRegistrar() {
    }

    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Function<Material, TextureAtlasSprite> textureGetter = Material::sprite;
        Supplier<TextureAtlasSprite> furnaceBase = sprite(textureGetter,
                FURNACE_TEXTURE_ROOT + "furnace_casing");
        Supplier<TextureAtlasSprite> furnaceCtm = sprite(textureGetter,
                FURNACE_TEXTURE_ROOT + "furnace_casing_ctm");

        wrapBlock(event.getModels(), ModBlocks.MULTIBLOCK_ALLOY_FURNACE_CORE.get(),
                furnaceBase, furnaceCtm);
        wrapBlock(event.getModels(), ModBlocks.OMNIVERSAL_FURNACE_CASING.get(),
                furnaceBase, furnaceCtm);
        wrapBlock(event.getModels(), ModBlocks.ME_PATTERN_ASSEMBLY.get(),
                sprite(textureGetter,FURNACE_TEXTURE_ROOT + "omniversal_forge_frame"),
                sprite(textureGetter,FURNACE_TEXTURE_ROOT + "omniversal_forge_frame_ctm"));
        wrapBlock(event.getModels(), ModBlocks.OMNIVERSAL_MOLD_HUB.get(),
                sprite(textureGetter,FURNACE_TEXTURE_ROOT + "omniversal_forge_frame"),
                sprite(textureGetter,FURNACE_TEXTURE_ROOT + "omniversal_forge_frame_ctm"));
        wrapBlock(event.getModels(), ModBlocks.PASSIVE_CRAFTING_HATCH.get(),
                sprite(textureGetter,FURNACE_TEXTURE_ROOT + "omniversal_forge_frame"),
                sprite(textureGetter,FURNACE_TEXTURE_ROOT + "omniversal_forge_frame_ctm"));

        wrapBlock(event.getModels(), ModBlocks.OMNIVERSAL_FORGE_FRAME.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_forge_frame"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_forge_frame_ctm"));
        wrapBlock(event.getModels(), ModBlocks.OMNIVERSAL_FORGE_VENT.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_forge_vent"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_forge_vent_ctm"));
        wrapBlock(event.getModels(),ModBlocks.OMNIVERSAL_FORGE_TRIM.get(),
                sprite(textureGetter,FURNACE_TEXTURE_ROOT + "omniversal_forge_trim"),
                sprite(textureGetter,FURNACE_TEXTURE_ROOT + "omniversal_forge_trim_ctm"));
        wrapBlock(event.getModels(), ModBlocks.OMNIVERSAL_FORGE_RESONATOR.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_forge_resonator"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_forge_resonator_ctm"));
        wrapBlock(event.getModels(), ModBlocks.OMNIVERSAL_FORGE_ARMOR.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_forge_armor"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_forge_armor_ctm"));
        wrapBlock(event.getModels(), ModBlocks.OMNIVERSAL_FORGE_COPPER.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_forge_copper"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_forge_copper_ctm"));
        wrapBlock(event.getModels(), ModBlocks.TRIGRAM_QIAN.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_qian"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_qian_ctm"));
        wrapBlock(event.getModels(), ModBlocks.TRIGRAM_DUI.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_dui"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_dui_ctm"));
        wrapBlock(event.getModels(), ModBlocks.TRIGRAM_LI.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_li"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_li_ctm"));
        wrapBlock(event.getModels(), ModBlocks.TRIGRAM_ZHEN.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_zhen"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_zhen_ctm"));
        wrapBlock(event.getModels(), ModBlocks.TRIGRAM_KUN.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_kun"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_kun_ctm"));
        wrapBlock(event.getModels(), ModBlocks.TRIGRAM_GEN.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_gen"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_gen_ctm"));
        wrapBlock(event.getModels(), ModBlocks.TRIGRAM_KAN.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_kan"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_kan_ctm"));
        wrapBlock(event.getModels(), ModBlocks.TRIGRAM_XUN.get(),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_xun"),
                sprite(textureGetter, FURNACE_TEXTURE_ROOT + "omniversal_trigram_xun_ctm"));

        ModBlocks.USELESS_COILS.forEach((tier, holder) -> {
            String name = UselessCoilBlock.registryName(tier);
            wrapBlock(event.getModels(), holder.get(),
                    sprite(textureGetter, COIL_TEXTURE_ROOT + name),
                    sprite(textureGetter, COIL_TEXTURE_ROOT + name + "_ctm"),
                    sprite(textureGetter, COIL_TEXTURE_ROOT + name + "_active"),
                    sprite(textureGetter, COIL_TEXTURE_ROOT + name + "_active_ctm"));
        });

        Supplier<TextureAtlasSprite> plasticBase = sprite(textureGetter, PLASTIC_TEXTURE);
        Supplier<TextureAtlasSprite> plasticCtm = sprite(textureGetter, PLASTIC_CTM_TEXTURE);
        GlowPlasticBlock.PLASTIC_CTM_BLOCKS.values().forEach(block ->
                wrapBlock(event.getModels(), block.get(), plasticBase, plasticCtm));
        GlowPlasticBlock.GLOW_PLASTIC_CTM_BLOCKS.values().forEach(block ->
                wrapBlock(event.getModels(), block.get(), plasticBase, plasticCtm));
    }

    private static Supplier<TextureAtlasSprite> sprite(
            Function<Material, TextureAtlasSprite> textureGetter, String path) {
        ResourceLocation texture = new ResourceLocation(
                UselessMod.MODID, path);
        // ModifyBakingResult runs before atlas upload in Forge 1.20.1.
        // Resolve from the active atlas only when the baked model is rendered.
        Material material = new Material(InventoryMenu.BLOCK_ATLAS, texture);
        return new Supplier<>() {
            private volatile TextureAtlasSprite resolved;

            @Override
            public TextureAtlasSprite get() {
                TextureAtlasSprite sprite = resolved;
                if (sprite == null) resolved = sprite = textureGetter.apply(material);
                return sprite;
            }
        };
    }

    private static void wrapBlock(
            Map<ResourceLocation, BakedModel> models, Block block,
            Supplier<TextureAtlasSprite> baseSprite, Supplier<TextureAtlasSprite> ctmSprite) {
        wrapBlock(models, block, baseSprite, ctmSprite, null, null);
    }

    private static void wrapBlock(
            Map<ResourceLocation, BakedModel> models, Block block,
            Supplier<TextureAtlasSprite> baseSprite, Supplier<TextureAtlasSprite> ctmSprite,
            Supplier<TextureAtlasSprite> overlayBaseSprite, Supplier<TextureAtlasSprite> overlayCtmSprite) {
        for (var state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            models.computeIfPresent(location, (ignored, model) ->
                    model instanceof CtmBakedModel
                            ? model
                            : new CtmBakedModel(model, baseSprite, ctmSprite,
                                    overlayBaseSprite, overlayCtmSprite));
        }
    }
}
