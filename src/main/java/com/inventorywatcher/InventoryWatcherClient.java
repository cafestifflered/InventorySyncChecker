package com.inventorywatcher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class InventoryWatcherClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        InventoryWatcherMod.LOGGER.info("InventoryWatcher client initializer loaded.");
        registerCommands();
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
