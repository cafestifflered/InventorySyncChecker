package com.inventorywatcher;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class WatcherEvents {
    public static final Event<PlayerFullyLoaded> PLAYER_FULLY_LOADED = EventFactory.createArrayBacked(
        PlayerFullyLoaded.class,
        callbacks -> () -> {
            for (PlayerFullyLoaded callback : callbacks) {
                callback.onPlayerFullyLoaded();
            }
        }
    );

    @FunctionalInterface
    public interface PlayerFullyLoaded {
        void onPlayerFullyLoaded();
    }
}
