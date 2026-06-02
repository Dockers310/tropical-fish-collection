package com.dok_si.tropicalfish.mixin;

import com.dok_si.tropicalfish.client.BucketDecorator;
import com.dok_si.tropicalfish.config.YACLConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    /**
     * Перед каждым рендером контейнера:
     * делаем снимок коллекции и сбрасываем список виденных вёдер.
     *
     * Вызывается каждый кадр — это нормально, снимок дешёвый (HashSet copy).
     * Главное что коллекция не меняется МЕЖДУ слотами одного кадра.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BucketDecorator.resetForNewContainer();
    }

    /**
     * После отрисовки каждого слота — рисуем индикатор поверх.
     */
    @Inject(
        method = "drawItem(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
        at = @At("TAIL")
    )
    private void onDrawItem(DrawContext context, ItemStack stack, int x, int y,
                            String amountText, CallbackInfo ci) {
        int color = BucketDecorator.getIndicatorColor(stack);
        if (color != 0) {
            int size = YACLConfig.HANDLER.instance().indicatorSize;
            // Рисуем квадрат в правом нижнем углу слота
            context.fill(x + 16 - size, y + 16 - size, x + 16, y + 16, color);
        }
    }
}
