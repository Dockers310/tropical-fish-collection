package com.dok_si.tropicalfish.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public class YACLConfig {
    public static final ConfigClassHandler<YACLConfig> HANDLER = ConfigClassHandler.createBuilder(YACLConfig.class)
            .id(Identifier.of("tropicalfishcollection", "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir()
                            .resolve("tropicalfishcollection/settings.json"))
                    .build())
            .build();

    // ── Основные ─────────────────────────────────────────────────────────
    @SerialEntry public boolean onlyShowCollected = false;
    @SerialEntry public boolean darkenUnsorted = true;
    @SerialEntry public boolean sortByColor = false;
    @SerialEntry public boolean sortByType = false;
    @SerialEntry public boolean resetOnDeath = false;
    @SerialEntry public boolean aggregateNotifications = true;

    // ── Отображение ───────────────────────────────────────────────────────
    @SerialEntry public int gridColumns = 8;
    @SerialEntry public int gridRows = 8;
    @SerialEntry public int gridOffsetX = 0;
    @SerialEntry public int gridOffsetY = 0;
    @SerialEntry public int iconScale = 20;
    @SerialEntry public int statsOffsetY = 15;
    @SerialEntry public boolean showSortButtons = true;
    @SerialEntry public int buttonOffsetY = 0;
    @SerialEntry public boolean showBucketIcon = true;
    @SerialEntry public int bucketOffsetX = 0;
    @SerialEntry public int bucketOffsetY = 0;
    @SerialEntry public int maxBucketIcons = 256;

    // ── Цвета ────────────────────────────────────────────────────────────
    @SerialEntry public String backgroundHex = "#000000";
    @SerialEntry public int backgroundAlpha = 204;
    @SerialEntry public String backgroundImage = "";
    @SerialEntry public String checkHex = "#00FF00";
    @SerialEntry public int checkAlpha = 255;
    @SerialEntry public String uncollectedHex = "#444444";
    @SerialEntry public int uncollectedAlpha = 255;
    @SerialEntry public int cellBorderWidth = 0;
    @SerialEntry public String cellBorderHex = "#000000";
    @SerialEntry public int cellBorderAlpha = 255;
    @SerialEntry public String statsTextHex = "#FFAA00";
    @SerialEntry public int statsTextAlpha = 255;

    // ── Индикаторы на вёдрах ─────────────────────────────────────────────
    @SerialEntry public String indicatorCollectedHex = "#00FF00";
    @SerialEntry public int indicatorCollectedAlpha = 255;
    @SerialEntry public String indicatorUncollectedHex = "#FF0000";
    @SerialEntry public int indicatorUncollectedAlpha = 255;
    @SerialEntry public String indicatorDuplicateHex = "#FFFF00";
    @SerialEntry public int indicatorDuplicateAlpha = 255;
    @SerialEntry public int indicatorSize = 6;
    /** false = только по Shift, true = всегда */
    @SerialEntry public boolean indicatorAlwaysShow = false;

    // ── Подсветка рыб в мире ─────────────────────────────────────────────
    /** По умолчанию ВЫКЛЮЧЕНО */
    @SerialEntry public boolean highlightUncollectedFish = false;
    @SerialEntry public String highlightColorHex = "#FFFF00";
    @SerialEntry public int highlightColorAlpha = 180;
    @SerialEntry public int highlightRange = 16;

    // ── Уведомления ───────────────────────────────────────────────────────
    @SerialEntry public int toastDurationMs = 5000;
    @SerialEntry public boolean useBuiltinSound = true;

    // ── Подсказки ─────────────────────────────────────────────────────────
    public enum TooltipMode { HOVER, ALWAYS, NEVER }
    @SerialEntry public TooltipMode tooltipMode = TooltipMode.HOVER;
}
