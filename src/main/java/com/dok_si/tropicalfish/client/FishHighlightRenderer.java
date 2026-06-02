package com.dok_si.tropicalfish.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Подсветка рыб теперь реализована через FishGlowMixin — переопределение isGlowing().
 * Этот класс оставлен пустым для совместимости с вызовами из TropicalFishCollectionClient.
 */
public class FishHighlightRenderer {

    /** Больше не нужен — подсветка работает через FishGlowMixin. */
    public static void tickHighlight() {
        // no-op: логика перенесена в FishGlowMixin.onIsGlowing()
    }

    /** Оставлен для совместимости. */
    public static void clearHighlight() {
        // no-op
    }

    /** Оставлен для совместимости. */
    public static void render(MatrixStack matrices,
                              VertexConsumerProvider consumers,
                              double camX, double camY, double camZ) {
        // no-op
    }
}