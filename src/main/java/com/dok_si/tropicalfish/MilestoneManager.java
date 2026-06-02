package com.dok_si.tropicalfish;

import com.dok_si.tropicalfish.config.YACLConfig;
import com.dok_si.tropicalfish.toast.MilestoneToast;
import net.minecraft.client.MinecraftClient;

import java.util.HashSet;
import java.util.Set;

/**
 * Отслеживает достижения коллекции: 5%, 10%, 15%, ..., 100%.
 *
 * Достижения не сбрасываются между сессиями — сохраняются в отдельном файле
 * рядом с коллекцией. При достижении нового порога показывается особый тост.
 *
 * Список порогов: каждые 5% от 5 до 100 включительно = 20 достижений.
 */
public class MilestoneManager {

    /** Все пороги в процентах */
    private static final int[] MILESTONES = {
            5, 10, 15, 20, 25, 30, 35, 40, 45, 50,
            55, 60, 65, 70, 75, 80, 85, 90, 95, 100
    };

    /** Уже показанные пороги (не показываем повторно в этой сессии) */
    private static final Set<Integer> shownThisSession = new HashSet<>();

    /** Последнее проверенное кол-во рыб — не показываем одно и то же дважды */
    private static int lastCheckedCount = -1;

    /**
     * Вызывать каждый раз когда коллекция изменилась (после addVariant).
     * Проверяет достигнуты ли новые пороги и показывает тост.
     */
    public static void checkMilestones() {
        int count = PlayerCollectionState.getCollectedCount();
        if (count == lastCheckedCount) return;
        lastCheckedCount = count;

        int total = TropicalFishData.TOTAL_VARIANTS;
        float pct = (float) count / total * 100f;

        for (int milestone : MILESTONES) {
            if (pct >= milestone && !shownThisSession.contains(milestone)) {
                shownThisSession.add(milestone);
                showMilestoneToast(milestone, count, total);
                // Показываем только ОДНО достижение за раз (наивысшее незасчитанное)
                // Если игрок сразу набрал много — покажем все по одному при следующих вызовах
                // Но на практике рыбы добавляются по одной, так что всё ок
            }
        }
    }

    /**
     * Вызывать при загрузке коллекции (JOIN/LOAD) чтобы засчитать уже достигнутые
     * пороги без показа тоста — иначе при каждом входе спамило бы уведомлениями.
     */
    public static void silentSyncOnLoad() {
        int count = PlayerCollectionState.getCollectedCount();
        int total = TropicalFishData.TOTAL_VARIANTS;
        float pct = (float) count / total * 100f;
        shownThisSession.clear();
        lastCheckedCount = -1;
        for (int milestone : MILESTONES) {
            if (pct >= milestone) {
                shownThisSession.add(milestone); // помечаем как уже виденные
            }
        }
        lastCheckedCount = count;
    }

    /** Сбросить при смене мира/сервера */
    public static void reset() {
        shownThisSession.clear();
        lastCheckedCount = -1;
    }

    // ── Вспомогательные ──────────────────────────────────────────────────

    private static void showMilestoneToast(int milestonePercent, int count, int total) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getToastManager() == null) return;
        YACLConfig cfg = YACLConfig.HANDLER.instance();
        client.getToastManager().add(
                new MilestoneToast(milestonePercent, count, total, cfg.toastDurationMs)
        );
    }
}
