package com.dok_si.tropicalfish.toast;

import com.dok_si.tropicalfish.PlayerCollectionState;
import com.dok_si.tropicalfish.TropicalFishData;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Красивый тост о новой тропической рыбе.
 *
 * Одиночный тост:
 *   ┌─────────────────────────────────────┐
 *   │ ░░  🐠 Новая рыба!                  │  ← жёлтый заголовок
 *   │ ░░  Санстрик                        │  ← голубой узор
 *   │ ░░  ██ Белый  /  ██ Оранжевый       │  ← цветные плашки основного и узора
 *   │ ░░  Найдено: 42 / 3072  ░░░░░░░░░   │  ← прогресс-бар
 *   └─────────────────────────────────────┘
 *   Левая полоса = основной цвет рыбы
 *
 * Групповой тост:
 *   ┌─────────────────────────────────────┐
 *   │ 🎣  Найдено 5 новых рыб!            │
 *   │     Проверьте коллекцию             │
 *   │     Итого: 47 / 3072  ░░░░░░░░░░░   │
 *   └─────────────────────────────────────┘
 */
public class NewFishToast implements Toast {

    private static final int WIDTH  = 220;
    private static final int HEIGHT_SINGLE = 52;
    private static final int HEIGHT_GROUP  = 38;

    private final int     variant;       // -1 если групповой
    private final int     count;         // >0 если групповой
    private final long    durationMs;

    private long startTime  = -1;
    private long currentTime;

    // Цвета рыбы (кэшируются)
    private final int baseArgb;
    private final int patternArgb;

    // ---- Конструктор для одной рыбы ----
    public NewFishToast(int variant, long durationMs) {
        this.variant    = variant;
        this.count      = -1;
        this.durationMs = durationMs;
        this.baseArgb    = TropicalFishData.getBaseColor(variant);
        this.patternArgb = TropicalFishData.getPatternColor(variant);
    }

    // ---- Конструктор для группового тоста ----
    public NewFishToast(int count, long durationMs, boolean isGroup) {
        this.variant    = -1;
        this.count      = count;
        this.durationMs = durationMs;
        this.baseArgb    = 0xFF1A6B8A;
        this.patternArgb = 0xFF2E9E6A;
    }

    // ==================================================================

    @Override
    public void draw(DrawContext ctx, TextRenderer tr, long time) {
        if (startTime < 0) startTime = time;
        currentTime = time;

        int w = getWidth();
        int h = getHeight();

        // ---- Фон с градиентом ----
        // Тёмная основа
        ctx.fill(0, 0, w, h, 0xF0131820);
        // Градиент сверху (слегка светлее)
        ctx.fillGradient(0, 0, w, h / 2, 0x18FFFFFF, 0x00FFFFFF);
        // Левая цветная полоса (цвет основного тела рыбы)
        int stripeColor = (baseArgb & 0x00FFFFFF) | 0xEE000000;
        ctx.fill(0, 0, 4, h, stripeColor);
        // Вторая тонкая полоса (цвет узора)
        int stripe2Color = (patternArgb & 0x00FFFFFF) | 0xCC000000;
        ctx.fill(4, 0, 6, h, stripe2Color);

        // Рамка
        ctx.fill(0,     0, w,     1, 0x55FFFFFF);
        ctx.fill(0, h - 1, w,     h, 0x33FFFFFF);
        ctx.fill(w - 1, 0, w,     h, 0x33FFFFFF);

        if (count > 0) {
            drawGroup(ctx, tr, w, h);
        } else {
            drawSingle(ctx, tr, w, h);
        }

        // Прогресс-бар времени снизу
        float timeProgress = 1f - Math.min(1f, (float)(currentTime - startTime) / durationMs);
        int barW = (int)((w - 8) * timeProgress);
        ctx.fill(4, h - 2, 4 + barW, h - 1, 0x88AAAAAA);
    }

    private void drawSingle(DrawContext ctx, TextRenderer tr, int w, int h) {
        int lx = 10; // левый отступ текста (после полосы)

        // ---- Заголовок ----
        ctx.drawText(tr,
                Text.literal("✦ ").formatted(Formatting.YELLOW)
                        .append(Text.translatable("toast.tropicalfishcollection.new_fish")
                                .formatted(Formatting.YELLOW, Formatting.BOLD)),
                lx, 5, -1, false);

        // ---- Название узора ----
        Text patternName = TropicalFishData.getPatternText(TropicalFishData.getPatternIndex(variant));
        ctx.drawText(tr, patternName.copy().formatted(Formatting.AQUA), lx, 16, -1, false);

        // ---- Цветные плашки ----
        int boxY    = 26;
        int boxSize = 8;

        // Основной цвет
        ctx.fill(lx, boxY, lx + boxSize, boxY + boxSize, baseArgb);
        ctx.fill(lx, boxY, lx + boxSize, boxY + 1, 0x55FFFFFF); // блик
        Text baseText = TropicalFishData.getColoredColorText(TropicalFishData.getBaseColorIndex(variant));
        ctx.drawText(tr, baseText, lx + boxSize + 3, boxY, -1, false);

        // Цвет узора (правее)
        int col2x = lx + 80;
        ctx.fill(col2x, boxY, col2x + boxSize, boxY + boxSize, patternArgb);
        ctx.fill(col2x, boxY, col2x + boxSize, boxY + 1, 0x55FFFFFF);
        Text patColText = TropicalFishData.getColoredColorText(TropicalFishData.getPatternColorIndex(variant));
        ctx.drawText(tr, patColText, col2x + boxSize + 3, boxY, -1, false);

        // ---- Прогресс коллекции ----
        drawProgressLine(ctx, tr, lx, 38, w - 10);
    }

    private void drawGroup(DrawContext ctx, TextRenderer tr, int w, int h) {
        int lx = 10;

        // ---- Заголовок ----
        ctx.drawText(tr,
                Text.literal("✦ ").formatted(Formatting.YELLOW)
                        .append(Text.translatable("toast.tropicalfishcollection.new_fish_many", count)
                                .formatted(Formatting.YELLOW, Formatting.BOLD)),
                lx, 6, -1, false);

        // ---- Подпись ----
        ctx.drawText(tr,
                Text.translatable("toast.tropicalfishcollection.check_collection")
                        .formatted(Formatting.GRAY),
                lx, 17, -1, false);

        // ---- Прогресс ----
        drawProgressLine(ctx, tr, lx, 27, w - 10);
    }

    /**
     * Рисует строку «Найдено: X / 3072» + прогресс-бар.
     */
    private void drawProgressLine(DrawContext ctx, TextRenderer tr, int x, int y, int maxW) {
        int total     = TropicalFishData.TOTAL_VARIANTS;
        int collected = PlayerCollectionState.getCollectedCount();
        float pct     = (float) collected / total;

        // Текст
        String text = collected + " / " + total;
        ctx.drawText(tr, Text.literal(text).formatted(Formatting.DARK_GRAY), x, y, -1, false);

        // Бар
        int textW  = tr.getWidth(text) + 4;
        int barX   = x + textW;
        int barW   = maxW - textW;
        int barH   = 4;
        int barY   = y + (tr.fontHeight - barH) / 2 + 1;
        int filled = (int)(barW * pct);

        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF222222);
        if (filled > 0) {
            // Градиент прогресса: красный → жёлтый → зелёный
            int barColor = progressColor(pct);
            ctx.fill(barX, barY, barX + filled, barY + barH, barColor);
            // Блик сверху
            ctx.fill(barX, barY, barX + filled, barY + 1, 0x44FFFFFF);
        }
    }

    private int progressColor(float pct) {
        if (pct >= 0.9f) return 0xFF00FF99;
        if (pct >= 0.5f) return 0xFF44EE44;
        if (pct >= 0.2f) return 0xFFFFEE22;
        return 0xFFFF4422;
    }

    // ==================================================================

    @Override
    public Visibility getVisibility() {
        if (startTime < 0) return Visibility.SHOW;
        return (currentTime - startTime) < durationMs ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public void update(ToastManager manager, long time) {
        currentTime = time;
    }

    @Override public int getWidth()  { return WIDTH; }
    @Override public int getHeight() { return count > 0 ? HEIGHT_GROUP : HEIGHT_SINGLE; }
}
