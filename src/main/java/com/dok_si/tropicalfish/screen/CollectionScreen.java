package com.dok_si.tropicalfish.screen;

import com.dok_si.tropicalfish.PlayerCollectionState;
import com.dok_si.tropicalfish.TropicalFishCollection;
import com.dok_si.tropicalfish.TropicalFishData;
import com.dok_si.tropicalfish.client.BucketDecorator;
import com.dok_si.tropicalfish.config.YACLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Экран коллекции.
 *
 * Исправления v1.0.5:
 * - renderGrid: исправлен баг счётчика skip при onlyShowCollected
 *   (теперь пропускаем только реально видимые элементы, а не все подряд)
 * - Стрелки пагинации разнесены шире, номер страницы хорошо виден между ними
 * - getPageStart() больше не используется в renderStats (там теперь правильный visible-счётчик)
 */
public class CollectionScreen extends Screen {

    private static final int CELL_PAD   = 2;
    private static final int ICON_BASE  = 16;

    private static final int MARGIN_X   = 8;
    private static final int MARGIN_TOP = 46;
    private static final int MARGIN_BOT = 32;

    private final List<Integer>          allVariants;
    private final Set<Integer>           collected;
    private List<Integer>                filtered;

    private int page, maxPage;
    private int cols, rows, iconSize;
    private int gridX, gridY;

    private final Map<Integer, long[]>    colorCache = new HashMap<>();
    private final Map<Integer, ItemStack> stackCache = new HashMap<>();

    private ButtonWidget prevBtn, nextBtn;
    private TextFieldWidget searchField;
    private String searchText = "";

    private boolean editMode = false;
    private boolean freeMode = false;
    private int     selElem  = 0;
    private boolean dragging   = false;
    private int     dragTarget = -1;
    private double  dragX, dragY;

    private long lastResetClick = 0;
    private static final long DBL_CLICK = 800;

    // ── Constructor ────────────────────────────────────────────────────────

    public CollectionScreen() {
        super(Text.translatable("screen." + TropicalFishCollection.MOD_ID + ".collection"));
        this.allVariants = new ArrayList<>(TropicalFishData.generateAllVariants());
        this.collected   = new HashSet<>(PlayerCollectionState.getCollected());

        YACLConfig cfg = YACLConfig.HANDLER.instance();
        if (cfg.sortByColor) TropicalFishData.sortByColor(allVariants);
        else if (cfg.sortByType) TropicalFishData.sortByType(allVariants);

        applyConfig();
    }

    // ── Config & layout ────────────────────────────────────────────────────

    private void applyConfig() {
        YACLConfig cfg = YACLConfig.HANDLER.instance();
        cols     = cfg.gridColumns;
        rows     = cfg.gridRows;
        iconSize = cfg.iconScale;
        colorCache.clear();
        rebuildFiltered();
    }

    private void computeAdaptiveLayout() {
        YACLConfig cfg = YACLConfig.HANDLER.instance();

        int availW = width  - MARGIN_X * 2;
        int availH = height - MARGIN_TOP - MARGIN_BOT - 30;

        cols     = cfg.gridColumns;
        rows     = cfg.gridRows;
        iconSize = cfg.iconScale;

        int minIcon = 8;
        while (iconSize > minIcon) {
            int gridW = cols * (iconSize + CELL_PAD) - CELL_PAD;
            int gridH = rows * (iconSize + CELL_PAD) - CELL_PAD;
            if (gridW <= availW && gridH <= availH) break;
            iconSize--;
        }

        while (cols > 1) {
            int gridW = cols * (iconSize + CELL_PAD) - CELL_PAD;
            if (gridW <= availW) break;
            cols--;
        }

        while (rows > 1) {
            int gridH = rows * (iconSize + CELL_PAD) - CELL_PAD;
            if (gridH <= availH) break;
            rows--;
        }

        int gridW = cols * (iconSize + CELL_PAD) - CELL_PAD;
        int gridH = rows * (iconSize + CELL_PAD) - CELL_PAD;
        gridX = (width  - gridW) / 2 + cfg.gridOffsetX;
        gridY = (height - gridH) / 2 + cfg.gridOffsetY;

        gridX = Math.clamp(gridX, MARGIN_X, width  - gridW - MARGIN_X);
        gridY = Math.clamp(gridY, MARGIN_TOP, height - gridH - MARGIN_BOT);
    }

    private void rebuildFiltered() {
        String q = searchText.trim().toLowerCase();
        if (q.isEmpty()) {
            filtered = new ArrayList<>(allVariants);
        } else {
            filtered = new ArrayList<>();
            for (int v : allVariants) {
                String pat  = TropicalFishData.getPatternText(TropicalFishData.getPatternIndex(v)).getString().toLowerCase();
                String col1 = TropicalFishData.getColorText(TropicalFishData.getBaseColorIndex(v)).getString().toLowerCase();
                String col2 = TropicalFishData.getColorText(TropicalFishData.getPatternColorIndex(v)).getString().toLowerCase();
                if (pat.contains(q) || col1.contains(q) || col2.contains(q)) filtered.add(v);
            }
        }
        recalcPages();
        page = Math.min(page, maxPage);
        colorCache.clear();
    }

    private void recalcPages() {
        YACLConfig cfg = YACLConfig.HANDLER.instance();
        int perPage = cols * rows;
        if (perPage == 0) { maxPage = 0; return; }

        if (!cfg.onlyShowCollected) {
            maxPage = filtered.isEmpty() ? 0 : (filtered.size() - 1) / perPage;
        } else {
            int vis = 0;
            for (int v : filtered) if (collected.contains(v)) vis++;
            maxPage = vis == 0 ? 0 : (vis - 1) / perPage;
        }
    }

    // ── Init ───────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        computeAdaptiveLayout();
        recalcPages();

        YACLConfig cfg = YACLConfig.HANDLER.instance();
        int gridW = cols * (iconSize + CELL_PAD) - CELL_PAD;
        int gridH = rows * (iconSize + CELL_PAD) - CELL_PAD;

        // ── Верхняя панель ────────────────────────────────────────────────
        searchField = new TextFieldWidget(textRenderer, 4, 4, 130, 15, Text.literal(""));
        searchField.setText(searchText);
        searchField.setPlaceholder(Text.translatable("search.tropicalfishcollection.placeholder"));
        searchField.setChangedListener(t -> { searchText = t; rebuildFiltered(); page = 0; clearChildren(); init(); });
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(
                Text.translatable(editMode ? "button.tropicalfishcollection.edit_done" : "button.tropicalfishcollection.edit_mode"),
                b -> { editMode = !editMode; freeMode = false; clearChildren(); init(); }
        ).dimensions(width - 66, 4, 62, 14).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable(freeMode ? "button.tropicalfishcollection.free_on" : "button.tropicalfishcollection.free_off"),
                b -> { freeMode = !freeMode; editMode = false; clearChildren(); init(); }
        ).dimensions(width - 132, 4, 62, 14).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("button.tropicalfishcollection.export"),
                b -> doExport()
        ).dimensions(4, 22, 58, 14).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("🏆 " + Text.translatable("button.tropicalfishcollection.leaderboard").getString()),
                b -> MinecraftClient.getInstance().setScreen(new LeaderboardScreen(this))
        ).dimensions(66, 22, 76, 14).build());

        // ── Edit mode controls ────────────────────────────────────────────
        if (editMode) {
            int ey = 40;
            addDrawableChild(ButtonWidget.builder(Text.literal("Grid"),    b -> selElem = 0).dimensions(4,   ey, 40, 12).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Stats"),   b -> selElem = 1).dimensions(48,  ey, 40, 12).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Buttons"), b -> selElem = 2).dimensions(92,  ey, 50, 12).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("↑"), b -> moveElem(0,-1)).dimensions(150, ey, 14, 12).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("↓"), b -> moveElem(0, 1)).dimensions(166, ey, 14, 12).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("←"), b -> moveElem(-1,0)).dimensions(182, ey, 14, 12).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("→"), b -> moveElem( 1,0)).dimensions(198, ey, 14, 12).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> scaleIcon( 1)).dimensions(216, ey, 14, 12).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("−"), b -> scaleIcon(-1)).dimensions(232, ey, 14, 12).build());
        }

        // ── Пагинация ─────────────────────────────────────────────────────
        // Стрелки разнесены на ±60px от центра — номер страницы хорошо виден между ними
        int pageY  = gridY + gridH + 4;
        int pageCX = gridX + gridW / 2;
        prevBtn = ButtonWidget.builder(Text.literal("◀"),
                        b -> { if (page > 0) { page--; colorCache.clear(); } updateNav(); })
                .dimensions(pageCX - 62, pageY, 20, 14).build();
        nextBtn = ButtonWidget.builder(Text.literal("▶"),
                        b -> { if (page < maxPage) { page++; colorCache.clear(); } updateNav(); })
                .dimensions(pageCX + 42, pageY, 20, 14).build();
        addDrawableChild(prevBtn);
        addDrawableChild(nextBtn);

        // ── Нижние кнопки ─────────────────────────────────────────────────
        if (cfg.showSortButtons) buildBottomButtons(cfg);

        updateNav();
    }

    private void buildBottomButtons(YACLConfig cfg) {
        int sortY = height - 20 + cfg.buttonOffsetY;

        int avail   = width - 8;
        int[] minW  = {60, 60, 44, 56, 80};
        int   gaps  = 4 * 4;
        int   total = 0; for (int w : minW) total += w;

        float scale = (avail - gaps) < total ? (float)(avail - gaps) / total : 1f;
        int[] w = new int[5];
        for (int i = 0; i < 5; i++) w[i] = Math.max(20, (int)(minW[i] * scale));

        int actualTotal = 0; for (int ww : w) actualTotal += ww;
        int sx = (width - actualTotal - gaps) / 2;

        int x = sx;
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.tropicalfishcollection.sort_color"), b -> {
            TropicalFishData.sortByColor(allVariants); rebuildFiltered(); page = 0; updateNav();
        }).dimensions(x, sortY, w[0], 16).build());
        x += w[0] + 4;

        addDrawableChild(ButtonWidget.builder(Text.translatable("button.tropicalfishcollection.sort_type"), b -> {
            TropicalFishData.sortByType(allVariants); rebuildFiltered(); page = 0; updateNav();
        }).dimensions(x, sortY, w[1], 16).build());
        x += w[1] + 4;

        addDrawableChild(ButtonWidget.builder(Text.translatable("button.tropicalfishcollection.reset_sort"), b -> {
            allVariants.clear(); allVariants.addAll(TropicalFishData.generateAllVariants());
            rebuildFiltered(); page = 0; updateNav();
        }).dimensions(x, sortY, w[2], 16).build());
        x += w[2] + 4;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable(cfg.onlyShowCollected
                        ? "button.tropicalfishcollection.show_all"
                        : "button.tropicalfishcollection.show_collected"), b -> {
                    cfg.onlyShowCollected = !cfg.onlyShowCollected;
                    YACLConfig.HANDLER.save();
                    b.setMessage(Text.translatable(cfg.onlyShowCollected
                            ? "button.tropicalfishcollection.show_all"
                            : "button.tropicalfishcollection.show_collected"));
                    recalcPages(); page = 0; updateNav();
                }).dimensions(x, sortY, w[3], 16).build());
        x += w[3] + 4;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("button.tropicalfishcollection.reset_collection"), b -> {
                    long now = System.currentTimeMillis();
                    if (now - lastResetClick < DBL_CLICK) { doReset(); }
                    else {
                        lastResetClick = now;
                        b.setMessage(Text.translatable("button.tropicalfishcollection.reset_confirm")
                                .formatted(Formatting.RED));
                    }
                }).dimensions(x, sortY, w[4], 16).build());
    }

    private void doReset() {
        PlayerCollectionState.reset();
        collected.clear();
        colorCache.clear();
        rebuildFiltered();
        page = 0;
        clearChildren(); init();
    }

    // ── Render ─────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        YACLConfig cfg = YACLConfig.HANDLER.instance();
        ctx.fill(0, 0, width, height, hex(cfg.backgroundHex, cfg.backgroundAlpha, 0xCC000000));
        super.render(ctx, mouseX, mouseY, delta);
        renderStats(ctx, cfg);
        renderGrid(ctx, cfg, mouseX, mouseY);
    }

    private void renderStats(DrawContext ctx, YACLConfig cfg) {
        int total = TropicalFishData.TOTAL_VARIANTS;
        int cnt   = collected.size();
        float pct = (float) cnt / total;

        String s  = Text.translatable("stats.tropicalfishcollection.collected", cnt, total).getString();
        int sw    = textRenderer.getWidth(s);
        int sx    = (width - sw) / 2;
        int sy    = cfg.statsOffsetY;
        ctx.fill(sx - 5, sy - 2, sx + sw + 5, sy + 10, 0x88000000);
        ctx.drawText(textRenderer, s, sx, sy, hex(cfg.statsTextHex, cfg.statsTextAlpha, 0xFFFFAA00), true);

        int bw     = Math.max(sw + 16, 160);
        int bx     = (width - bw) / 2;
        int by     = sy + 13;
        int filled = (int)(bw * pct);
        ctx.fill(bx, by, bx + bw, by + 4, 0xFF222222);
        if (filled > 0) {
            ctx.fill(bx, by, bx + filled, by + 4, pctColor(pct));
            ctx.fill(bx, by, bx + filled, by + 1, 0x33FFFFFF);
        }
        String pctStr = String.format("%.2f%%", pct * 100f);
        ctx.drawCenteredTextWithShadow(textRenderer, pctStr, width / 2, by + 6, 0xFF888888);

        // Узор текущей страницы — считаем через visible-счётчик (тот же алгоритм что в renderGrid)
        Set<Integer> pagePatterns = new HashSet<>();
        int perPage    = cols * rows;
        int skipTarget = page * perPage;
        int visible    = 0;
        int drawn      = 0;
        for (int i = 0; i < filtered.size() && drawn < perPage; i++) {
            int v = filtered.get(i);
            if (cfg.onlyShowCollected && !collected.contains(v)) continue;
            if (visible < skipTarget) { visible++; continue; }
            visible++;
            pagePatterns.add(TropicalFishData.getPatternIndex(v));
            drawn++;
        }
        if (pagePatterns.size() == 1) {
            String ps = Text.translatable("stats.tropicalfishcollection.page_pattern",
                    TropicalFishData.getPatternText(pagePatterns.iterator().next())).getString();
            ctx.drawCenteredTextWithShadow(textRenderer, ps, width / 2, by + 17, 0xFF777777);
        }

        // Номер страницы — крупно, по центру между стрелками пагинации
        int gridW   = cols * (iconSize + CELL_PAD) - CELL_PAD;
        int gridH   = rows * (iconSize + CELL_PAD) - CELL_PAD;
        int pageNavY = gridY + gridH + 8;
        ctx.drawCenteredTextWithShadow(textRenderer,
                "§e" + (page + 1) + " §7/ §e" + (maxPage + 1),
                gridX + gridW / 2, pageNavY, 0xFFFFFFFF);
    }

    /**
     * Рисует сетку вариантов на текущей странице.
     *
     * ИСПРАВЛЕНИЕ: счётчик skip теперь считает только реально видимые элементы
     * (с учётом фильтра onlyShowCollected), а не все подряд.
     * Старый баг: если onlyShowCollected=true и страница > 0,
     * skip пропускал неправильное количество элементов → на странице 2+ показывался не тот узор.
     */
    private void renderGrid(DrawContext ctx, YACLConfig cfg, int mx, int my) {
        boolean drawIcons = cfg.showBucketIcon &&
                (cfg.maxBucketIcons == 0 || cols * rows <= cfg.maxBucketIcons);

        int perPage    = cols * rows;
        int skipTarget = page * perPage; // сколько видимых элементов пропустить
        int visible    = 0;              // счётчик видимых (прошедших фильтр) элементов
        int drawn      = 0;              // сколько уже нарисовали на этой странице

        for (int i = 0; i < filtered.size() && drawn < perPage; i++) {
            int v = filtered.get(i);

            // Применяем фильтр — несобранные пропускаем целиком
            if (cfg.onlyShowCollected && !collected.contains(v)) continue;

            // Пропускаем элементы предыдущих страниц
            if (visible < skipTarget) {
                visible++;
                continue;
            }
            visible++;

            int col = drawn % cols;
            int row = drawn / cols;
            int x   = gridX + col * (iconSize + CELL_PAD);
            int y   = gridY + row * (iconSize + CELL_PAD);
            boolean have = collected.contains(v);

            // Фон ячейки
            if (have) {
                long[] c = colorCache.computeIfAbsent(v, k -> new long[]{
                        TropicalFishData.getBaseColor(k), TropicalFishData.getPatternColor(k)});
                int half = iconSize / 2;
                ctx.fill(x, y, x + half, y + iconSize, (int) c[0]);
                ctx.fill(x + half, y, x + iconSize, y + iconSize, (int) c[1]);
            } else {
                ctx.fill(x, y, x + iconSize, y + iconSize,
                        hex(cfg.uncollectedHex, cfg.uncollectedAlpha, 0xFF444444));
            }

            // Рамка ячейки
            if (cfg.cellBorderWidth > 0) {
                int bw = cfg.cellBorderWidth, bc = hex(cfg.cellBorderHex, cfg.cellBorderAlpha, 0xFF000000);
                ctx.fill(x - bw, y - bw, x + iconSize + bw, y, bc);
                ctx.fill(x - bw, y + iconSize, x + iconSize + bw, y + iconSize + bw, bc);
                ctx.fill(x - bw, y, x, y + iconSize, bc);
                ctx.fill(x + iconSize, y, x + iconSize + bw, y + iconSize, bc);
            }

            // Иконка ведра
            if (drawIcons) {
                ItemStack stack = stackCache.computeIfAbsent(v, this::makeStack);
                ctx.drawItemWithoutEntity(stack,
                        x + (iconSize - ICON_BASE) / 2 + cfg.bucketOffsetX,
                        y + (iconSize - ICON_BASE) / 2 + cfg.bucketOffsetY);
            }

            // Галочка для собранных
            if (have) ctx.drawText(textRenderer, "✔",
                    x + iconSize - 10, y + 2,
                    hex(cfg.checkHex, cfg.checkAlpha, 0xFF00FF00), true);

            // Тултип при наведении
            if (mx >= x && mx < x + iconSize && my >= y && my < y + iconSize
                    && cfg.tooltipMode != YACLConfig.TooltipMode.NEVER) {
                ctx.drawTooltip(textRenderer, List.of(
                        Text.translatable("tooltip.tropicalfishcollection.pattern",
                                TropicalFishData.getPatternText(TropicalFishData.getPatternIndex(v))),
                        Text.translatable("tooltip.tropicalfishcollection.base_color",
                                TropicalFishData.getColoredColorText(TropicalFishData.getBaseColorIndex(v))),
                        Text.translatable("tooltip.tropicalfishcollection.pattern_color",
                                TropicalFishData.getColoredColorText(TropicalFishData.getPatternColorIndex(v)))
                ), mx, my);
            }
            drawn++;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private int hex(String hex, int alpha, int fallback) {
        try { if (hex.startsWith("#")) hex = hex.substring(1); return (alpha << 24) | Integer.parseInt(hex, 16); }
        catch (Exception e) { return fallback; }
    }

    private int pctColor(float p) {
        if (p >= .9f) return 0xFF00FF88;
        if (p >= .5f) return 0xFF44EE44;
        if (p >= .2f) return 0xFFFFEE22;
        return 0xFFFF4422;
    }

    private ItemStack makeStack(int v) {
        ItemStack s = new ItemStack(Items.TROPICAL_FISH_BUCKET);
        s.set(DataComponentTypes.TROPICAL_FISH_BASE_COLOR,    DyeColor.values()[TropicalFishData.getBaseColorIndex(v)]);
        s.set(DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR, DyeColor.values()[TropicalFishData.getPatternColorIndex(v)]);
        s.set(DataComponentTypes.TROPICAL_FISH_PATTERN,       TropicalFishEntity.Pattern.values()[TropicalFishData.getPatternIndex(v)]);
        return s;
    }

    private void updateNav() { prevBtn.active = page > 0; nextBtn.active = page < maxPage; }

    // ── Edit / Free mode ───────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean lmb = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        double mx = MinecraftClient.getInstance().mouse.getX();
        double my = MinecraftClient.getInstance().mouse.getY();
        if (editMode || freeMode) handleDrag(lmb, mx, my);
    }

    private void handleDrag(boolean lmb, double mx, double my) {
        if (lmb) {
            if (!dragging) {
                dragTarget = freeMode ? 0 : hitTest((int) mx, (int) my);
                if (dragTarget >= 0) { dragX = mx; dragY = my; dragging = true; }
            } else {
                int dx = (int)(mx - dragX), dy = (int)(my - dragY);
                YACLConfig cfg = YACLConfig.HANDLER.instance();
                if (freeMode) { cfg.gridOffsetX += dx; cfg.gridOffsetY += dy; YACLConfig.HANDLER.save(); clearChildren(); init(); }
                else applyDrag(dx, dy, cfg);
                dragX = mx; dragY = my;
            }
        } else { dragging = false; dragTarget = -1; }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double ha, double va) {
        if (freeMode) {
            YACLConfig cfg = YACLConfig.HANDLER.instance();
            cfg.iconScale = Math.clamp(cfg.iconScale + (int) Math.signum(-va), 10, 40);
            YACLConfig.HANDLER.save(); clearChildren(); init(); return true;
        }
        return super.mouseScrolled(mx, my, ha, va);
    }

    private int hitTest(int mx, int my) {
        int gw = cols * (iconSize + CELL_PAD) - CELL_PAD, gh = rows * (iconSize + CELL_PAD) - CELL_PAD;
        if (mx >= gridX && mx < gridX + gw && my >= gridY && my < gridY + gh) return 0;
        int sy = YACLConfig.HANDLER.instance().statsOffsetY;
        if (mx >= (width-200)/2 && mx < (width+200)/2 && my >= sy-5 && my < sy+25) return 1;
        int by2 = height - 20 + YACLConfig.HANDLER.instance().buttonOffsetY;
        if (my >= by2 - 4 && my < by2 + 20) return 2;
        return -1;
    }

    private void applyDrag(int dx, int dy, YACLConfig cfg) {
        switch (dragTarget) {
            case 0 -> { cfg.gridOffsetX += dx; cfg.gridOffsetY += dy; }
            case 1 -> cfg.statsOffsetY += dy;
            case 2 -> cfg.buttonOffsetY += dy;
        }
        YACLConfig.HANDLER.save(); clearChildren(); init();
    }

    private void moveElem(int dx, int dy) {
        YACLConfig cfg = YACLConfig.HANDLER.instance();
        switch (selElem) {
            case 0 -> { cfg.gridOffsetX += dx; cfg.gridOffsetY += dy; }
            case 1 -> cfg.statsOffsetY += dy;
            case 2 -> cfg.buttonOffsetY += dy;
        }
        YACLConfig.HANDLER.save(); clearChildren(); init();
    }

    private void scaleIcon(int d) {
        YACLConfig cfg = YACLConfig.HANDLER.instance();
        cfg.iconScale = Math.clamp(cfg.iconScale + d, 8, 40);
        YACLConfig.HANDLER.save(); clearChildren(); init();
    }

    // ── Export ─────────────────────────────────────────────────────────────

    private void doExport() {
        Path out = MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config/tropicalfishcollection/exported_fish.txt");
        try {
            Files.createDirectories(out.getParent());
            try (PrintWriter pw = new PrintWriter(new FileWriter(out.toFile()))) {
                pw.printf("# Tropical Fish Collection — %d / %d%n", collected.size(), TropicalFishData.TOTAL_VARIANTS);
                for (int v : collected) {
                    pw.printf("%d | %-14s | %-12s | %s%n", v,
                            TropicalFishData.getPatternText(TropicalFishData.getPatternIndex(v)).getString(),
                            TropicalFishData.getColorText(TropicalFishData.getBaseColorIndex(v)).getString(),
                            TropicalFishData.getColorText(TropicalFishData.getPatternColorIndex(v)).getString());
                }
            }
            if (MinecraftClient.getInstance().player != null)
                MinecraftClient.getInstance().player.sendMessage(Text.literal("§aExported → " + out), false);
        } catch (Exception e) { TropicalFishCollection.LOGGER.error("Export failed", e); }
    }

    @Override public boolean shouldPause() { return false; }
}