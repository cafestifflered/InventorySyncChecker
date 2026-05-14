package com.inventorywatcher;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class WatcherStateMachine {
    public enum State {
        IDLE,
        CAPTURING_BASELINE,
        SWITCHING_TO_ZEKROM,
        VERIFYING_ZEKROM,
        SWITCHING_TO_CHARIZARDSPAWN,
        VERIFYING_CHARIZARDSPAWN,
        HALTED
    }

    private static State currentState = State.IDLE;
    private static InventorySnapshot baselineSnapshot = null;
    private static boolean awaitingServerLoad = false;

    public static void start() {
        ModConfig.running = true;
        currentState = State.CAPTURING_BASELINE;
        captureBaselineAndSwitch();
    }

    public static void stop() {
        ModConfig.running = false;
        awaitingServerLoad = false;
        currentState = State.IDLE;

        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(null);

        InventoryWatcherMod.LOGGER.info("Watcher stopped by user");
    }

    public static void onServerLoadConfirmed() {
        if (!awaitingServerLoad) {
            return;
        }

        awaitingServerLoad = false;

        if (!ModConfig.running) {
            return;
        }

        switch (currentState) {
            case SWITCHING_TO_ZEKROM -> verifyInventory("Zekrom", State.VERIFYING_ZEKROM);
            case SWITCHING_TO_CHARIZARDSPAWN -> verifyInventory("CharizardSpawn", State.VERIFYING_CHARIZARDSPAWN);
            default -> InventoryWatcherMod.LOGGER.warn("onServerLoadConfirmed called in unexpected state: " + currentState);
        }
    }

    private static void captureBaselineAndSwitch() {
        if (!ModConfig.running) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.setScreen(new InventoryScreen(client.player));

        InventoryWatcherClient.scheduleAfterTicks(2, () -> {
            if (!ModConfig.running) {
                return;
            }

            MinecraftClient delayedClient = MinecraftClient.getInstance();
            if (delayedClient.player == null) {
                return;
            }

            baselineSnapshot = InventorySnapshot.capture(delayedClient.player.getInventory());
            delayedClient.setScreen(null);

            InventoryWatcherClient.scheduleAfterTicks(1, () -> {
                if (!ModConfig.running) {
                    return;
                }

                MinecraftClient switchClient = MinecraftClient.getInstance();
                if (switchClient.player == null) {
                    return;
                }

                awaitingServerLoad = true;
                currentState = State.SWITCHING_TO_ZEKROM;
                ServerSwitchHelper.switchTo("Zekrom");
                InventoryWatcherMod.LOGGER.info("Baseline captured. Switching to Zekrom...");
            });
        });
    }

    private static void verifyInventory(String serverName, State verifyingState) {
        if (!ModConfig.running) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        currentState = verifyingState;
        client.setScreen(new InventoryScreen(client.player));

        InventoryWatcherClient.scheduleAfterTicks(2, () -> {
            if (!ModConfig.running) {
                return;
            }

            MinecraftClient delayedClient = MinecraftClient.getInstance();
            if (delayedClient.player == null) {
                return;
            }

            InventorySnapshot current = InventorySnapshot.capture(delayedClient.player.getInventory());
            checkAndProceed(serverName, current, verifyingState);
        });
    }

    private static void checkAndProceed(String serverName, InventorySnapshot current, State verifyingState) {
        if (baselineSnapshot != null && baselineSnapshot.isIdenticalTo(current)) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(null);

            InventoryWatcherMod.LOGGER.info("Inventory verified on " + serverName + " - identical to baseline");

            if (verifyingState == State.VERIFYING_ZEKROM) {
                awaitingServerLoad = true;
                currentState = State.SWITCHING_TO_CHARIZARDSPAWN;
                ServerSwitchHelper.switchTo("CharizardSpawn");
                InventoryWatcherMod.LOGGER.info("Switching to CharizardSpawn...");
            } else if (verifyingState == State.VERIFYING_CHARIZARDSPAWN) {
                awaitingServerLoad = true;
                currentState = State.SWITCHING_TO_ZEKROM;
                ServerSwitchHelper.switchTo("Zekrom");
                InventoryWatcherMod.LOGGER.info("Loop complete. Switching back to Zekrom...");
            }
            return;
        }

        currentState = State.HALTED;
        ModConfig.running = false;
        awaitingServerLoad = false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Identifier hornId = Identifier.of("minecraft", "event.raid.horn");
            SoundEvent horn = SoundEvent.of(hornId);
            client.player.playSound(horn, 1.0f, 1.0f);
        }

        InventoryWatcherMod.LOGGER.warn("!!! INVENTORY MISMATCH DETECTED on " + serverName + " !!!");
        InventoryWatcherMod.LOGGER.warn("--- Baseline Inventory ---");
        InventoryWatcherMod.LOGGER.warn(baselineSnapshot == null ? "(baseline is null)" : baselineSnapshot.toLogString());
        InventoryWatcherMod.LOGGER.warn("--- Current Inventory ---");
        InventoryWatcherMod.LOGGER.warn(current.toLogString());
        InventoryWatcherMod.LOGGER.warn("Watcher halted. Inventory screen left open for inspection.");
    }
}
