package com.dok_si.tropicalfish.mixin;

import com.dok_si.tropicalfish.network.ChatLeaderboardTransport;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Перехватывает входящие сообщения чата ДО их отображения.
 *
 * Если сообщение является TFC-пакетом (содержит невидимые маркеры),
 * оно обрабатывается как данные лидерборда и СКРЫВАЕТСЯ из чата.
 * Обычные сообщения проходят без изменений.
 */
@Mixin(ChatHud.class)
public class MessageMixin {

    /**
     * Внедряемся в addMessage — метод который добавляет сообщение в чат.
     * Если это TFC-пакет — отменяем добавление (cancel).
     */
    @Inject(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onAddMessage(Text message,
                               net.minecraft.network.message.MessageSignatureData signature,
                               net.minecraft.client.gui.hud.MessageIndicator indicator,
                               CallbackInfo ci) {
        String raw = message.getString();
        if (ChatLeaderboardTransport.tryHandleChatMessage(raw)) {
            ci.cancel(); // скрываем пакет из чата
        }
    }
}
