package com.inventorywatcher;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InventoryWatcherMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("InventoryWatcher");
    public static final ModConfig CONFIG = new ModConfig();

    @Override
    public void onInitialize() {
        LOGGER.info("InventoryWatcher initialized.");
    }
}
