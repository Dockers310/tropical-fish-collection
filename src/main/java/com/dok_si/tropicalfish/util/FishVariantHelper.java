package com.dok_si.tropicalfish.util;

import com.dok_si.tropicalfish.TropicalFishData;
import com.dok_si.tropicalfish.mixin.TropicalFishEntityAccessor;
import net.minecraft.entity.passive.TropicalFishEntity;

public final class FishVariantHelper {

    private FishVariantHelper() {}

    public static int extractVariant(TropicalFishEntity fish) {
        try {
            int raw = fish.getDataTracker().get(TropicalFishEntityAccessor.getVariantData());
            int pattern      = (raw >> 8)  & 0xFF;
            int base         = (raw >> 16) & 0xFF;
            int patternColor = (raw >> 24) & 0xFF;

            if (pattern >= 12 || base >= 16 || patternColor >= 16) return -1;
            return TropicalFishData.encode(pattern, base, patternColor);
        } catch (Exception e) {
            return -1;
        }
    }
}
