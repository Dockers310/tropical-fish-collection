package com.dok_si.tropicalfish.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.dok_si.tropicalfish.TropicalFishCollection;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.text.Text;

import java.awt.Color;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            YACLConfig cfg = YACLConfig.HANDLER.instance();

            return YetAnotherConfigLib.createBuilder()
                    .title(Text.translatable("config." + TropicalFishCollection.MOD_ID + ".title"))

                    // ── Основные ──────────────────────────────────────────────────────
                    .category(ConfigCategory.createBuilder()
                            .name(Text.translatable("config.tropicalfishcollection.category.general"))
                            .option(boolOpt("config.tropicalfishcollection.only_show_collected",
                                    false, () -> cfg.onlyShowCollected, v -> cfg.onlyShowCollected = v))
                            .option(boolOpt("config.tropicalfishcollection.darken_unsorted",
                                    true, () -> cfg.darkenUnsorted, v -> cfg.darkenUnsorted = v))
                            .option(boolOpt("config.tropicalfishcollection.reset_on_death",
                                    false, () -> cfg.resetOnDeath, v -> cfg.resetOnDeath = v))
                            .option(boolOpt("config.tropicalfishcollection.aggregate_notifications",
                                    true, () -> cfg.aggregateNotifications, v -> cfg.aggregateNotifications = v))
                            .option(boolOpt("config.tropicalfishcollection.use_builtin_sound",
                                    true, () -> cfg.useBuiltinSound, v -> cfg.useBuiltinSound = v))
                            .build())

                    // ── Отображение ───────────────────────────────────────────────────
                    .category(ConfigCategory.createBuilder()
                            .name(Text.translatable("config.tropicalfishcollection.category.display"))
                            .option(intSlider("config.tropicalfishcollection.grid_columns",
                                    2, 32, 8, () -> cfg.gridColumns, v -> cfg.gridColumns = v))
                            .option(intSlider("config.tropicalfishcollection.grid_rows",
                                    2, 32, 8, () -> cfg.gridRows, v -> cfg.gridRows = v))
                            .option(intSlider("config.tropicalfishcollection.icon_scale",
                                    10, 40, 20, () -> cfg.iconScale, v -> cfg.iconScale = v))
                            .option(intSlider("config.tropicalfishcollection.grid_offset_x",
                                    -200, 200, 0, () -> cfg.gridOffsetX, v -> cfg.gridOffsetX = v))
                            .option(intSlider("config.tropicalfishcollection.grid_offset_y",
                                    -200, 200, 0, () -> cfg.gridOffsetY, v -> cfg.gridOffsetY = v))
                            .option(intSlider("config.tropicalfishcollection.stats_offset_y",
                                    0, 100, 15, () -> cfg.statsOffsetY, v -> cfg.statsOffsetY = v))
                            .option(boolOpt("config.tropicalfishcollection.show_sort_buttons",
                                    true, () -> cfg.showSortButtons, v -> cfg.showSortButtons = v))
                            .option(boolOpt("config.tropicalfishcollection.show_bucket_icon",
                                    true, () -> cfg.showBucketIcon, v -> cfg.showBucketIcon = v))
                            .option(intSlider("config.tropicalfishcollection.bucket_offset_x",
                                    -25, 25, 0, () -> cfg.bucketOffsetX, v -> cfg.bucketOffsetX = v))
                            .option(intSlider("config.tropicalfishcollection.bucket_offset_y",
                                    -25, 25, 0, () -> cfg.bucketOffsetY, v -> cfg.bucketOffsetY = v))
                            .option(intSlider("config.tropicalfishcollection.max_bucket_icons",
                                    0, 1024, 256, () -> cfg.maxBucketIcons, v -> cfg.maxBucketIcons = v))
                            .build())

                    // ── Цвета ────────────────────────────────────────────────────────
                    .category(ConfigCategory.createBuilder()
                            .name(Text.translatable("config.tropicalfishcollection.category.display"))
                            // Фон
                            .option(colorOpt("config.tropicalfishcollection.background_color",
                                    0x000000, () -> cfg.backgroundRGB, v -> cfg.backgroundRGB = v))
                            .option(intSlider("config.tropicalfishcollection.background_alpha",
                                    0, 255, 204, () -> cfg.backgroundAlpha, v -> cfg.backgroundAlpha = v))
                            // Галочка
                            .option(colorOpt("config.tropicalfishcollection.check_hex",
                                    0x00FF00, () -> cfg.checkRGB, v -> cfg.checkRGB = v))
                            .option(intSlider("config.tropicalfishcollection.check_alpha",
                                    0, 255, 255, () -> cfg.checkAlpha, v -> cfg.checkAlpha = v))
                            // Несобранные ячейки
                            .option(colorOpt("config.tropicalfishcollection.uncollected_hex",
                                    0x444444, () -> cfg.uncollectedRGB, v -> cfg.uncollectedRGB = v))
                            .option(intSlider("config.tropicalfishcollection.uncollected_alpha",
                                    0, 255, 255, () -> cfg.uncollectedAlpha, v -> cfg.uncollectedAlpha = v))
                            // Рамка ячейки
                            .option(intSlider("config.tropicalfishcollection.cell_border_width",
                                    0, 5, 0, () -> cfg.cellBorderWidth, v -> cfg.cellBorderWidth = v))
                            .option(colorOpt("config.tropicalfishcollection.cell_border_hex",
                                    0x000000, () -> cfg.cellBorderRGB, v -> cfg.cellBorderRGB = v))
                            .option(intSlider("config.tropicalfishcollection.cell_border_alpha",
                                    0, 255, 255, () -> cfg.cellBorderAlpha, v -> cfg.cellBorderAlpha = v))
                            // Текст статистики
                            .option(colorOpt("config.tropicalfishcollection.stats_text_hex",
                                    0xFFAA00, () -> cfg.statsTextRGB, v -> cfg.statsTextRGB = v))
                            .option(intSlider("config.tropicalfishcollection.stats_text_alpha",
                                    0, 255, 255, () -> cfg.statsTextAlpha, v -> cfg.statsTextAlpha = v))
                            .build())

                    // ── Индикаторы на вёдрах ──────────────────────────────────────────
                    .category(ConfigCategory.createBuilder()
                            .name(Text.translatable("config.tropicalfishcollection.indicator_size"))
                            .option(intSlider("config.tropicalfishcollection.indicator_size",
                                    2, 14, 6, () -> cfg.indicatorSize, v -> cfg.indicatorSize = v))
                            // Собрана
                            .option(colorOpt("config.tropicalfishcollection.indicator_collected_hex",
                                    0x00FF00, () -> cfg.indicatorCollectedRGB, v -> cfg.indicatorCollectedRGB = v))
                            .option(intSlider("config.tropicalfishcollection.indicator_collected_alpha",
                                    0, 255, 255, () -> cfg.indicatorCollectedAlpha, v -> cfg.indicatorCollectedAlpha = v))
                            // Не собрана
                            .option(colorOpt("config.tropicalfishcollection.indicator_uncollected_hex",
                                    0xFF0000, () -> cfg.indicatorUncollectedRGB, v -> cfg.indicatorUncollectedRGB = v))
                            .option(intSlider("config.tropicalfishcollection.indicator_uncollected_alpha",
                                    0, 255, 255, () -> cfg.indicatorUncollectedAlpha, v -> cfg.indicatorUncollectedAlpha = v))
                            // Дубликат
                            .option(colorOpt("config.tropicalfishcollection.indicator_duplicate_hex",
                                    0xFFFF00, () -> cfg.indicatorDuplicateRGB, v -> cfg.indicatorDuplicateRGB = v))
                            .option(intSlider("config.tropicalfishcollection.indicator_duplicate_alpha",
                                    0, 255, 255, () -> cfg.indicatorDuplicateAlpha, v -> cfg.indicatorDuplicateAlpha = v))
                            .build())

                    // ── Подсветка рыб в мире ──────────────────────────────────────────
                    .category(ConfigCategory.createBuilder()
                            .name(Text.translatable("config.tropicalfishcollection.highlight_uncollected_fish"))
                            .option(boolOpt("config.tropicalfishcollection.highlight_uncollected_fish",
                                    false, () -> cfg.highlightUncollectedFish, v -> cfg.highlightUncollectedFish = v))
                            .option(colorOpt("config.tropicalfishcollection.highlight_color",
                                    0xFFFF00, () -> cfg.highlightRGB, v -> cfg.highlightRGB = v))
                            .option(intSlider("config.tropicalfishcollection.highlight_alpha",
                                    0, 255, 180, () -> cfg.highlightAlpha, v -> cfg.highlightAlpha = v))
                            .option(intSlider("config.tropicalfishcollection.highlight_range",
                                    4, 64, 16, () -> cfg.highlightRange, v -> cfg.highlightRange = v))
                            .build())

                    // ── Подсказки ─────────────────────────────────────────────────────
                    .category(ConfigCategory.createBuilder()
                            .name(Text.translatable("config.tropicalfishcollection.category.tooltips"))
                            .option(Option.<YACLConfig.TooltipMode>createBuilder()
                                    .name(Text.translatable("config.tropicalfishcollection.tooltip_mode"))
                                    .binding(YACLConfig.TooltipMode.HOVER, () -> cfg.tooltipMode, v -> cfg.tooltipMode = v)
                                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(YACLConfig.TooltipMode.class))
                                    .build())
                            .build())

                    // ── Уведомления ───────────────────────────────────────────────────
                    .category(ConfigCategory.createBuilder()
                            .name(Text.translatable("config.tropicalfishcollection.category.toasts"))
                            .option(intSlider("config.tropicalfishcollection.toast_duration",
                                    1000, 15000, 5000, () -> cfg.toastDurationMs, v -> cfg.toastDurationMs = v))
                            .build())

                    .save(() -> YACLConfig.HANDLER.save())
                    .build()
                    .generateScreen(parent);
        };
    }

    // ── Хелперы ──────────────────────────────────────────────────────────────

    /**
     * Цветовой пикер — хранит цвет как 0xRRGGBB int.
     * ColorControllerBuilder работает с java.awt.Color, поэтому конвертируем туда-обратно.
     */
    private static Option<Color> colorOpt(String key, int def,
                                          java.util.function.Supplier<Integer> getter,
                                          java.util.function.Consumer<Integer> setter) {
        return Option.<Color>createBuilder()
                .name(Text.translatable(key))
                .binding(
                        new Color(def),
                        () -> new Color(getter.get()),
                        v -> setter.accept(v.getRGB() & 0xFFFFFF)
                )
                .controller(ColorControllerBuilder::create)
                .build();
    }

    private static Option<Integer> intSlider(String key, int min, int max, int def,
                                             java.util.function.Supplier<Integer> getter,
                                             java.util.function.Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(Text.translatable(key))
                .binding(def, getter, setter)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(1))
                .build();
    }

    private static Option<Boolean> boolOpt(String key, boolean def,
                                           java.util.function.Supplier<Boolean> getter,
                                           java.util.function.Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Text.translatable(key))
                .binding(def, getter, setter)
                .controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter())
                .build();
    }
}