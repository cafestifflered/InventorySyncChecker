package com.inventorywatcher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class InventoryWatcherClient implements ClientModInitializer {
    private static int tickDelay = 0;
    private static Runnable pendingCallback = null;
    private static boolean lastPlayerLoadedState = false;

    @Override
    public void onInitializeClient() {
        InventoryWatcherMod.LOGGER.info("InventoryWatcher client initializer loaded.");
        registerCommands();
        registerEventListeners();
    }

    private void registerEventListeners() {
        // Register tick listener for scheduled callbacks and player load detection
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle tick delay and pending callbacks
            if (tickDelay > 0) {
                tickDelay--;
            }
            if (tickDelay == 0 && pendingCallback != null) {
                pendingCallback.run();
                pendingCallback = null;
            }

            // Detect when player loads into a world
            boolean isPlayerLoaded = client.world != null && client.player != null;
            if (isPlayerLoaded && !lastPlayerLoadedState) {
                // Player just loaded into the world
                InventoryWatcherMod.LOGGER.info("Player loaded into world - firing PLAYER_FULLY_LOADED event");
                WatcherEvents.PLAYER_FULLY_LOADED.invoker().onPlayerFullyLoaded();
            }
            lastPlayerLoadedState = isPlayerLoaded;
        });

        // Register listener for PLAYER_FULLY_LOADED event
        WatcherEvents.PLAYER_FULLY_LOADED.register(() -> {
            if (InventoryWatcherMod.CONFIG.isRunning()) {
                InventoryWatcherMod.LOGGER.info("Watcher is running - scheduling server load confirmation");
                scheduleAfterTicks(3, WatcherStateMachine::onServerLoadConfirmed);
            }
        });
    }

    public static void scheduleAfterTicks(int ticks, Runnable callback) {
        tickDelay = ticks;
        pendingCallback = callback;
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("inventorywatch")
                    .then(ClientCommandManager.literal("start")
                        .executes(this::handleStart))
                    .then(ClientCommandManager.literal("stop")
                        .executes(this::handleStop))
            );
        });
    }

    private int handleStart(CommandContext<FabricClientCommandSource> context) {
        InventoryWatcherMod.CONFIG.setRunning(true);
        context.getSource().sendFeedback(Text.literal("Watcher started."));
        InventoryWatcherMod.LOGGER.info("Watcher started");
        return 1;
    }

    private int handleStop(CommandContext<FabricClientCommandSource> context) {
        InventoryWatcherMod.CONFIG.setRunning(false);
        context.getSource().sendFeedback(Text.literal("Watcher stopped."));
        InventoryWatcherMod.LOGGER.info("Watcher stopped");
        return 1;
    }
}
