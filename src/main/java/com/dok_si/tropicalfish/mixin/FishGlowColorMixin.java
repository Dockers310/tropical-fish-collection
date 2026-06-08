package com.dok_si.tropicalfish.mixin;

import com.dok_si.tropicalfish.PlayerCollectionState;
import com.dok_si.tropicalfish.config.YACLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TropicalFishEntity;
import org.spongepowered.asm.mixin.Mixin;
import com.dok_si.tropicalfish.util.FishVariantHelper;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class FishGlowColorMixin {

    @Inject(method = "getTeamColorValue", at = @At("RETURN"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity)(Object) this;
        if (!(entity instanceof TropicalFishEntity fish)) return;
        YACLConfig cfg = YACLConfig.HANDLER.instance();
        if (!cfg.highlightUncollectedFish) return;
        if (MinecraftClient.getInstance() == null || MinecraftClient.getInstance().world == null) return;

        int variant = FishVariantHelper.extractVariant(fish);
        if (variant < 0) return;

        if (!PlayerCollectionState.getCollected().contains(variant)) {
            cir.setReturnValue(cfg.highlightRGB);
        }
    }
}
