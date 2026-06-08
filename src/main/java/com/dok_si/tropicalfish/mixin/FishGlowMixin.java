package com.dok_si.tropicalfish.mixin;

import com.dok_si.tropicalfish.PlayerCollectionState;
import com.dok_si.tropicalfish.TropicalFishData;
import com.dok_si.tropicalfish.config.YACLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TropicalFishEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class FishGlowMixin {

    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        // Если уже светится (например, от зелья) — не мешаем
        if (Boolean.TRUE.equals(cir.getReturnValue())) return;

        Entity entity = (Entity)(Object) this;
        if (!(entity instanceof TropicalFishEntity fish)) return;

        YACLConfig cfg = YACLConfig.HANDLER.instance();
        if (!cfg.highlightUncollectedFish) return;

        if (MinecraftClient.getInstance() == null || MinecraftClient.getInstance().world == null) return;

        int variant = extractVariant(fish);
        if (variant < 0) return;

        // Подсвечиваем, только если этой рыбы НЕТ в коллекции
        if (!PlayerCollectionState.getCollected().contains(variant)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Извлекает внутренний код варианта из живой тропической рыбы.
     * Логика полностью идентична BucketDecorator.decodeVanillaRaw(),
     * чтобы гарантировать совпадение с вариантами в коллекции.
     */
    private static int extractVariant(TropicalFishEntity fish) {
        try {
            int raw = fish.getDataTracker().get(TropicalFishEntityAccessor.getVariantData());
            // Раскладываем raw согласно вики:
            //   bits 0..7   = размер (нам не важен)
            //   bits 8..15  = индекс узора (pattern)
            //   bits 16..23 = индекс основного цвета (base)
            //   bits 24..31 = индекс цвета узора (patternColor)
            int pattern      = (raw >> 8)  & 0xFF;
            int base         = (raw >> 16) & 0xFF;
            int patternColor = (raw >> 24) & 0xFF;

            // Проверяем допустимость индексов
            if (pattern >= 12 || base >= 16 || patternColor >= 16) return -1;

            // Кодируем точно так же, как в TropicalFishData.encode()
            return TropicalFishData.encode(pattern, base, patternColor);
        } catch (Exception e) {
            return -1;
        }
    }
}