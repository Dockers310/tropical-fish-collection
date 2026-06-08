package com.dok_si.tropicalfish;

import com.dok_si.tropicalfish.client.BucketDecorator;
import com.dok_si.tropicalfish.client.FishHighlightRenderer;
import com.dok_si.tropicalfish.config.YACLConfig;
import com.dok_si.tropicalfish.leaderboard.FriendLeaderboard;
import com.dok_si.tropicalfish.leaderboard.PeerLeaderboard;
import com.dok_si.tropicalfish.network.ChatLeaderboardTransport;
import com.dok_si.tropicalfish.screen.CollectionScreen;
import com.dok_si.tropicalfish.toast.NewFishToast;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.Map;
import java.util.HashMap;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TropicalFishCollectionClient implements ClientModInitializer {

    private static KeyBinding openCollectionKey;
    public static final Map<Integer, TropicalFishEntity> entityCache = new HashMap<>();
    private static int tickCounter    = 0;
    private static final int CHECK_INTERVAL     = 5;
    private static final int SAVE_INTERVAL      = 100;
    private static final int AGGREGATE_DELAY    = 20;
    private static final int HIGHLIGHT_INTERVAL = 10; // обновляем glowing каждые 10 тиков

    private static final List<Integer> pendingNew     = new ArrayList<>();
    private static int ticksSinceLastNew = -1;

    @Override
    public void onInitializeClient() {
        try {
            Files.createDirectories(
                    FabricLoader.getInstance().getConfigDir().resolve("tropicalfishcollection"));
        } catch (IOException e) {
            TropicalFishCollection.LOGGER.error("Config dir creation failed", e);
        }

        YACLConfig.HANDLER.load();

        // ── Горячие клавиши ───────────────────────────────────────────────
        KeyBindingHelper.registerKeyBinding(BucketDecorator.TOGGLE_INDICATORS);
        openCollectionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tropicalfishcollection.open_collection",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                new KeyBinding.Category(Identifier.of(TropicalFishCollection.MOD_ID, "category"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            while (openCollectionKey.wasPressed()) c.setScreen(new CollectionScreen());
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        // ── JOIN ─────────────────────────────────────────────────────────
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PlayerCollectionState.loadAll();
            FriendLeaderboard.load();
            String self  = client.player != null ? client.player.getName().getString() : "";
            int    count = PlayerCollectionState.getCollectedCount();
            PeerLeaderboard.initSelf(self, count);
            ChatLeaderboardTransport.onJoinServer();
            MilestoneManager.silentSyncOnLoad();
        });

        // ── DISCONNECT ────────────────────────────────────────────────────
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FishHighlightRenderer.clearHighlight();
            PeerLeaderboard.clear();
            ChatLeaderboardTransport.onLeaveServer();
            PlayerCollectionState.saveNow();
            entityCache.clear();

        });
    }

    // ── Тик ───────────────────────────────────────────────────────────────

    private void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || player.isSpectator()) return;

        YACLConfig config = YACLConfig.HANDLER.instance();
        tickCounter++;

        ChatLeaderboardTransport.tick();

        // Подсветка несобранных рыб (glowing)
        if (tickCounter % HIGHLIGHT_INTERVAL == 0) {
            FishHighlightRenderer.tickHighlight();
        }

        // Скан инвентаря
        if (tickCounter % CHECK_INTERVAL == 0) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                checkAndAdd(player.getInventory().getStack(i), config);
            }
        }

        // Агрегация уведомлений
        if (config.aggregateNotifications) {
            if (!pendingNew.isEmpty()) {
                if (ticksSinceLastNew < 0) ticksSinceLastNew = 0;
                else if (++ticksSinceLastNew >= AGGREGATE_DELAY) {
                    showAggregated(player, config);
                    pendingNew.clear();
                    ticksSinceLastNew = -1;
                }
            } else {
                ticksSinceLastNew = -1;
            }
        }

        // Автосохранение
        if (tickCounter % SAVE_INTERVAL == 0) {
            PlayerCollectionState.saveIfDirty();
        }
    }

    // ── Проверка предмета ─────────────────────────────────────────────────

    private static void checkAndAdd(ItemStack stack, YACLConfig config) {
        if (!stack.isOf(Items.TROPICAL_FISH_BUCKET)) return;
        int variant = extractVariant(stack);
        if (variant < 0) return;

        boolean isNew = PlayerCollectionState.addVariant(variant);
        if (!isNew) return;

        // Проверяем достижения
        MilestoneManager.checkMilestones();

        if (config.aggregateNotifications) {
            pendingNew.add(variant);
            ticksSinceLastNew = 0;
        } else {
            MinecraftClient.getInstance().getToastManager()
                    .add(new NewFishToast(variant, config.toastDurationMs));
            playSound(config);
        }

        ChatLeaderboardTransport.onCollectionChanged();
    }

    public static int extractVariant(ItemStack stack) {
        DyeColor base   = stack.get(DataComponentTypes.TROPICAL_FISH_BASE_COLOR);
        DyeColor patCol = stack.get(DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR);
        TropicalFishEntity.Pattern pat = stack.get(DataComponentTypes.TROPICAL_FISH_PATTERN);
        if (base != null && patCol != null && pat != null) {
            return TropicalFishData.encode(pat.ordinal(), base.ordinal(), patCol.ordinal());
        }
        var bd = stack.get(DataComponentTypes.BUCKET_ENTITY_DATA);
        if (bd != null) {
            var nbt = bd.copyNbt();
            if (nbt.contains("BucketVariantTag")) return nbt.getInt("BucketVariantTag").orElse(-1);
            if (nbt.contains("variant"))           return nbt.getInt("variant").orElse(-1);
        }
        return -1;
    }

    private static void showAggregated(ClientPlayerEntity player, YACLConfig config) {
        if (pendingNew.isEmpty()) return;
        var mgr = MinecraftClient.getInstance().getToastManager();
        if (pendingNew.size() == 1) mgr.add(new NewFishToast(pendingNew.get(0), config.toastDurationMs));
        else                         mgr.add(new NewFishToast(pendingNew.size(), config.toastDurationMs, true));
        playSound(config);
    }

    private static void playSound(YACLConfig config) {
        if (!config.useBuiltinSound) return;
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null) p.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
    }

}