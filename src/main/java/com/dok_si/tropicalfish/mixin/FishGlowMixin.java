package com.dok_si.tropicalfish.mixin;

import com.dok_si.tropicalfish.PlayerCollectionState;
import com.dok_si.tropicalfish.TropicalFishData;
import com.dok_si.tropicalfish.config.YACLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.TropicalFishEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(Entity.class)
public class FishGlowMixin {

    private static TrackedData<Integer> VARIANT_FIELD = null;

    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) return;

        Entity entity = (Entity)(Object) this;
        if (!(entity instanceof TropicalFishEntity fish)) return;

        YACLConfig cfg = YACLConfig.HANDLER.instance();
        if (!cfg.highlightUncollectedFish) return;

        if (MinecraftClient.getInstance() == null) return;
        if (MinecraftClient.getInstance().world == null) return;

        int variant = extractVariant(fish);
        if (variant < 0) return;

        if (!PlayerCollectionState.getCollected().contains(variant)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Достаёт сырой вариант из DataTracker через рефлексию.
     * Формат: (patternIndex << 16) | (baseColorIndex << 8) | patternColorIndex
     */
    private static int extractVariant(TropicalFishEntity fish) {
        try {
            // Ленивая инициализация поля VARIANT
            if (VARIANT_FIELD == null) {
                Field field = TropicalFishEntity.class.getDeclaredField("VARIANT");
                field.setAccessible(true);
                VARIANT_FIELD = (TrackedData<Integer>) field.get(null);
            }
            Integer raw = fish.getDataTracker().get(VARIANT_FIELD);
            if (raw == null) return -1;
            int rawValue = raw;
            int patternIndex = (rawValue >> 16) & 0xFF;
            int baseColorIndex = (rawValue >> 8) & 0xFF;
            int patternColorIndex = rawValue & 0xFF;
            if (baseColorIndex >= 16 || patternColorIndex >= 16 || patternIndex >= 12) {
                return -1;
            }
            return TropicalFishData.encode(patternIndex, baseColorIndex, patternColorIndex);
        } catch (Throwable e) {
            return -1;
        }
    }
}