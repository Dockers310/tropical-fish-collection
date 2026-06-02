package com.dok_si.tropicalfish.network;

import com.dok_si.tropicalfish.PlayerCollectionState;
import com.dok_si.tropicalfish.TropicalFishCollection;
import com.dok_si.tropicalfish.leaderboard.FriendLeaderboard;
import com.dok_si.tropicalfish.leaderboard.PeerLeaderboard;
import net.minecraft.client.MinecraftClient;

/**
 * Транспорт лидерборда — ТОЛЬКО через /tell (sendCommand, не sendChatMessage).
 *
 * ИСПРАВЛЕНИЕ ГЛАВНОГО БАГА:
 *   БЫЛО:   client.player.networkHandler.sendChatMessage("/tell НИК TFC_DATA|...")
 *           → отправляет строку "/tell НИК ..." в ОБЩИЙ ЧАТ как обычный текст
 *
 *   СТАЛО:  client.player.networkHandler.sendCommand("tell НИК TFC_DATA|...")
 *           → выполняет команду /tell на сервере — видна ТОЛЬКО получателю
 *
 * sendCommand принимает строку БЕЗ слэша в начале.
 */
public class ChatLeaderboardTransport {

    public static void onJoinServer() {
        String name  = getSelfName();
        int    count = PlayerCollectionState.getCollectedCount();
        PeerLeaderboard.initSelf(name, count);
        FriendLeaderboard.load();
        TropicalFishCollection.LOGGER.info(
                "[TFC] Joined as {} with {} fish. Friends: {}", name, count, FriendLeaderboard.size());
    }

    public static void onLeaveServer() {
        FriendLeaderboard.onLeaveServer();
        PeerLeaderboard.clear();
    }

    public static void tick() {
        // нет автоматических рассылок
    }

    public static void onCollectionChanged() {
        PeerLeaderboard.updateSelf(PlayerCollectionState.getCollectedCount());
    }

    /**
     * Перехватывает входящие сообщения чата.
     * Вызывается из MessageMixin.
     * @return true — скрыть сообщение от чата
     */
    public static boolean tryHandleChatMessage(String rawMessage) {
        if (FriendLeaderboard.tryHandle(rawMessage)) return true;
        if (rawMessage.contains("\u200B\u200C[TFC|")) return true;
        return false;
    }

    private static String getSelfName() {
        MinecraftClient client = MinecraftClient.getInstance();
        return (client.player != null) ? client.player.getName().getString() : "";
    }
}
