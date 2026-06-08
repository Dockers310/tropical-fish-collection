package com.dok_si.tropicalfish.client;

import com.dok_si.tropicalfish.TropicalFishCollection;
import com.dok_si.tropicalfish.TropicalFishData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Генерирует 2D спрайты тропических рыб из ванильных текстур.
 *
 * Vanilla texture layout (tropical_a.png / tropical_b.png):
 *   Каждый файл 64x32px. Содержит body texture в левой половине.
 *   Отдельные pattern файлы: tropical_a_pattern_N.png / tropical_b_pattern_N.png
 *
 * Алгоритм:
 *   1. Загружаем base texture (tropical_a или tropical_b)
 *   2. Загружаем pattern texture для нужного паттерна
 *   3. Tint base в baseColor, pattern в patternColor
 *   4. Накладываем pattern поверх base
 *   5. Регистрируем как NativeImageBackedTexture
 */
public class TropicalFishSprite {

    // Кэш: variant -> Identifier зарегистрированной текстуры
    private static final Map<Integer, Identifier> CACHE = new HashMap<>();

    // Vanilla DyeColor -> RGB (firework color)
    private static final int[] DYE_COLORS = new int[16];

    static {
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < 16 && i < dyes.length; i++) {
            DYE_COLORS[i] = dyes[i].getFireworkColor();
        }
    }

    /**
     * Возвращает Identifier текстуры для данного варианта.
     * Генерирует и кэширует при первом обращении.
     * Возвращает null если не удалось загрузить текстуры.
     */
    public static Identifier getOrCreate(int variant) {
        return CACHE.computeIfAbsent(variant, TropicalFishSprite::generate);
    }

    private static Identifier generate(int variant) {
        try {
            int patternIndex      = TropicalFishData.getPatternIndex(variant);
            int baseColorIndex    = TropicalFishData.getBaseColorIndex(variant);
            int patternColorIndex = TropicalFishData.getPatternColorIndex(variant);

            // shape: паттерны 0..5 = small (a), 6..11 = large (b)
            // Но в vanilla shape определяется отдельно.
            // Используем стандартную маппинг паттернов:
            // Vanilla patterns: KOB(0), SUNSTREAK(1), SNOOPER(2), DASHER(3),
            //   BRINELY(4), SPOTTY(5), FLOPPER(6), STRIPEY(7), GLITTER(8),
            //   BLOCKFISH(9), BETTY(10), CLAYFISH(11)
            // Shape A (small): 0-5, Shape B (large): 6-11
            boolean isLarge = isLargePattern(patternIndex);
            String shapeChar = isLarge ? "b" : "a";

            // Пути к ванильным текстурам
            String basePath    = "textures/entity/fish/tropical_" + shapeChar + ".png";
            String patternPath = "textures/entity/fish/tropical_" + shapeChar
                    + "_pattern_" + (patternIndex % 6 + 1) + ".png";

            NativeImage base    = loadVanillaTexture(basePath);
            NativeImage pattern = loadVanillaTexture(patternPath);

            if (base == null) return null;

            // Размер спрайта — берём левую часть (front view) текстуры
            // Vanilla fish textures: 64x32, front face примерно 16x16 начиная с (0,0)
            int spriteW = 16;
            int spriteH = 16;

            NativeImage result = new NativeImage(spriteW, spriteH, true);

            int baseRgb    = DYE_COLORS[baseColorIndex];
            int patternRgb = DYE_COLORS[patternColorIndex];

            // Рисуем base слой с tint
            for (int y = 0; y < spriteH; y++) {
                for (int x = 0; x < spriteW; x++) {
                    int px = safeGetPixel(base, x, y);
                    if (px == 0) continue;
                    int tinted = tint(px, baseRgb);
                    result.setColorArgb(x, y, tinted);
                }
            }

            // Рисуем pattern слой поверх
            if (pattern != null) {
                for (int y = 0; y < spriteH; y++) {
                    for (int x = 0; x < spriteW; x++) {
                        int px = safeGetPixel(pattern, x, y);
                        if (getAlpha(px) < 10) continue;
                        int tinted = tint(px, patternRgb);
                        result.setColorArgb(x, y, blend(result.getColorArgb(x, y), tinted));
                    }
                }
            }

            base.close();
            if (pattern != null) pattern.close();

            // Регистрируем текстуру
            Identifier id = Identifier.of(TropicalFishCollection.MOD_ID,
                    "fish_sprite/" + variant);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "tfc_fish/" + variant, result);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);

            return id;

        } catch (Exception e) {
            TropicalFishCollection.LOGGER.warn("[TFC] Failed to generate sprite for variant {}: {}", variant, e.getMessage());
            return null;
        }
    }

    /**
     * Vanilla pattern → shape mapping.
     * KOB=0(small), SUNSTREAK=1(small), SNOOPER=2(small), DASHER=3(small), BRINELY=4(small), SPOTTY=5(small),
     * FLOPPER=6(large), STRIPEY=7(large), GLITTER=8(large), BLOCKFISH=9(large), BETTY=10(large), CLAYFISH=11(large)
     */
    private static boolean isLargePattern(int patternIndex) {
        // В vanilla TropicalFishEntity.Pattern: первые 6 = small body, последние 6 = large body
        return patternIndex >= 6;
    }

    private static NativeImage loadVanillaTexture(String path) {
        try {
            var manager = MinecraftClient.getInstance().getResourceManager();
            var resource = manager.getResource(Identifier.ofVanilla(path));
            if (resource.isEmpty()) return null;
            try (InputStream stream = resource.get().getInputStream()) {
                return NativeImage.read(stream);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static int safeGetPixel(NativeImage img, int x, int y) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return 0;
        return img.getColorArgb(x, y);
    }

    /**
     * Окрашивает пиксель в нужный цвет сохраняя яркость оригинала.
     * Multiplies RGB channels с target color.
     */
    private static int tint(int pixel, int targetRgb) {
        int a = getAlpha(pixel);
        if (a == 0) return 0;

        float r = getRed(pixel)   / 255f;
        float g = getGreen(pixel) / 255f;
        float b = getBlue(pixel)  / 255f;

        float tr = ((targetRgb >> 16) & 0xFF) / 255f;
        float tg = ((targetRgb >> 8)  & 0xFF) / 255f;
        float tb = (targetRgb & 0xFF)          / 255f;

        int nr = (int)(r * tr * 255);
        int ng = (int)(g * tg * 255);
        int nb = (int)(b * tb * 255);

        return (a << 24) | (nr << 16) | (ng << 8) | nb;
    }

    /** Alpha-blend src поверх dst. */
    private static int blend(int dst, int src) {
        int sa = getAlpha(src);
        if (sa == 0) return dst;
        if (sa == 255) return src;
        float alpha = sa / 255f;
        int r = (int)(getRed(src) * alpha + getRed(dst) * (1 - alpha));
        int g = (int)(getGreen(src) * alpha + getGreen(dst) * (1 - alpha));
        int b = (int)(getBlue(src) * alpha + getBlue(dst) * (1 - alpha));
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }

    private static int getAlpha(int argb) { return (argb >> 24) & 0xFF; }
    private static int getRed(int argb)   { return (argb >> 16) & 0xFF; }
    private static int getGreen(int argb) { return (argb >> 8)  & 0xFF; }
    private static int getBlue(int argb)  { return argb & 0xFF; }

    /** Очистить кэш (при смене ресурс-паков). */
    public static void clearCache() {
        CACHE.clear();
    }

}