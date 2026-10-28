package com.sorrowmist.useless.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;

/** Double-sided world geometry, independent of the weather/lightning render target. */
final class ForgeEffectRenderType extends RenderType {
    static final RenderType EFFECT = create("useless_forge_effect", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 524288, false, true, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL).createCompositeState(false));
    private ForgeEffectRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int size,
                                  boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sorted, setup, clear);
    }
}
