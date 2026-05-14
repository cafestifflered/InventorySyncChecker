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
        InventoryWatcherMod.LOGGER.info("Watcher state machine started. Capturing baseline inventory...");
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

    private static void attemptSwitch(String serverName, State switchingState, int retriesRemaining) {
        if (!ModConfig.running) {
            return;
        }

        awaitingServerLoad = true;
        currentState = switchingState;
        ServerSwitchHelper.switchTo(serverName);
        InventoryWatcherMod.LOGGER.info("Attempting switch to " + serverName + " (retries remaining: " + retriesRemaining + ")");

        // scheduleAfterTicks is single-slot; this assumes attemptSwitch is only called when no other callback is pending.
        InventoryWatcherClient.scheduleAfterTicks(200, () -> {
            if (!ModConfig.running) {
                return;
            }

            if (awaitingServerLoad) {
                if (retriesRemaining > 0) {
                    InventoryWatcherMod.LOGGER.warn("Server switch to " + serverName + " timed out. Retrying...");
                    attemptSwitch(serverName, switchingState, retriesRemaining - 1);
                } else {
                    InventoryWatcherMod.LOGGER.warn("Server switch to " + serverName + " failed after all retries. Halting.");
                    currentState = State.HALTED;
                    ModConfig.running = false;
                    awaitingServerLoad = false;
                }
            }
        });
    }

    private static void captureBaselineAndSwitch() {
        if (!ModConfig.running) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            InventoryWatcherMod.LOGGER.warn("captureBaselineAndSwitch: player is null, retrying in 10 ticks...");
            InventoryWatcherClient.scheduleAfterTicks(10, () -> {
                if (!ModConfig.running) {
                    return;
                }
                captureBaselineAndSwitch();
            });
            return;
        }

        InventoryWatcherMod.LOGGER.info("Opening inventory screen to capture baseline snapshot...");
        client.setScreen(new InventoryScreen(client.player));

        InventoryWatcherClient.scheduleAfterTicks(2, () -> {
            if (!ModConfig.running) {
                return;
            }

            MinecraftClient delayedClient = MinecraftClient.getInstance();
            if (delayedClient.player == null) {
                InventoryWatcherMod.LOGGER.warn("Baseline capture callback: player is null, retrying in 10 ticks...");
                InventoryWatcherClient.scheduleAfterTicks(10, () -> {
                    if (!ModConfig.running) {
                        return;
                    }
                    captureBaselineAndSwitch();
                });
                return;
            }

            baselineSnapshot = InventorySnapshot.capture(delayedClient.player.getInventory());
            InventoryWatcherMod.LOGGER.info("Baseline snapshot captured:");
            InventoryWatcherMod.LOGGER.info(baselineSnapshot.toLogString());
            delayedClient.setScreen(null);

            InventoryWatcherClient.scheduleAfterTicks(1, () -> {
                if (!ModConfig.running) {
                    return;
                }

                MinecraftClient switchClient = MinecraftClient.getInstance();
                if (switchClient.player == null) {
                    return;
                }

                attemptSwitch("Zekrom", State.SWITCHING_TO_ZEKROM, 1);
            });
        });
    }

    private static void verifyInventory(String serverName, State verifyingState) {
        if (!ModConfig.running) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            InventoryWatcherMod.LOGGER.warn("verifyInventory(" + serverName + "): player is null, retrying in 10 ticks...");
            InventoryWatcherClient.scheduleAfterTicks(10, () -> {
                if (!ModConfig.running) {
                    return;
                }
                verifyInventory(serverName, verifyingState);
            });
            return;
        }

        currentState = verifyingState;
        InventoryWatcherMod.LOGGER.info("Verifying inventory on " + serverName + "...");
        client.setScreen(new InventoryScreen(client.player));

        InventoryWatcherClient.scheduleAfterTicks(2, () -> {
            if (!ModConfig.running) {
                return;
            }

            MinecraftClient delayedClient = MinecraftClient.getInstance();
            if (delayedClient.player == null) {
                InventoryWatcherMod.LOGGER.warn("verifyInventory callback (" + serverName + "): player is null, retrying in 10 ticks...");
                InventoryWatcherClient.scheduleAfterTicks(10, () -> {
                    if (!ModConfig.running) {
                        return;
                    }
                    verifyInventory(serverName, verifyingState);
                });
                return;
            }

            InventorySnapshot current = InventorySnapshot.capture(delayedClient.player.getInventory());
            checkAndProceed(serverName, current, verifyingState);
        });
    }

    private static void checkAndProceed(String serverName, InventorySnapshot current, State verifyingState) {
        if (baselineSnapshot != null && baselineSnapshot.isIdenticalTo(current)) {
            InventoryWatcherMod.LOGGER.info("Inventory check PASSED on " + serverName + " - inventories are identical.");

            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(null);

            InventoryWatcherMod.LOGGER.info("Inventory verified on " + serverName + " - identical to baseline");

            if (verifyingState == State.VERIFYING_ZEKROM) {
                InventoryWatcherClient.scheduleAfterTicks(200, () -> {
                    if (!ModConfig.running) {
                        return;
                    }

                    attemptSwitch("CharizardSpawn", State.SWITCHING_TO_CHARIZARDSPAWN, 1);
                });
            } else if (verifyingState == State.VERIFYING_CHARIZARDSPAWN) {
                InventoryWatcherClient.scheduleAfterTicks(200, () -> {
                    if (!ModConfig.running) {
                        return;
                    }

                    attemptSwitch("Zekrom", State.SWITCHING_TO_ZEKROM, 1);
                });
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
