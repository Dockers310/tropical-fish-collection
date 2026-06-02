package com.dok_si.tropicalfish.screen;

import com.dok_si.tropicalfish.TropicalFishCollection;
import com.dok_si.tropicalfish.TropicalFishData;
import com.dok_si.tropicalfish.leaderboard.FriendLeaderboard;
import com.dok_si.tropicalfish.leaderboard.PeerLeaderboard;
import com.dok_si.tropicalfish.network.ChatLeaderboardTransport;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Экран лидерборда с двумя вкладками: Друзья и Онлайн.
 *
 * ИСПРАВЛЕНИЕ: кнопка «Отправить запрос» теперь вызывает
 * FriendLeaderboard.sendRequest() который использует sendCommand (не sendChatMessage).
 */
public class LeaderboardScreen extends Screen {

    private static final int ROWS_PER_PAGE = 16;
    private static final int ROW_H         = 15;

    private final Screen parent;
    private int page = 0;

    /** 0 = Друзья, 1 = Онлайн */
    private int activeTab = 0;

    private static final int[] MEDAL_COLORS = {0xFFFFD700, 0xFFC0C0C0, 0xFFCD7F32};

    private TextFieldWidget addFriendField;
    private boolean showAddFriend = false;

    public LeaderboardScreen(Screen parent) {
        super(Text.translatable("screen." + TropicalFishCollection.MOD_ID + ".leaderboard"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;

        // Кнопка «Назад»
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("button." + TropicalFishCollection.MOD_ID + ".back"),
                btn -> close()
        ).dimensions(cx - 55, height - 24, 50, 16).build());

        // Вкладки
        addDrawableChild(ButtonWidget.builder(
                Text.literal(activeTab == 0 ? "§e§l👥 Друзья" : "§7👥 Друзья"),
                btn -> { activeTab = 0; page = 0; showAddFriend = false; clearChildren(); init(); }
        ).dimensions(cx - 110, 20, 100, 14).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(activeTab == 1 ? "§e§l📡 Онлайн" : "§7📡 Онлайн"),
                btn -> { activeTab = 1; page = 0; showAddFriend = false; clearChildren(); init(); }
        ).dimensions(cx + 10, 20, 100, 14).build());

        // Пагинация
        addDrawableChild(ButtonWidget.builder(Text.literal("◀"),
                        btn -> { if (page > 0) page--; })
                .dimensions(cx - 110, height - 24, 16, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶"),
                        btn -> { if (page < maxPage()) page++; })
                .dimensions(cx + 94, height - 24, 16, 16).build());

        if (activeTab == 0) {
            // Кнопка добавления друга
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(showAddFriend ? "§c✕ Закрыть" : "§a+ Добавить друга"),
                    btn -> { showAddFriend = !showAddFriend; clearChildren(); init(); }
            ).dimensions(cx + 5, height - 24, 90, 16).build());

            if (showAddFriend) {
                addFriendField = new TextFieldWidget(textRenderer,
                        cx - 80, height - 48, 130, 14, Text.literal(""));
                addFriendField.setPlaceholder(Text.literal("Введите ник игрока..."));
                addDrawableChild(addFriendField);

                addDrawableChild(ButtonWidget.builder(
                        Text.literal("§eОтправить"),
                        btn -> {
                            if (addFriendField != null && !addFriendField.getText().isBlank()) {
                                String nick = addFriendField.getText().trim();
                                // ИСПРАВЛЕНИЕ: FriendLeaderboard.sendRequest использует sendCommand
                                FriendLeaderboard.sendRequest(nick);
                                showAddFriend = false;
                                clearChildren(); init();
                            }
                        }
                ).dimensions(cx + 55, height - 48, 80, 14).build());
            }
        }
    }

    private int maxPage() {
        int s = activeTab == 0 ? FriendLeaderboard.size() : PeerLeaderboard.size();
        return s == 0 ? 0 : (s - 1) / ROWS_PER_PAGE;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xEE0A0A14);
        super.render(ctx, mouseX, mouseY, delta);

        int cx = width / 2;

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("🏆 ").append(
                        Text.translatable("screen." + TropicalFishCollection.MOD_ID + ".leaderboard")
                                .formatted(Formatting.GOLD, Formatting.BOLD)),
                cx, 6, -1);

        if (activeTab == 0) renderFriendsTab(ctx, cx);
        else                renderOnlineTab(ctx, cx);

        if (maxPage() > 0) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    (page + 1) + " / " + (maxPage() + 1), cx, height - 20, 0xFF444455);
        }
    }

    // ── Вкладка «Друзья» ─────────────────────────────────────────────────

    private void renderFriendsTab(DrawContext ctx, int cx) {
        List<FriendLeaderboard.FriendEntry> friends = FriendLeaderboard.getSortedFriends();

        int hy = 38;

        // Инструкция по добавлению
        ctx.drawCenteredTextWithShadow(textRenderer,
                "§7Добавить: §e/tell §bНИК §eTFC_TOP §7→ нажать кнопку ниже", cx, hy, -1);

        if (friends.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("leaderboard." + TropicalFishCollection.MOD_ID + ".empty_friends")
                            .formatted(Formatting.GRAY),
                    cx, height / 2, -1);
            return;
        }

        int tx = cx - 185, ty = hy + 14, tw = 370;
        renderTableHeader(ctx, tx, ty, tw);
        ty += 14;

        String self  = PeerLeaderboard.getSelfName();
        int start = page * ROWS_PER_PAGE;
        int end   = Math.min(start + ROWS_PER_PAGE, friends.size());

        for (int i = start; i < end; i++) {
            FriendLeaderboard.FriendEntry e = friends.get(i);
            int ry = ty + (i - start) * ROW_H, rank = i + 1;
            boolean me = e.name().equals(self);
            renderRowBg(ctx, tx, ry, tw, i - start, me, rank);
            renderRank(ctx, tx, ry, rank);
            renderName(ctx, tx, ry, e.name(), me, rank);
            ctx.drawTextWithShadow(textRenderer, String.valueOf(e.count()), tx + 228, ry + 3, 0xFFFFFF44);
            renderProgressBar(ctx, tx + 272, ry, 68, ROW_H, e.progress());
        }
    }

    // ── Вкладка «Онлайн» ─────────────────────────────────────────────────

    private void renderOnlineTab(DrawContext ctx, int cx) {
        List<PeerLeaderboard.Entry> all  = PeerLeaderboard.getSortedSnapshot();
        String                      self = PeerLeaderboard.getSelfName();

        ctx.drawCenteredTextWithShadow(textRenderer,
                "§7Игроки с модом в текущей сессии", cx, 38, -1);

        if (all.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("leaderboard." + TropicalFishCollection.MOD_ID + ".empty")
                            .formatted(Formatting.GRAY), cx, height / 2, -1);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "§7Используйте вкладку «Друзья» для надёжного топа",
                    cx, height / 2 + 14, -1);
            return;
        }

        int tx = cx - 185, ty = 52, tw = 370;
        renderTableHeader(ctx, tx, ty, tw);
        ty += 14;

        int start = page * ROWS_PER_PAGE;
        int end   = Math.min(start + ROWS_PER_PAGE, all.size());

        for (int i = start; i < end; i++) {
            PeerLeaderboard.Entry e = all.get(i);
            int ry = ty + (i - start) * ROW_H, rank = i + 1;
            boolean me = e.playerName().equals(self);
            renderRowBg(ctx, tx, ry, tw, i - start, me, rank);
            renderRank(ctx, tx, ry, rank);
            renderName(ctx, tx, ry, e.playerName(), me, rank);
            ctx.drawTextWithShadow(textRenderer, String.valueOf(e.count()), tx + 228, ry + 3, 0xFFFFFF44);
            renderProgressBar(ctx, tx + 272, ry, 68, ROW_H, e.progress());
        }
    }

    // ── Общие элементы таблицы ────────────────────────────────────────────

    private void renderTableHeader(DrawContext ctx, int tx, int ty, int tw) {
        ctx.fill(tx - 2, ty, tx + tw, ty + 12, 0x55223344);
        ctx.drawTextWithShadow(textRenderer, "#",        tx,       ty + 1, 0xFF667788);
        ctx.drawTextWithShadow(textRenderer, "Игрок",    tx + 24,  ty + 1, 0xFF667788);
        ctx.drawTextWithShadow(textRenderer, "Рыб",      tx + 228, ty + 1, 0xFF667788);
        ctx.drawTextWithShadow(textRenderer, "Прогресс", tx + 272, ty + 1, 0xFF667788);
    }

    private void renderRowBg(DrawContext ctx, int tx, int ry, int tw, int idx, boolean me, int rank) {
        int bg = idx % 2 == 0 ? 0x0FFFFFFF : 0;
        if (me) bg = 0x1A44AAFF;
        if (rank <= 3) bg |= switch (rank) {
            case 1 -> 0x14FFD700; case 2 -> 0x0EC0C0C0; default -> 0x0ECD7F32;
        };
        ctx.fill(tx - 2, ry, tx + tw, ry + ROW_H - 1, bg);
    }

    private void renderRank(DrawContext ctx, int tx, int ry, int rank) {
        String rankStr  = rank <= 3 ? ("#" + rank) : String.valueOf(rank);
        int rankColor   = rank <= 3 ? MEDAL_COLORS[rank - 1] : 0xFF555566;
        ctx.drawTextWithShadow(textRenderer, rankStr, tx, ry + 3, rankColor);
    }

    private void renderName(DrawContext ctx, int tx, int ry, String name, boolean me, int rank) {
        int nc = me ? 0xFF55CCFF : (rank <= 3 ? MEDAL_COLORS[rank - 1] : 0xFFCCCCCC);
        if (textRenderer.getWidth(name) > 188) name = textRenderer.trimToWidth(name, 185) + "…";
        if (me) name = "► " + name;
        ctx.drawTextWithShadow(textRenderer, name, tx + 24, ry + 3, nc);
    }

    private void renderProgressBar(DrawContext ctx, int bx, int ry, int bw, int rowH, float pct) {
        int bh = 5, by2 = ry + (rowH - bh) / 2;
        int filled = (int)(bw * pct);
        ctx.fill(bx, by2, bx + bw, by2 + bh, 0xFF1A1A1A);
        if (filled > 0) {
            int bc = pct >= .9f ? 0xFF00FF88 : pct >= .5f ? 0xFF44EE44 : pct >= .2f ? 0xFFFFEE22 : 0xFFFF4422;
            ctx.fill(bx, by2, bx + filled, by2 + bh, bc);
            ctx.fill(bx, by2, bx + filled, by2 + 1, 0x33FFFFFF);
        }
        ctx.drawTextWithShadow(textRenderer,
                String.format("%.1f%%", pct * 100f), bx + bw + 3, ry + 3, 0xFF666677);
    }

    @Override public void close() { assert client != null; client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
