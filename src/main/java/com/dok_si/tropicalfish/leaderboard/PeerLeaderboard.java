package com.dok_si.tropicalfish.leaderboard;

import com.dok_si.tropicalfish.TropicalFishData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Клиентский реестр лидерборда.
 *
 * Ключевые моменты:
 * - Себя добавляем ВСЕГДА при инициализации, независимо от сети
 * - Обновляем себя при каждом изменении коллекции
 * - Чужих добавляем когда получаем пакет
 * - ConcurrentHashMap — безопасно писать из netty-потока, читать из render-потока
 */
public class PeerLeaderboard {

    private static final ConcurrentHashMap<String, Integer> scores    = new ConcurrentHashMap<>();
    private static volatile String                           selfName  = "";

    // ── Обновление ──────────────────────────────────────────────────────

    /** Обновить запись игрока (себя или чужого). */
    public static void update(String playerName, int count) {
        if (playerName == null || playerName.isBlank()) return;
        scores.put(playerName, Math.max(0, count));
    }

    /** Удалить игрока из списка (вышел с сервера). */
    public static void remove(String playerName) {
        // Себя не удаляем
        if (!playerName.equals(selfName)) {
            scores.remove(playerName);
        }
    }

    /**
     * Установить имя «себя» и сразу добавить в таблицу.
     * Вызывать при входе в мир/на сервер.
     */
    public static void initSelf(String name, int count) {
        selfName = name;
        scores.put(name, count);
    }

    /** Обновить только свой счёт (при добавлении новой рыбы). */
    public static void updateSelf(int count) {
        if (!selfName.isEmpty()) {
            scores.put(selfName, count);
        }
    }

    /** Полная очистка при выходе (себя тоже убираем). */
    public static void clear() {
        scores.clear();
        selfName = "";
    }

    // ── Чтение ──────────────────────────────────────────────────────────

    public static String getSelfName() { return selfName; }

    public static int size() { return scores.size(); }

    /** Отсортированный снимок по убыванию счёта. Потокобезопасен. */
    public static List<Entry> getSortedSnapshot() {
        List<Entry> list = new ArrayList<>(scores.size());
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            list.add(new Entry(e.getKey(), e.getValue()));
        }
        list.sort(Comparator.comparingInt(Entry::count).reversed()
                .thenComparing(Entry::playerName));
        return list;
    }

    /** Позиция (1-based) для selfName, или -1. */
    public static int getSelfRank() {
        if (selfName.isEmpty()) return -1;
        List<Entry> sorted = getSortedSnapshot();
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).playerName().equals(selfName)) return i + 1;
        }
        return -1;
    }

    // ── Entry ────────────────────────────────────────────────────────────

    public record Entry(String playerName, int count) {
        public float progress() {
            return (float) count / TropicalFishData.TOTAL_VARIANTS;
        }
    }
}
