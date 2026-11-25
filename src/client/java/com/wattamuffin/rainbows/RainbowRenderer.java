// ██████╗  █████╗ ██╗███╗   ██╗██████╗  ██████╗ ██╗    ██╗███████╗██╗
// ██╔══██╗██╔══██╗██║████╗  ██║██╔══██╗██╔═══██╗██║    ██║██╔════╝██║
// ██████╔╝███████║██║██╔██╗ ██║██████╔╝██║   ██║██║ █╗ ██║███████╗██║
// ██╔══██╗██╔══██║██║██║╚██╗██║██╔══██╗██║   ██║██║███╗██║╚════██║╚═╝
// ██║  ██║██║  ██║██║██║ ╚████║██████╔╝╚██████╔╝╚███╔███╔╝███████║██╗
// ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝╚═════╝  ╚═════╝  ╚══╝╚══╝ ╚══════╝╚═╝
package com.wattamuffin.rainbows;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class RainbowRenderer {
    private record PixelQuad(float x0, float y0, float x1, float y1, float r, float g, float b, float edgeFade) {}

    private static final Identifier WHITE_TEXTURE = Identifier.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType RAINBOW_RENDER_TYPE = RenderTypes.entityTranslucent(WHITE_TEXTURE);

    private static final int[] DAY_COLORS = {
        0xFF3B30, // Red
        0xFF9500, // Orange
        0xFFCC00, // Yellow
        0x34C759, // Green
        0x32ADE6, // Light Blue
        0x007AFF, // Selene Studios Easter Egg
        0xAF52DE  // by wattamuffin
    };

    private static final int[] MOONBOW_COLORS = {
        0xC8D0DC,
        0xBCC5D4,
        0xB0BACB,
        0xA5B0C2,
        0x9AA6B9,
        0x909DB1,
        0x8693A8
    };

    private static final float DISTANCE = 140.0f;
    private static final float BASE_PIXEL_SIZE = 2.7f;
    private static final int BASE_RADIUS_PX = 26;
    private static final int BORDER_LAYERS = 2;
    private static final int MOONBOW_BORDER_LAYERS = 5;

    private static PixelQuad[] cachedQuads = null;
    private static RainbowsConfig.DetailLevel cachedDetail = null;
    private static boolean cachedMoonbow = false;
    private static float cachedMoonbowColorFactor = -1.0f;

    private static PixelQuad[] getOrBuildCache(RainbowsConfig.DetailLevel detail, boolean moonbow, float moonbowColorFactor) {
        if (cachedQuads != null
            && cachedDetail == detail
            && cachedMoonbow == moonbow
            && (!moonbow || Math.abs(cachedMoonbowColorFactor - moonbowColorFactor) < 0.0001f)) {
            return cachedQuads;
        }

        int scale = detail.getScale();
        boolean smooth = detail.isSmooth() || moonbow;
        int[] palette = moonbow ? MOONBOW_COLORS : DAY_COLORS;
        int borderLayers = moonbow ? MOONBOW_BORDER_LAYERS : BORDER_LAYERS;

        float pixelSize = BASE_PIXEL_SIZE / scale;
        double innerRadius = BASE_RADIUS_PX * scale;
        double bandThickness = palette.length * scale;
        double outerRadius = innerRadius + bandThickness;
        double borderThickness = borderLayers * scale;
        double minRadius = innerRadius - borderThickness;
        double maxRadius = outerRadius + borderThickness;
        double centerRadius = (innerRadius + outerRadius) * 0.5;
        double halfSpan = (maxRadius - minRadius) * 0.5;
        int bound = (int) Math.ceil(maxRadius);

        List<PixelQuad> list = new ArrayList<>();

        for (int gridY = -bound; gridY <= bound; gridY++) {
            for (int gridX = -bound; gridX <= bound; gridX++) {
                double dist = smooth
                    ? Math.hypot(gridX + 0.5, gridY + 0.5)
                    : Math.hypot(gridX, gridY);
                if (dist < minRadius || dist >= maxRadius) continue;

                float edgeFade;
                float r, g, b;

                if (moonbow) {
                    float t = Mth.clamp((float) (1.0 - Math.abs(dist - centerRadius) / halfSpan), 0.0f, 1.0f);
                    edgeFade = t * t * (3.0f - 2.0f * t);

                    double norm = Mth.clamp((dist - innerRadius) / bandThickness, 0.0, 1.0);
                    double colorPos = (1.0 - norm) * (MOONBOW_COLORS.length - 1);
                    int idx0 = Mth.clamp((int) Math.floor(colorPos), 0, MOONBOW_COLORS.length - 1);
                    int idx1 = Mth.clamp(idx0 + 1, 0, MOONBOW_COLORS.length - 1);
                    float frac = (float) (colorPos - idx0);

                    int m0 = MOONBOW_COLORS[idx0];
                    int m1 = MOONBOW_COLORS[idx1];
                    float mr = Mth.lerp(frac, ((m0 >> 16) & 0xFF) / 255.0f, ((m1 >> 16) & 0xFF) / 255.0f);
                    float mg = Mth.lerp(frac, ((m0 >> 8) & 0xFF) / 255.0f, ((m1 >> 8) & 0xFF) / 255.0f);
                    float mb = Mth.lerp(frac, (m0 & 0xFF) / 255.0f, (m1 & 0xFF) / 255.0f);

                    int d0 = DAY_COLORS[idx0];
                    int d1 = DAY_COLORS[idx1];
                    float dr = Mth.lerp(frac, ((d0 >> 16) & 0xFF) / 255.0f, ((d1 >> 16) & 0xFF) / 255.0f);
                    float dg = Mth.lerp(frac, ((d0 >> 8) & 0xFF) / 255.0f, ((d1 >> 8) & 0xFF) / 255.0f);
                    float db = Mth.lerp(frac, (d0 & 0xFF) / 255.0f, (d1 & 0xFF) / 255.0f);

                    r = Mth.lerp(moonbowColorFactor, mr, dr);
                    g = Mth.lerp(moonbowColorFactor, mg, dg);
                    b = Mth.lerp(moonbowColorFactor, mb, db);
                } else if (dist < innerRadius) {
                    float t = Mth.clamp((float) ((dist - minRadius) / borderThickness), 0.0f, 1.0f);
                    edgeFade = smooth ? (t * t * (3.0f - 2.0f * t) * 0.55f) : (t * 0.45f);
                    int c = palette[palette.length - 1];
                    r = ((c >> 16) & 0xFF) / 255.0f;
                    g = ((c >> 8) & 0xFF) / 255.0f;
                    b = (c & 0xFF) / 255.0f;
                } else if (dist >= outerRadius) {
                    float t = Mth.clamp((float) ((maxRadius - dist) / borderThickness), 0.0f, 1.0f);
                    edgeFade = smooth ? (t * t * (3.0f - 2.0f * t) * 0.55f) : (t * 0.45f);
                    int c = palette[0];
                    r = ((c >> 16) & 0xFF) / 255.0f;
                    g = ((c >> 8) & 0xFF) / 255.0f;
                    b = (c & 0xFF) / 255.0f;
                } else {
                    edgeFade = 1.0f;
                    if (smooth) {
                        double norm = (dist - innerRadius) / bandThickness;
                        double colorPos = (1.0 - norm) * (palette.length - 1);
                        int idx0 = Mth.clamp((int) Math.floor(colorPos), 0, palette.length - 1);
                        int idx1 = Mth.clamp(idx0 + 1, 0, palette.length - 1);
                        float frac = (float) (colorPos - idx0);

                        int c0 = palette[idx0];
                        int c1 = palette[idx1];
                        r = Mth.lerp(frac, ((c0 >> 16) & 0xFF) / 255.0f, ((c1 >> 16) & 0xFF) / 255.0f);
                        g = Mth.lerp(frac, ((c0 >> 8) & 0xFF) / 255.0f, ((c1 >> 8) & 0xFF) / 255.0f);
                        b = Mth.lerp(frac, (c0 & 0xFF) / 255.0f, (c1 & 0xFF) / 255.0f);
                    } else {
                        int bandFromInner = (int) ((dist - innerRadius) / scale);
                        int band = Mth.clamp(palette.length - 1 - bandFromInner, 0, palette.length - 1);
                        int c = palette[band];
                        r = ((c >> 16) & 0xFF) / 255.0f;
                        g = ((c >> 8) & 0xFF) / 255.0f;
                        b = (c & 0xFF) / 255.0f;
                    }
                }

                if (edgeFade <= 0.01f) continue;

                float x0 = gridX * pixelSize;
                float y0 = gridY * pixelSize;
                list.add(new PixelQuad(x0, y0, x0 + pixelSize, y0 + pixelSize, r, g, b, edgeFade));
            }
        }

        cachedQuads = list.toArray(PixelQuad[]::new);
        cachedDetail = detail;
        cachedMoonbow = moonbow;
        cachedMoonbowColorFactor = moonbowColorFactor;
        return cachedQuads;
    }

    public static void submit(LevelRenderContext context, float alpha, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        RainbowsConfig config = RainbowsConfig.get();
        boolean moonbow = RainbowState.isMoonbow();
        float moonbowColorFactor = moonbow
            ? (config.saturation / 100.0f) * (config.moonbowSaturation / 100.0f)
            : 0.0f;

        PixelQuad[] quads = getOrBuildCache(config.detailLevel, moonbow, moonbowColorFactor);

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        Matrix4f rotation = new Matrix4f()
            .rotateY((float) Math.toRadians(RainbowState.getRainbowYaw()));
        poseStack.mulPose(rotation);
        poseStack.translate(0.0f, -15.0f, -DISTANCE);

        context.submitNodeCollector().submitCustomGeometry(
            poseStack,
            RAINBOW_RENDER_TYPE,
            (pose, consumer) -> {
                for (PixelQuad q : quads) {
                    float pixelAlpha = alpha * q.edgeFade();
                    if (pixelAlpha <= 0.003f) continue;

                    consumer.addVertex(pose, q.x0(), q.y0(), 0.0f)
                        .setColor(q.r(), q.g(), q.b(), pixelAlpha)
                        .setUv(0.0f, 0.0f)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(pose, 0.0f, 0.0f, 1.0f);

                    consumer.addVertex(pose, q.x1(), q.y0(), 0.0f)
                        .setColor(q.r(), q.g(), q.b(), pixelAlpha)
                        .setUv(1.0f, 0.0f)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(pose, 0.0f, 0.0f, 1.0f);

                    consumer.addVertex(pose, q.x1(), q.y1(), 0.0f)
                        .setColor(q.r(), q.g(), q.b(), pixelAlpha)
                        .setUv(1.0f, 1.0f)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(pose, 0.0f, 0.0f, 1.0f);

                    consumer.addVertex(pose, q.x0(), q.y1(), 0.0f)
                        .setColor(q.r(), q.g(), q.b(), pixelAlpha)
                        .setUv(0.0f, 1.0f)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(pose, 0.0f, 0.0f, 1.0f);
                }
            }
        );

        poseStack.popPose();
    }
}