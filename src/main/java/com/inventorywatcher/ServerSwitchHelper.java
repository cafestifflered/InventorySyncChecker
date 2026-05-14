package com.inventorywatcher;

import net.minecraft.client.MinecraftClient;

public class ServerSwitchHelper {
    public static void switchTo(String serverName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.networkHandler.sendCommand("server " + serverName);
            InventoryWatcherMod.LOGGER.info("Switching to server: " + serverName);
        } else {
            InventoryWatcherMod.LOGGER.warn("Cannot switch server: player is null");
        }
    }
}
