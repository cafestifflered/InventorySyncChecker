package com.inventorywatcher;

public class WatcherStateMachine {
    public static void onServerLoadConfirmed() {
        InventoryWatcherMod.LOGGER.info("Server load confirmed - ready for next step");
    }
}
