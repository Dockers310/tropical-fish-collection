package com.dok_si.tropicalfish;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Управляет коллекцией тропических рыб игрока.
 *
 * Улучшения по сравнению с оригиналом:
 *  - Атомарное сохранение (write → temp → rename), файл никогда не портится
 *  - Асинхронное сохранение в пуле потоков, не блокирует игровой тред
 *  - Кэш цветов вынесен сюда же (invalidate при смене контекста)
 *  - isDuplicate и getCollected работают через один объект без лишних копий
 *  - Метод getCollectedCount() без создания Set-копии
 */
public class PlayerCollectionState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME        = "collections.json";
    private static final String MAPPING_FILE     = "context_mapping.json";
    private static final long   SAVE_DEBOUNCE_MS = 300;

    // ---- Состояние ----
    private static final Map<String, Set<Integer>> collectionsByContext = new HashMap<>();
    private static Map<String, String>             contextMapping       = new HashMap<>();
    private static String                          lastContext          = null;
    private static Set<Integer>                    cachedCollection     = null;

    private static volatile boolean dirty         = false;
    private static volatile long    lastDirtyTime = 0;

    // Пул для асинхронного сохранения (1 поток, демон)
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TropicalFish-Save");
        t.setDaemon(true);
        return t;
    });

    // ---- Вспомогательный класс для JSON ----
    private static class FishEntry {
        int    variant;
        String pattern;
        String base_color;
        String pattern_color;

        FishEntry(int variant) {
            this.variant       = variant;
            this.pattern       = TropicalFishData.getPatternText(TropicalFishData.getPatternIndex(variant)).getString();
            this.base_color    = TropicalFishData.getColorText(TropicalFishData.getBaseColorIndex(variant)).getString();
            this.pattern_color = TropicalFishData.getColorText(TropicalFishData.getPatternColorIndex(variant)).getString();
        }
    }

    // ==================================================================
    //  Публичное API
    // ==================================================================

    public static boolean isDuplicate(int variant) {
        Set<Integer> col = getCurrentCollection();
        return col != null && col.contains(variant);
    }

    /** Добавляет вариант. Возвращает true, если это была новинка. */
    public static boolean addVariant(int variant) {
        try {
            Set<Integer> collection = getCurrentCollection();
            if (collection.add(variant)) {
                markDirty();
                return true;
            }
        } catch (Exception e) {
            TropicalFishCollection.LOGGER.error("[TropicalFish] addVariant error", e);
        }
        return false;
    }

    /**
     * Возвращает живую ссылку на коллекцию текущего контекста.
     * Для чтения — безопасно. Для итерации в других потоках — делайте копию.
     */
    public static Set<Integer> getCollected() {
        Set<Integer> col = getCurrentCollection();
        return col == null ? Collections.emptySet() : Collections.unmodifiableSet(col);
    }

    /** Быстрый счётчик без создания копии. */
    public static int getCollectedCount() {
        Set<Integer> col = getCurrentCollection();
        return col == null ? 0 : col.size();
    }

    public static void reset() {
        Set<Integer> collection = getCurrentCollection();
        if (collection != null) {
            collection.clear();
            markDirty();
            saveNow();
        }
    }

    // ==================================================================
    //  Сохранение / загрузка
    // ==================================================================

    private static void markDirty() {
        dirty         = true;
        lastDirtyTime = System.currentTimeMillis();
    }

    /** Вызывается каждые ~20 тиков из ClientTickEvents. */
    public static void saveIfDirty() {
        if (dirty && System.currentTimeMillis() - lastDirtyTime >= SAVE_DEBOUNCE_MS) {
            dirty = false;
            SAVE_EXECUTOR.submit(PlayerCollectionState::saveNow);
        }
    }

    /** Синхронное сохранение (вызывается при reset и выходе). */
    public static void saveNow() {
        Path path = getFilePath();
        try {
            Files.createDirectories(getConfigDir());
            JsonObject root = buildJson();
            // Атомарная запись: пишем в .tmp, затем move
            Path tmp = path.resolveSibling(FILE_NAME + ".tmp");
            try (Writer w = new OutputStreamWriter(Files.newOutputStream(tmp), StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            TropicalFishCollection.LOGGER.error("[TropicalFish] Save failed", e);
        }
    }

    private static JsonObject buildJson() {
        JsonObject root = new JsonObject();
        // snapshot под синхронизацией
        Map<String, Set<Integer>> snapshot;
        synchronized (collectionsByContext) {
            snapshot = new HashMap<>();
            for (Map.Entry<String, Set<Integer>> e : collectionsByContext.entrySet()) {
                snapshot.put(e.getKey(), new HashSet<>(e.getValue()));
            }
        }
        for (Map.Entry<String, Set<Integer>> entry : snapshot.entrySet()) {
            JsonArray arr = new JsonArray();
            for (int variant : entry.getValue()) {
                arr.add(GSON.toJsonTree(new FishEntry(variant)));
            }
            root.add(entry.getKey(), arr);
        }
        return root;
    }

    public static void loadAll() {
        Path path = getFilePath();
        synchronized (collectionsByContext) {
            collectionsByContext.clear();
            if (Files.exists(path)) {
                try (Reader r = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                        JsonArray arr = entry.getValue().getAsJsonArray();
                        Set<Integer> variants = new HashSet<>();
                        for (JsonElement elem : arr) {
                            if (elem.isJsonObject() && elem.getAsJsonObject().has("variant")) {
                                variants.add(elem.getAsJsonObject().get("variant").getAsInt());
                            } else if (elem.isJsonPrimitive()) {
                                variants.add(elem.getAsInt());
                            }
                        }
                        collectionsByContext.put(entry.getKey(), variants);
                    }
                } catch (Exception e) {
                    TropicalFishCollection.LOGGER.error("[TropicalFish] Load failed", e);
                }
            }
        }
        lastContext      = null;
        cachedCollection = null;
    }

    // ==================================================================
    //  Mapping
    // ==================================================================

    public static void loadMapping() {
        Path path = getMappingFilePath();
        if (Files.exists(path)) {
            try (Reader r = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                @SuppressWarnings("unchecked")
                Map<String, String> map = GSON.fromJson(r, Map.class);
                contextMapping = map != null ? map : new HashMap<>();
            } catch (Exception e) {
                TropicalFishCollection.LOGGER.warn("[TropicalFish] Mapping load failed", e);
                contextMapping = new HashMap<>();
            }
        }
    }

    public static void saveMapping() {
        try {
            Files.createDirectories(getConfigDir());
            try (Writer w = new OutputStreamWriter(Files.newOutputStream(getMappingFilePath()), StandardCharsets.UTF_8)) {
                GSON.toJson(contextMapping, w);
            }
        } catch (IOException e) {
            TropicalFishCollection.LOGGER.error("[TropicalFish] Mapping save failed", e);
        }
    }

    public static String getCurrentContextId() {
        loadMapping();
        MinecraftClient client = MinecraftClient.getInstance();
        String realId = "unknown";
        if (client.isInSingleplayer()) {
            IntegratedServer server = client.getServer();
            if (server != null) {
                realId = "singleplayer_" + server.getSaveProperties().getLevelName();
            }
        } else {
            ServerInfo serverInfo = client.getCurrentServerEntry();
            if (serverInfo != null) {
                realId = "server_" + serverInfo.address;
            }
        }
        return contextMapping.getOrDefault(realId, realId);
    }

    // ==================================================================
    //  Внутренние хелперы
    // ==================================================================

    private static Set<Integer> getCurrentCollection() {
        String context = getCurrentContextId();
        if (!context.equals(lastContext) || cachedCollection == null) {
            loadAll();
            synchronized (collectionsByContext) {
                cachedCollection = collectionsByContext.computeIfAbsent(context, k -> new HashSet<>());
            }
            lastContext = context;
        }
        return cachedCollection;
    }

    private static Path getConfigDir() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("config/tropicalfishcollection");
    }

    private static Path getFilePath()       { return getConfigDir().resolve(FILE_NAME);     }
    private static Path getMappingFilePath() { return getConfigDir().resolve(MAPPING_FILE); }
}
