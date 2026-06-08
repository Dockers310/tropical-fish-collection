package com.dok_si.tropicalfish.mixin;

import com.dok_si.tropicalfish.client.BucketDecorator;
import com.dok_si.tropicalfish.config.YACLConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BucketDecorator.resetForNewContainer();
    }

    @Inject(
            method = "drawItem(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL")
    )
    private void onDrawItem(DrawContext context, ItemStack stack, int x, int y,
                            String amountText, CallbackInfo ci) {
        YACLConfig cfg = YACLConfig.HANDLER.instance();

        // угловой индикатор (Shift)
        int indicatorColor = BucketDecorator.getIndicatorColor(stack);
        if (indicatorColor != 0) {
            int size = cfg.indicatorSize;
            context.fill(x + 16 - size, y + 16 - size, x + 16, y + 16, indicatorColor);
        }

        // подсветка дубликатов
        if (!stack.isOf(Items.TROPICAL_FISH_BUCKET)) return;
        int thisVariant = BucketDecorator.extractVariant(stack);
        if (thisVariant < 0) return;

        int hoveredVariant = BucketDecorator.getHoveredVariant();
        if (hoveredVariant < 0 || thisVariant != hoveredVariant) return;

        Slot hoveredSlot = BucketDecorator.getHoveredSlot();
        if (hoveredSlot != null && hoveredSlot.x == x && hoveredSlot.y == y) return;

        int highlightColor = BucketDecorator.getHoverHighlightColor(thisVariant);
        if (highlightColor != 0) {
            context.fill(x, y, x + 16, y + 16, highlightColor);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void afterRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        Slot slot = ((HandledScreenAccessor) screen).invokeGetSlotAt(mouseX, mouseY);
        if (slot == null || !slot.hasStack()) {
            BucketDecorator.setHoveredVariant(-1);
            BucketDecorator.setHoveredSlot(null);
            return;
        }

        ItemStack stack = slot.getStack();
        if (!stack.isOf(Items.TROPICAL_FISH_BUCKET)) {
            BucketDecorator.setHoveredVariant(-1);
            BucketDecorator.setHoveredSlot(null);
            return;
        }

        int variant = BucketDecorator.extractVariant(stack);
        if (variant < 0) {
            BucketDecorator.setHoveredVariant(-1);
            BucketDecorator.setHoveredSlot(null);
            return;
        }

        BucketDecorator.setHoveredVariant(variant);
        BucketDecorator.setHoveredSlot(slot);

        int color = BucketDecorator.getHoveredSlotColor(variant);
        context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
    }
}