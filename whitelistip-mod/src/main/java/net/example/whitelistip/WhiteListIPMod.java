// WhiteListIPMod.java
package net.example.whitelistip;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WhiteListIPMod implements DedicatedServerModInitializer {
    private static final Path WHITELIST_FILE = Paths.get("whitelistip.txt");
    private static final Set<String> ALLOWED_IPS = new HashSet<>();
    private static MinecraftServer serverInstance;

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::loadWhitelist);
        ServerPlayConnectionEvents.INIT.register((handler, server) -> validateIP(handler.getConnection().getAddress().toString()));
    }

    private void loadWhitelist(MinecraftServer server) {
        serverInstance = server;
        ALLOWED_IPS.clear();

        try {
            if (!Files.exists(WHITELIST_FILE)) {
                Files.write(WHITELIST_FILE, Collections.singletonList("# Добавьте IPv4-адреса (по одному на строку)\n127.0.0.1"));
                serverInstance.sendMessage(Text.literal("Создан новый файл whitelistip.txt"));
            }

            List<String> lines = Files.readAllLines(WHITELIST_FILE);
            for (String line : lines) {
                String trimmed = line.split("#")[0].trim();
                if (!trimmed.isEmpty() && trimmed.matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
                    ALLOWED_IPS.add(trimmed);
                }
            }
            serverInstance.sendMessage(Text.literal("Загружено разрешенных IP: " + ALLOWED_IPS.size()));
        } catch (IOException e) {
            serverInstance.sendMessage(Text.literal("Ошибка загрузки whitelistip.txt: " + e.getMessage()));
        }
    }

    private static void validateIP(String fullAddress) {
        if (serverInstance == null) return;

        String ip = fullAddress.replace("/", "").split(":")[0];
        if (!ALLOWED_IPS.contains(ip)) {
            serverInstance.getPlayerManager().broadcast(
                Text.literal("Блокировка подключения с IP: " + ip), false
            );
            throw new SecurityException("Ваш IP не в белом списке");
        }
    }
}