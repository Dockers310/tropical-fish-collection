package com.dok_si.tropicalfish.network;

import com.dok_si.tropicalfish.TropicalFishCollection;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Сетевые пакеты мода для P2P лидерборда.
 *
 * КАК ЭТО РАБОТАЕТ НА ЛЮБОМ СЕРВЕРЕ:
 * ─────────────────────────────────────────────────────────────
 * Minecraft имеет специальный системный канал "minecraft:register".
 * Когда клиент подключается, он отправляет список каналов которые
 * он поддерживает. Vanilla/Paper/Spigot серверы ГАРАНТИРОВАННО
 * пересылают plugin-channel пакеты клиентам которые тоже
 * зарегистрировали этот канал.
 *
 * Схема:
 *   Клиент A входит → шлёт minecraft:register ["tropicalfishcollection:announce"]
 *   Клиент B входит → шлёт minecraft:register ["tropicalfishcollection:announce"]
 *   Сервер видит: оба клиента поддерживают канал
 *   Клиент A шлёт C2S announce → сервер бродкастит S2C announce → Клиент B получает
 *
 * НО: Fabric API PayloadTypeRegistry регистрирует каналы автоматически
 * при регистрации payload'ов в обоих направлениях (C2S + S2C).
 * Это и есть правильный способ.
 *
 * Пакеты:
 *   AnnouncePayload — "я [имя], у меня [N] рыб"
 *   RequestPayload  — "только зашёл, пришлите счета"
 */
public final class Packets {

    public static final Identifier ANNOUNCE_ID = Identifier.of(TropicalFishCollection.MOD_ID, "announce");
    public static final Identifier REQUEST_ID  = Identifier.of(TropicalFishCollection.MOD_ID, "request");

    // ── Announce ────────────────────────────────────────────────────────
    public record AnnouncePayload(String playerName, int count)
            implements CustomPayload {

        public static final Id<AnnouncePayload> ID =
                new Id<>(ANNOUNCE_ID);
        public static final PacketCodec<PacketByteBuf, AnnouncePayload> CODEC =
                PacketCodec.of(AnnouncePayload::write, AnnouncePayload::read);

        @Override public Id<? extends CustomPayload> getId() { return ID; }

        private void write(PacketByteBuf buf) {
            buf.writeString(playerName, 64);
            buf.writeVarInt(Math.max(0, count));
        }
        private static AnnouncePayload read(PacketByteBuf buf) {
            return new AnnouncePayload(buf.readString(64), buf.readVarInt());
        }
    }

    // ── Request ─────────────────────────────────────────────────────────
    public record RequestPayload(String senderName)
            implements CustomPayload {

        public static final Id<RequestPayload> ID =
                new Id<>(REQUEST_ID);
        public static final PacketCodec<PacketByteBuf, RequestPayload> CODEC =
                PacketCodec.of(RequestPayload::write, RequestPayload::read);

        @Override public Id<? extends CustomPayload> getId() { return ID; }

        private void write(PacketByteBuf buf) { buf.writeString(senderName, 64); }
        private static RequestPayload read(PacketByteBuf buf) {
            return new RequestPayload(buf.readString(64));
        }
    }
}
