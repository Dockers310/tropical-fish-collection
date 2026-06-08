package com.dok_si.tropicalfish.client;

import com.dok_si.tropicalfish.PlayerCollectionState;
import com.dok_si.tropicalfish.TropicalFishCollection;
import com.dok_si.tropicalfish.TropicalFishData;
import com.dok_si.tropicalfish.config.YACLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import net.minecraft.screen.slot.Slot;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BucketDecorator {

    private static final KeyBinding.Category CATEGORY =
            new KeyBinding.Category(Identifier.of(TropicalFishCollection.MOD_ID, "category"));

    public static final KeyBinding TOGGLE_INDICATORS = new KeyBinding(
            "key.tropicalfishcollection.toggle_indicators",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            CATEGORY
    );

    // variant -> сколько раз встречен в текущем контейнере за этот кадр
    private static final Map<Integer, Integer> seenThisFrame = new HashMap<>();

    // Снимок коллекции на момент открытия контейнера
    private static Set<Integer> collectionSnapshot = Set.of();

    // Вариант под курсором мыши в текущем кадре (-1 = нет)
    private static int hoveredVariant = -1;
    public static void setHoveredSlot(Slot slot) {
        hoveredSlot = slot;
    }
    public static Slot getHoveredSlot() {
        return hoveredSlot;
    }
    private static Slot hoveredSlot = null;
    // ── Сброс в начале каждого кадра ────────────────────────────────────

    public static void resetForNewContainer() {
        seenThisFrame.clear();
        hoveredVariant = -1;
        hoveredSlot = null;
        collectionSnapshot = new java.util.HashSet<>(PlayerCollectionState.getCollected());
    }

    // ── Hover-подсветка ──────────────────────────────────────────────────

    /** Запомнить вариант под курсором (вызывается из afterRender). */
    public static void setHoveredVariant(int variant) {
        hoveredVariant = variant;
    }

    /** Вариант под курсором в текущем кадре. */
    public static int getHoveredVariant() {
        return hoveredVariant;
    }

    /**
     * Цвет подсветки для слота на который НАВЕЛИ:
     *   Зелёный — есть в коллекции
     *   Красный — нет в коллекции
     */
    public static int getHoveredSlotColor(int variant) {
        boolean collected = collectionSnapshot.contains(variant);
        return collected ? 0x6000FF00 : 0x60FF0000;
    }

    /**
     * Цвет подсветки для ДУБЛИКАТОВ hovered варианта в контейнере.
     * Жёлтый — тот же тип что под курсором, но другой слот.
     * 0 — не подсвечивать.
     */
    public static int getHoverHighlightColor(int thisVariant) {
        if (hoveredVariant < 0) return 0;
        if (thisVariant != hoveredVariant) return 0;
        return 0x60FFFF00;
    }

    // ── Угловой индикатор (по Shift) ─────────────────────────────────────

    public static int getIndicatorColor(ItemStack stack) {
        if (!isShiftHeld()) return 0;
        if (!stack.isOf(Items.TROPICAL_FISH_BUCKET)) return 0;

        int variant = extractVariant(stack);
        if (variant < 0) return 0;

        YACLConfig config = YACLConfig.HANDLER.instance();
        boolean collected = collectionSnapshot.contains(variant);

        if (!collected) {
            return rgbToArgb(config.indicatorUncollectedRGB, config.indicatorUncollectedAlpha);
        }

        int seen = seenThisFrame.getOrDefault(variant, 0);
        seenThisFrame.put(variant, seen + 1);

        if (seen == 0) {
            return rgbToArgb(config.indicatorCollectedRGB, config.indicatorCollectedAlpha);
        } else {
            return rgbToArgb(config.indicatorDuplicateRGB, config.indicatorDuplicateAlpha);
        }
    }

    // ── Утилиты ──────────────────────────────────────────────────────────

    private static boolean isShiftHeld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return false;
        long handle = client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    public static int extractVariant(ItemStack stack) {
        // 1. DataComponents (1.21+)
        DyeColor base   = stack.get(DataComponentTypes.TROPICAL_FISH_BASE_COLOR);
        DyeColor patCol = stack.get(DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR);
        TropicalFishEntity.Pattern pat = stack.get(DataComponentTypes.TROPICAL_FISH_PATTERN);

        if (base != null && patCol != null && pat != null) {
            return TropicalFishData.encode(pat.ordinal(), base.ordinal(), patCol.ordinal());
        }

        // 2. NBT fallback для спавнерных/именованных рыб
        var bucketData = stack.get(DataComponentTypes.BUCKET_ENTITY_DATA);
        if (bucketData != null) {
            NbtCompound nbt = bucketData.copyNbt();
            if (nbt.contains("BucketVariantTag")) {
                int raw = nbt.getInt("BucketVariantTag").orElse(-1);
                if (raw >= 0) return decodeVanillaRaw(raw);
            }
            if (nbt.contains("variant")) {
                int raw = nbt.getInt("variant").orElse(-1);
                if (raw >= 0) return decodeVanillaRaw(raw);
            }
        }
        return -1;
    }

    private static int decodeVanillaRaw(int raw) {
        int pattern      = (raw >> 8)  & 0xFF;
        int base         = (raw >> 16) & 0xFF;
        int patternColor = (raw >> 24) & 0xFF;
        if (pattern >= 12 || base >= 16 || patternColor >= 16) return -1;
        return TropicalFishData.encode(pattern, base, patternColor);
    }

    private static int rgbToArgb(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }
}