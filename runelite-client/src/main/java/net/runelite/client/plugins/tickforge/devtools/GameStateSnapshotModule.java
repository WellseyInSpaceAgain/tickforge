package net.runelite.client.plugins.tickforge.devtools;

import javax.inject.Inject;
import javax.inject.Singleton;
import dev.tickforge.api.module.TickforgeModule;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.tickforge.devtools.GameStateSnapshotModule.GameStateSnapshot.GameStateSnapshotBuilder;

@Slf4j
@Singleton
public class GameStateSnapshotModule implements TickforgeModule {
    private final Client client;
    private final EventBus eventBus;
    private GameStateSnapshot previousSnapshot;

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
    public String getDescription() {
        return "Logs compact player and game state whenever it changes.";
    }

    @Override
    public void startUp() {
        previousSnapshot = null;
        eventBus.register(this);

        log.info("Game state snapshot module started");
    }

    @Override
    public void shutDown() {
        eventBus.unregister(this);
        previousSnapshot = null;

        log.info("Game state snapshot module stopped");
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        GameStateSnapshot snapshot = captureSnapshot();

        if (snapshot.equals(previousSnapshot)) {
            return;
        }

        previousSnapshot = snapshot;

        log.info("{}", snapshot);
    }

    private GameStateSnapshot captureSnapshot() {
        GameStateSnapshotBuilder builder = GameStateSnapshot.builder()
                .tick(client.getTickCount())
                .gameCycle(client.getGameCycle())
                .gameState(client.getGameState())
                .world(client.getWorld())
                .widgetSelected(client.isWidgetSelected());

        Player player = client.getLocalPlayer();

        if (player != null) {
            enrichPlayerContext(builder, player);
            enrichInteractionContext(builder, player.getInteracting());
        }

        enrichSelectionContext(builder);

        return builder.build();
    }

    private void enrichPlayerContext(
            GameStateSnapshotBuilder builder,
            Player player) {
        WorldPoint worldPoint = player.getWorldLocation();

        builder
                .worldX(worldPoint.getX())
                .worldY(worldPoint.getY())
                .plane(worldPoint.getPlane())
                .animation(player.getAnimation());
    }

    private void enrichInteractionContext(
            GameStateSnapshotBuilder builder,
            Actor target) {
        if (target == null) {
            return;
        }

        WorldPoint worldPoint = target.getWorldLocation();

        builder
                .interactionTargetName(target.getName())
                .interactionTargetWorldX(worldPoint.getX())
                .interactionTargetWorldY(worldPoint.getY())
                .interactionTargetPlane(worldPoint.getPlane());

        if (target instanceof NPC) {
            NPC npc = (NPC) target;

            builder
                    .interactionTargetType(InteractionTargetType.NPC)
                    .interactionTargetId(npc.getId())
                    .interactionTargetIndex(npc.getIndex());

            return;
        }

        if (target instanceof Player) {
            builder.interactionTargetType(InteractionTargetType.PLAYER);
            return;
        }

        builder.interactionTargetType(InteractionTargetType.UNKNOWN);
    }

    private void enrichSelectionContext(GameStateSnapshotBuilder builder) {
        if (!client.isWidgetSelected()) {
            return;
        }

        Widget widget = client.getSelectedWidget();

        if (widget == null) {
            return;
        }

        builder
                .selectedWidgetId(widget.getId())
                .selectedWidgetIndex(widget.getIndex())
                .selectedWidgetName(widget.getName())
                .selectedWidgetTargetVerb(widget.getTargetVerb())
                .selectedWidgetItemId(widget.getItemId());
    }

    @Value
    @Builder
    public static class GameStateSnapshot {
        // Observation metadata - deliberately excluded from state equality
        @EqualsAndHashCode.Exclude
        int tick;

        @EqualsAndHashCode.Exclude
        int gameCycle;

        // Client
        GameState gameState;
        int world;

        // Local player
        Integer worldX;
        Integer worldY;
        Integer plane;
        Integer animation;

        // Current actor interaction
        InteractionTargetType interactionTargetType;
        Integer interactionTargetId;
        Integer interactionTargetIndex;
        String interactionTargetName;
        Integer interactionTargetWorldX;
        Integer interactionTargetWorldY;
        Integer interactionTargetPlane;

        // Item/spell/widget target mode
        boolean widgetSelected;
        Integer selectedWidgetId;
        Integer selectedWidgetIndex;
        Integer selectedWidgetItemId;
        String selectedWidgetName;
        String selectedWidgetTargetVerb;
    }

    public enum InteractionTargetType {
        NPC,
        PLAYER,
        UNKNOWN
    }
}