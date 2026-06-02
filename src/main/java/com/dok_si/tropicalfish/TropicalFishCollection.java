package com.dok_si.tropicalfish;

import com.dok_si.tropicalfish.network.Packets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Серверная точка входа.
 *
 * Здесь регистрируем пакеты в ОБОИХ направлениях (C2S и S2C).
 * Это критично: Fabric API при регистрации C2S автоматически добавляет
 * канал в minecraft:register payload который клиент шлёт серверу.
 * Vanilla/Paper/Spigot сервер видит что клиенты поддерживают каналы
 * и транслирует plugin-channel пакеты между ними.
 *
 * БЕЗ регистрации S2C — клиент не знает что принимать этот канал,
 * без регистрации C2S — сервер не знает что пересылать.
 * Нужно ОБА.
 */
public class TropicalFishCollection implements ModInitializer {

    public static final String MOD_ID = "tropicalfishcollection";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Tropical Fish Collection v1.0.4 loaded!");

        // Регистрация C2S (клиент → сервер → другие клиенты)
        PayloadTypeRegistry.playC2S().register(
                Packets.AnnouncePayload.ID, Packets.AnnouncePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(
                Packets.RequestPayload.ID, Packets.RequestPayload.CODEC);

        // Регистрация S2C (клиент получает от сервера)
        PayloadTypeRegistry.playS2C().register(
                Packets.AnnouncePayload.ID, Packets.AnnouncePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                Packets.RequestPayload.ID, Packets.RequestPayload.CODEC);
    }
}
