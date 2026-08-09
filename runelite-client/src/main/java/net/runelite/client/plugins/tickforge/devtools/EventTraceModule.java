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
public class EventTraceModule implements TickforgeModule {
    private final Client client;
    private final EventBus eventBus;

    @Inject
    public EventTraceModule(
            Client client,
            EventBus eventBus) {
        this.client = client;
        this.eventBus = eventBus;
    }

    @Override
    public String getId() {
        return "event-trace";
    }

    @Override
    public String getName() {
        return "Event Trace";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public void startUp() {
        eventBus.register(this);

        log.info("Event trace module started");
    }

    @Override
    public void shutDown() {
        eventBus.unregister(this);

        log.info("Event trace module stopped");
    }
}