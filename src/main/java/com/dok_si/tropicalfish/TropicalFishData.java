package com.dok_si.tropicalfish;

import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.DyeColor;
import java.util.*;

public class TropicalFishData {

    public static final int TOTAL_VARIANTS = 12 * 16 * 16; // 3072

    // Технические ключи форм (используются в путях перевода)
    private static final String[] PATTERN_KEYS = {
            "sunstreak", "snooper", "dasher", "brinely", "spotty", "glitter",
            "stripey", "blockfish", "betty", "clayfish", "flopper", "salmon"
    };

    // Технические ключи цветов
    private static final String[] COLOR_KEYS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "light_gray", "cyan", "purple", "blue",
            "brown", "green", "red", "black"
    };

    /**
     * Возвращает переведённое название формы (узора) в виде Text.
     */
    public static Text getPatternText(int patternIndex) {
        String key = PATTERN_KEYS[patternIndex % PATTERN_KEYS.length];
        return Text.translatable("tropicalfishcollection.pattern." + key);
    }

    /**
     * Возвращает переведённое название цвета в виде Text (без окраски).
     */
    public static Text getColorText(int colorIndex) {
        String key = COLOR_KEYS[colorIndex % COLOR_KEYS.length];
        return Text.translatable("tropicalfishcollection.color." + key);
    }

    /**
     * Возвращает переведённое название цвета, окрашенное в сам цвет.
     */
    public static Text getColoredColorText(int colorIndex) {
        DyeColor dye = DyeColor.values()[colorIndex % DyeColor.values().length];
        int rgb = dye.getFireworkColor();
        return getColorText(colorIndex).copy()
                .styled(style -> style.withColor(TextColor.fromRgb(rgb)));
    }

    // ---------- Кодирование ----------
    public static int encode(int patternIndex, int baseColorIndex, int patternColorIndex) {
        return (patternIndex << 16) | (baseColorIndex << 8) | patternColorIndex;
    }

    public static int getPatternIndex(int variant) { return (variant >> 16) & 0xFF; }
    public static int getBaseColorIndex(int variant) { return (variant >> 8) & 0xFF; }
    public static int getPatternColorIndex(int variant) { return variant & 0xFF; }

    /** RGB с полной альфой */
    public static int getBaseColor(int variant) {
        int index = getBaseColorIndex(variant);
        return 0xFF000000 | DyeColor.values()[index].getFireworkColor();
    }

    public static int getPatternColor(int variant) {
        int index = getPatternColorIndex(variant);
        return 0xFF000000 | DyeColor.values()[index].getFireworkColor();
    }

    public static String getVariantKey(int variant) {
        return getPatternIndex(variant) + "_" + getBaseColorIndex(variant) + "_" + getPatternColorIndex(variant);
    }

    // ---------- Генерация и сортировки ----------
    public static List<Integer> generateAllVariants() {
        List<Integer> list = new ArrayList<>(TOTAL_VARIANTS);
        for (TropicalFishEntity.Pattern pattern : TropicalFishEntity.Pattern.values()) {
            for (DyeColor base : DyeColor.values()) {
                for (DyeColor patternColor : DyeColor.values()) {
                    list.add(encode(pattern.ordinal(), base.ordinal(), patternColor.ordinal()));
                }
            }
        }
        return list;
    }

    public static void sortByColor(List<Integer> list) {
        list.sort(Comparator.comparingInt(TropicalFishData::getBaseColorIndex));
    }

    public static void sortByType(List<Integer> list) {
        list.sort(Comparator.comparingInt(TropicalFishData::getPatternIndex)
                .thenComparingInt(TropicalFishData::getBaseColorIndex));
    }
}