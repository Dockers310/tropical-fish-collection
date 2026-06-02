package com.dok_si.tropicalfish.leaderboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.dok_si.tropicalfish.PlayerCollectionState;
import com.dok_si.tropicalfish.TropicalFishCollection;
import com.dok_si.tropicalfish.TropicalFishData;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ИСПРАВЛЕНИЕ: sendChatCommand (PUBLIC) вместо sendCommand (PRIVATE).
 *
 * sendCommand в ClientPlayNetworkHandler является PRIVATE методом —
 * он недоступен снаружи и не компилируется.
 *
 * sendChatCommand(String command) — PUBLIC метод, принимает команду
 * БЕЗ слэша, выполняет её на сервере. НЕ отправляет в публичный чат.
 */
public class FriendLeaderboard {

    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "friends.json";

    public static final String REQUEST_KEY  = "TFC_TOP";
    public static final String RESPONSE_KEY = "TFC_DATA";

    private static final Map<String, FriendEntry> friends = new ConcurrentHashMap<>();

    public static void updateFriend(String name, int count) {
        friends.put(name, new FriendEntry(name, count, System.currentTimeMillis()));
        save();
        PeerLeaderboard.update(name, count);
        TropicalFishCollection.LOGGER.info("[TFC-Friends] Updated: {} = {} fish", name, count);
    }

    public static List<FriendEntry> getSortedFriends() {
        List<FriendEntry> list = new ArrayList<>(friends.values());
        list.sort(Comparator.comparingInt(FriendEntry::count).reversed());
        return list;
    }

    public static void removeFriend(String name) { friends.remove(name); save(); }
    public static int  size()                     { return friends.size(); }
    public static void onLeaveServer()            { }

    public static boolean tryHandle(String rawMessage) {
        if (rawMessage.contains(RESPONSE_KEY + "|")) {
            try {
                int idx     = rawMessage.indexOf(RESPONSE_KEY + "|");
                String rest = rawMessage.substring(idx + RESPONSE_KEY.length() + 1);
                int count   = Integer.parseInt(rest.split("[|\\s]")[0].trim());
                String sender = extractSender(rawMessage);
                if (sender != null && !sender.isBlank()) updateFriend(sender, count);
            } catch (Exception e) {
                TropicalFishCollection.LOGGER.debug("[TFC] Response parse error: {}", rawMessage);
            }
            return true;
        }
        if (isWhisper(rawMessage) && rawMessage.contains(REQUEST_KEY) && !rawMessage.contains(RESPONSE_KEY)) {
            String sender = extractSender(rawMessage);
            if (sender != null && !sender.isBlank()) respondToFriend(sender);
            return true;
        }
        return false;
    }

    /**
     * ИСПРАВЛЕНИЕ: sendChatCommand (public) вместо sendCommand (private).
     * Строка БЕЗ слэша. Команда выполняется тихо, не пишется в чат отправителя.
     */
    public static void respondToFriend(String targetName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;
        int count = PlayerCollectionState.getCollectedCount();
        // sendChatCommand — PUBLIC, принимает команду без слэша
        client.getNetworkHandler().sendChatCommand(
                "tell " + sanitize(targetName) + " " + RESPONSE_KEY + "|" + count);
        TropicalFishCollection.LOGGER.debug("[TFC] Responded to {}: {} fish", targetName, count);
    }

    /**
     * ИСПРАВЛЕНИЕ: sendChatCommand (public) вместо sendCommand (private).
     */
    public static void sendRequest(String targetName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;
        // sendChatCommand — PUBLIC, принимает команду без слэша
        client.getNetworkHandler().sendChatCommand(
                "tell " + sanitize(targetName) + " " + REQUEST_KEY);
        TropicalFishCollection.LOGGER.info("[TFC] Request sent to {}", targetName);
    }

    public static void load() {
        Path path = getFilePath();
        if (!Files.exists(path)) return;
        try (Reader r = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, FriendEntry>>(){}.getType();
            Map<String, FriendEntry> loaded = GSON.fromJson(r, type);
            if (loaded != null) {
                friends.clear(); friends.putAll(loaded);
                for (FriendEntry e : friends.values()) PeerLeaderboard.update(e.name(), e.count());
            }
        } catch (Exception e) {
            TropicalFishCollection.LOGGER.warn("[TFC] Friends load failed: {}", e.getMessage());
        }
    }

    public static void save() {
        Path path = getFilePath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8)) {
                GSON.toJson(friends, w);
            }
        } catch (Exception e) {
            TropicalFishCollection.LOGGER.error("[TFC] Friends save failed: {}", e.getMessage());
        }
    }

    private static String extractSender(String msg) {
        if (msg.contains(" -> ")) {
            int end = msg.indexOf(" -> ");
            String c = msg.substring(0, end).replaceAll("[<>\\[\\]§\\p{C}]", "").trim();
            if (isValidName(c)) return c;
        }
        for (String m : new String[]{" whispers", " шепчет", " tells you"}) {
            int idx = msg.indexOf(m);
            if (idx > 0) {
                String c = msg.substring(0, idx).replaceAll("[<>\\[\\]§\\p{C}]", "").trim();
                if (isValidName(c)) return c;
            }
        }
        return null;
    }

    private static boolean isWhisper(String msg) {
        return msg.contains(" -> ") || msg.contains("whispers")
                || msg.contains("шепчет") || msg.contains("tells you");
    }

    private static boolean isValidName(String n) {
        return !n.isEmpty() && n.length() <= 16 && n.matches("[a-zA-Z0-9_]+");
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "");
    }

    private static Path getFilePath() {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config/tropicalfishcollection/" + FILE_NAME);
    }

    public record FriendEntry(String name, int count, long lastSeen) {
        public float progress() { return (float) count / TropicalFishData.TOTAL_VARIANTS; }
    }
}
