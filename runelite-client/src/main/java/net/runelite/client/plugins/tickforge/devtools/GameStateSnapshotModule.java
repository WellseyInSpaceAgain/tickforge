package net.runelite.client.plugins.tickforge.devtools;

import javax.inject.Inject;
import javax.inject.Singleton;
import dev.tickforge.api.module.TickforgeModule;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
public class GameStateSnapshotModule implements TickforgeModule {
    private final Client client;
    private final EventBus eventBus;

    @Inject
    public GameStateSnapshotModule(
            Client client,
            EventBus eventBus) {
        this.client = client;
        this.eventBus = eventBus;
    }

    @Override
    public String getId() {
        return "game-state-snapshot";
    }

    @Override
    public String getName() {
        return "Game State Snapshot";
    }

    @Override
    public void startUp() {
        eventBus.register(this);

        log.info("Game state snapshot module started");
    }

    @Override
    public void shutDown() {
        eventBus.unregister(this);

        log.info("Game state snapshot module stopped");
    }
}