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
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/**
 * Рисует цветной индикатор на вёдрах с рыбой в контейнерах.
 *
 * Показывается ТОЛЬКО когда зажат Left Shift (TOGGLE_INDICATORS).
 *
 * Цвета:
 *   🟢 ЗЕЛЁНЫЙ  — рыба есть в коллекции, встречается первый раз в этом контейнере
 *   🟡 ЖЁЛТЫЙ   — рыба есть в коллекции, но в этом контейнере дубликат
 *   🔴 КРАСНЫЙ  — рыбы НЕТ в коллекции (стоит взять!)
 *
 * Снимок коллекции делается при открытии контейнера и не меняется пока он открыт.
 */
public class BucketDecorator {

    private static final KeyBinding.Category CATEGORY =
            new KeyBinding.Category(Identifier.of(TropicalFishCollection.MOD_ID, "category"));

    /** Зажатый Shift = показать индикаторы */
    public static final KeyBinding TOGGLE_INDICATORS = new KeyBinding(
            "key.tropicalfishcollection.toggle_indicators",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            CATEGORY
    );

    /**
     * Варианты уже встреченные в ТЕКУЩЕМ открытом контейнере.
     * Сбрасывается каждый кадр из HandledScreenMixin.
     */
    private static final Set<Integer> seenInThisContainer = new HashSet<>();

    /**
     * Снимок коллекции на момент открытия контейнера.
     * Не меняется пока контейнер открыт.
     */
    private static Set<Integer> collectionSnapshot = new HashSet<>();

    /**
     * Вызывается из HandledScreenMixin перед рендером каждого контейнера.
     */
    public static void resetForNewContainer() {
        seenInThisContainer.clear();
        collectionSnapshot = new HashSet<>(PlayerCollectionState.getCollected());
    }

    /**
     * Возвращает ARGB цвет индикатора для данного стека,
     * или 0 если Shift не зажат / стек не является ведром с рыбой.
     */
    public static int getIndicatorColor(ItemStack stack) {
        // Показываем только пока зажат Shift
        if (!isShiftHeld()) return 0;

        if (!stack.isOf(Items.TROPICAL_FISH_BUCKET)) return 0;

        int variant = extractVariant(stack);
        if (variant < 0) return 0;

        YACLConfig config = YACLConfig.HANDLER.instance();
        boolean collected = collectionSnapshot.contains(variant);

        if (!collected) {
            // 🔴 Красный — рыбы нет в коллекции
            return hexToArgb(config.indicatorUncollectedHex, config.indicatorUncollectedAlpha);
        }

        if (seenInThisContainer.contains(variant)) {
            // 🟡 Жёлтый — дубликат в этом контейнере
            return hexToArgb(config.indicatorDuplicateHex, config.indicatorDuplicateAlpha);
        } else {
            // 🟢 Зелёный — первое появление, рыба собрана
            seenInThisContainer.add(variant);
            return hexToArgb(config.indicatorCollectedHex, config.indicatorCollectedAlpha);
        }
    }

    /**
     * Проверяет зажат ли Left Shift через GLFW.
     */
    private static boolean isShiftHeld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return false;
        long handle = client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    // ------------------------------------------------------------------

    private static int extractVariant(ItemStack stack) {
        DyeColor base   = stack.get(DataComponentTypes.TROPICAL_FISH_BASE_COLOR);
        DyeColor patCol = stack.get(DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR);
        TropicalFishEntity.Pattern pat = stack.get(DataComponentTypes.TROPICAL_FISH_PATTERN);

        if (base != null && patCol != null && pat != null) {
            return TropicalFishData.encode(pat.ordinal(), base.ordinal(), patCol.ordinal());
        }

        var bucketData = stack.get(DataComponentTypes.BUCKET_ENTITY_DATA);
        if (bucketData != null) {
            var nbt = bucketData.copyNbt();
            if (nbt.contains("BucketVariantTag")) return nbt.getInt("BucketVariantTag").orElse(-1);
            if (nbt.contains("variant"))           return nbt.getInt("variant").orElse(-1);
        }
        return -1;
    }

    private static int hexToArgb(String hex, int alpha) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            return (alpha << 24) | Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return 0xFF000000;
        }
    }
}
