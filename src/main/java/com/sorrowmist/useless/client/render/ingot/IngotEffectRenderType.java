package com.sorrowmist.useless.client.render.ingot;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;

final class IngotEffectRenderType extends RenderType {
    static final RenderType GLOW = create("useless_ingot_nebula", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 65536, false, true, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL).createCompositeState(false));
    private IngotEffectRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int size,
                                 boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sorted, setup, clear);
    }
}
