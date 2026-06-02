package com.dok_si.tropicalfish.toast;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Особый тост для достижений коллекции (5%, 10%, ..., 100%).
 *
 * Визуально отличается от обычного NewFishToast:
 *  - Золотая/радужная рамка
 *  - Большая цифра процента
 *  - Анимация яркости
 *
 * ┌──────────────────────────────────────────┐
 * │ ★  ДОСТИЖЕНИЕ РАЗБЛОКИРОВАНО!            │  ← золотой
 * │    🐠 Собрано 25% коллекции              │  ← основная надпись
 * │    154 / 3072 тропических рыб            │  ← серый subtitle
 * │    ████████░░░░░░░░░░░░ 25%             │  ← прогресс-бар
 * └──────────────────────────────────────────┘
 */
public class MilestoneToast implements Toast {

    private static final int WIDTH  = 240;
    private static final int HEIGHT = 50;

    private final int  milestonePercent;
    private final int  count;
    private final int  total;
    private final long durationMs;

    private long startTime = -1;
    private long currentTime;

    public MilestoneToast(int milestonePercent, int count, int total, long durationMs) {
        this.milestonePercent = milestonePercent;
        this.count            = count;
        this.total            = total;
        this.durationMs       = durationMs;
    }

    @Override
    public void draw(DrawContext ctx, TextRenderer tr, long time) {
        if (startTime < 0) startTime = time;
        currentTime = time;

        int w = getWidth();
        int h = getHeight();

        // ── Фон ──────────────────────────────────────────────────────────
        ctx.fill(0, 0, w, h, 0xF0100A00);
        ctx.fillGradient(0, 0, w, h / 2, 0x1AFFD700, 0x00FFD700);

        // ── Анимированная золотая рамка ───────────────────────────────────
        float progress = Math.min(1f, (float)(currentTime - startTime) / 300f);
        int glowAlpha  = (int)(0x88 * progress);
        int borderColor = (glowAlpha << 24) | 0xFFD700;

        // Верх/низ
        ctx.fill(0, 0, w, 1, borderColor);
        ctx.fill(0, h - 1, w, h, borderColor);
        // Лево/право
        ctx.fill(0, 0, 1, h, borderColor);
        ctx.fill(w - 1, 0, w, h, borderColor);

        // Внутренняя подсветка для 100%
        if (milestonePercent == 100) {
            // Радужная рамка — пульсирует
            long t = (currentTime - startTime) % 2000;
            int rainbowColor = hsvToRgb((float) t / 2000f, 1f, 1f);
            ctx.fill(0, 0, w, 1, (0xCC << 24) | (rainbowColor & 0xFFFFFF));
            ctx.fill(0, h - 1, w, h, (0xCC << 24) | (rainbowColor & 0xFFFFFF));
        }

        // ── Золотая полоска слева ─────────────────────────────────────────
        ctx.fill(0, 0, 5, h, 0xEEFFD700);
        ctx.fill(5, 0, 7, h, 0x66FFD700);

        int lx = 12;

        // ── Заголовок ─────────────────────────────────────────────────────
        String header = milestonePercent == 100
                ? "✦ " + Text.translatable("toast.tropicalfishcollection.achievement_100").getString()
                : "✦ " + Text.translatable("toast.tropicalfishcollection.achievement_unlocked").getString();
        ctx.drawText(tr, Text.literal(header).formatted(Formatting.GOLD, Formatting.BOLD),
                lx, 5, -1, false);

        // ── Основной текст ────────────────────────────────────────────────
        String mainLine = Text.translatable(
                "toast.tropicalfishcollection.milestone_reached",
                milestonePercent + "%").getString();
        ctx.drawText(tr, Text.literal("🐠 " + mainLine).formatted(Formatting.YELLOW),
                lx, 16, -1, false);

        // ── Subtitle ──────────────────────────────────────────────────────
        String subtitle = count + " / " + total + " "
                + Text.translatable("toast.tropicalfishcollection.fish_collected").getString();
        ctx.drawText(tr, Text.literal(subtitle).formatted(Formatting.GRAY),
                lx, 27, -1, false);

        // ── Прогресс-бар ─────────────────────────────────────────────────
        int barX    = lx;
        int barY    = 38;
        int barW    = w - lx - 8;
        int barH    = 4;
        float pct   = (float) count / total;
        int filled  = (int)(barW * pct);

        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1A00);
        if (filled > 0) {
            ctx.fill(barX, barY, barX + filled, barY + barH, progressColor(pct));
            ctx.fill(barX, barY, barX + filled, barY + 1, 0x44FFFFFF);
        }

        // Процент на баре
        String pctStr = milestonePercent + "%";
        ctx.drawText(tr, Text.literal(pctStr).formatted(Formatting.GOLD),
                barX + barW - tr.getWidth(pctStr), barY - 9, -1, false);

        // ── Таймер снизу ──────────────────────────────────────────────────
        float timeProgress = 1f - Math.min(1f, (float)(currentTime - startTime) / durationMs);
        int timerW = (int)((w - 8) * timeProgress);
        ctx.fill(4, h - 2, 4 + timerW, h - 1, 0x88FFD700);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private int progressColor(float pct) {
        if (pct >= 0.9f) return 0xFFFFD700; // золото для 90%+
        if (pct >= 0.5f) return 0xFF44EE44;
        if (pct >= 0.2f) return 0xFFFFEE22;
        return 0xFFFF8822;
    }

    /** HSV → packed RGB (без альфы) */
    private static int hsvToRgb(float h, float s, float v) {
        int hi = (int)(h * 6) % 6;
        float f = h * 6 - (int)(h * 6);
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (hi) {
            case 0  -> { r = v; g = t; b = p; }
            case 1  -> { r = q; g = v; b = p; }
            case 2  -> { r = p; g = v; b = t; }
            case 3  -> { r = p; g = q; b = v; }
            case 4  -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    @Override
    public Visibility getVisibility() {
        if (startTime < 0) return Visibility.SHOW;
        return (currentTime - startTime) < durationMs ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public void update(ToastManager manager, long time) { currentTime = time; }

    @Override public int getWidth()  { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
}
