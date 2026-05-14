package com.inventorywatcher;

import net.fabricmc.api.ClientModInitializer;

public class InventoryWatcherClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        InventoryWatcherMod.LOGGER.info("InventoryWatcher client initializer loaded.");
    }
}
