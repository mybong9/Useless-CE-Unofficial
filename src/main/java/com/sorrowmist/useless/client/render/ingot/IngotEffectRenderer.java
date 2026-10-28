package com.sorrowmist.useless.client.render.ingot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Texture-masked, full-bright nebula, rim light and bounded orbiting sparks, with no custom shader dependency. */
public final class IngotEffectRenderer extends BlockEntityWithoutLevelRenderer {
    private static IngotEffectRenderer renderer;
    private static final float[][] CIRCLE = new float[13][2];
    static {
        for (int i = 0; i <= 12; i++) {
            CIRCLE[i][0] = (float)Math.cos(i * Math.PI / 6);
            CIRCLE[i][1] = (float)Math.sin(i * Math.PI / 6);
        }
    }
    private static final int[] COLORS = {0x65e3b0, 0xb192fa, 0xf477ad, 0x88d9ef,
            0xf3a8ee, 0xc79cfa, 0xa183e0, 0x76abff, 0xffcf74, 0xe5a2ff};
    record Pixel(float x, float y, float size, int color, boolean edge) {}
    static final class Material {
        final BakedModel model;
        Pixel[] pixels;
        int tint;
        Material(BakedModel model) { this.model = model; }
        Pixel[] pixels() {
            if (pixels != null) return pixels;
            var sprite = model.getParticleIcon().contents();
            int w = Math.min(32, sprite.width()), h = Math.min(32, sprite.height());
            boolean[][] mask = new boolean[w][h];
            for (int x = 0; x < w; x++) for (int y = 0; y < h; y++)
                mask[x][y] = !sprite.isTransparent(0, x * sprite.width() / w, y * sprite.height() / h);
            var result = new ArrayList<Pixel>();
            long red = 0, green = 0, blue = 0, weight = 0;
            for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
                if (!mask[x][y]) continue;
                boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1
                        || !mask[x-1][y] || !mask[x+1][y] || !mask[x][y-1] || !mask[x][y+1];
                int abgr = sprite.getOriginalImage().getPixelRGBA(x * sprite.width() / w, y * sprite.height() / h);
                int rgb = (abgr & 255) << 16 | abgr & 0xff00 | (abgr >>> 16 & 255);
                int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                // Saturated pixels describe the material; neutral white highlights should not bleach its hue.
                int saturation = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
                int importance = 1 + saturation;
                red += (long)r * importance; green += (long)g * importance; blue += (long)b * importance;
                weight += importance;
                result.add(new Pixel((x + .5f) / w, 1 - (y + .5f) / h, 1f / Math.max(w, h), rgb, edge));
            }
            if (weight > 0) {
                int r = (int)(red / weight), g = (int)(green / weight), b = (int)(blue / weight);
                double gain = 225.0 / Math.max(1, Math.max(r, Math.max(g, b)));
                tint = (int)(r * gain) << 16 | (int)(g * gain) << 8 | (int)(b * gain);
            }
            return pixels = result.toArray(Pixel[]::new);
        }
    }
    private IngotEffectRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }
    public static IngotEffectRenderer instance() {
        if (renderer == null) renderer = new IngotEffectRenderer();
        return renderer;
    }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay) {
        var material = IngotEffects.MATERIALS.get(stack.getItem());
        if (material == null) return;
        var mc = Minecraft.getInstance();
        // ItemRenderer already applied the model transform and (-.5,-.5,-.5). Never re-enter renderStatic.
        mc.getItemRenderer().renderModelLists(material.model, stack, light, overlay, pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)));
        int tier = ((com.sorrowmist.useless.content.items.IngotItem) stack.getItem()).getFurnaceTier();
        var pixels = material.pixels();
        int color = material.tint != 0 ? material.tint : COLORS[Math.min(COLORS.length - 1, Math.max(0, tier - 1))];
        double time = mc.level == null ? Util.getMillis() / 1000.0
                : (mc.level.getGameTime() + mc.getFrameTime()) / 20.0;
        time += tier * .71;
        var out = buffers.getBuffer(IngotEffectRenderType.GLOW);
        var matrix = pose.last().pose();
        // Both faces receive the same texture mask, so dropped and framed ingots work from either side.
        for (int face = 0; face < 2; face++) {
            float z = face == 0 ? .465f : .535f;
            for (var p : pixels) {
                double wave = Math.sin(p.x * 10 + Math.sin(p.y * 8 + time * .65) * 1.9 - time * .85);
                double cloud = .5 + .5 * Math.sin(p.y * 13 - p.x * 7 + time * .55 + wave * 1.4);
                // All ingots share the useful ingot's gradual fade. No sign-based hue switch or generic purple tint.
                double gradient = .5 + .5 * Math.sin(p.x * 4.8 - p.y * 3.6 - time * .9 + wave * .6);
                int deep = mix(color, 0x100d22, .42);
                int luminous = mix(color, 0xffffff, .26 + cloud * .30);
                int nebula = mix(deep, luminous, gradient);
                // Thin clouds preserve the original metal's texture, highlights and tier color.
                square(out, matrix, p.x, p.y, z, p.size * .505f,
                        mix(p.color, nebula, .78), .32 + cloud * .30);
                if (p.edge) {
                    double sweep = Math.pow(.5 + .5 * Math.sin(p.x * 6 + p.y * 8 - time * 1.2), 9);
                    square(out, matrix, p.x, p.y, z, p.size * .505f,
                            mix(color, 0xffffff, .62), .12 + sweep * .64);
                    glow(out, matrix, p.x, p.y, z, p.size * 1.6f, color, .035 + sweep * .025);
                }
                double sparkle = Math.pow(Math.max(0, Math.sin(p.x * 139 + p.y * 83 + time * 1.4)), 46);
                if (sparkle > .55) square(out, matrix, p.x, p.y, z, p.size * .10f, 0xf3ffff, sparkle * .9);
            }
            // A fixed twelve particles: no particles/entities allocated into the world or GUI.
            for (int i = 0; i < 12; i++) {
                double phase = time * (.18 + (i % 3) * .035) + i * 2.39996;
                float x = .5f + (float)Math.cos(phase) * (.42f + i % 3 * .055f);
                float y = .5f + (float)Math.sin(phase * 1.13) * (.26f + i % 4 * .035f);
                double alpha = .25 + .48 * Math.pow(.5 + .5 * Math.sin(time * 1.5 + i), 2);
                glow(out, matrix, x, y, z, .052f, color, alpha * .18);
                square(out, matrix, x, y, z, .009f + (i % 3) * .0025f,
                        mix(color, 0xffffff, .55), alpha);
            }
        }
    }
    private static int mix(int a, int b, double factor) {
        int r = (int)((a >> 16 & 255) * (1-factor) + (b >> 16 & 255) * factor);
        int g = (int)((a >> 8 & 255) * (1-factor) + (b >> 8 & 255) * factor);
        int bl = (int)((a & 255) * (1-factor) + (b & 255) * factor);
        return r << 16 | g << 8 | bl;
    }
    private static void square(VertexConsumer out, Matrix4f m, float x, float y, float z, float size, int color, double alpha) {
        vertex(out,m,x-size,y-size,z,color,alpha); vertex(out,m,x+size,y-size,z,color,alpha);
        vertex(out,m,x+size,y+size,z,color,alpha); vertex(out,m,x-size,y+size,z,color,alpha);
    }
    private static void glow(VertexConsumer out, Matrix4f m, float x, float y, float z, float radius, int color, double alpha) {
        for (int j = 0; j < 12; j++) {
            vertex(out,m,x,y,z,color,alpha);
            vertex(out,m,x+CIRCLE[j][0]*radius,y+CIRCLE[j][1]*radius,z,color,0);
            vertex(out,m,x+CIRCLE[j+1][0]*radius,y+CIRCLE[j+1][1]*radius,z,color,0);
            vertex(out,m,x,y,z,color,alpha);
        }
    }
    private static void vertex(VertexConsumer out, Matrix4f m, float x, float y, float z, int c, double a) {
        out.vertex(m,x,y,z).color((c>>16&255)/255f,(c>>8&255)/255f,(c&255)/255f,(float)a).endVertex();
    }
}
