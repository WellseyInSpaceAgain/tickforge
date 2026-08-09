package net.runelite.client.plugins.tickforge.devtools;

import javax.inject.Inject;
import javax.inject.Singleton;
import dev.tickforge.api.module.TickforgeModule;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.tickforge.devtools.InteractionProbeModule.InteractionProbeSnapshot.InteractionProbeSnapshotBuilder;

@Slf4j
@Singleton
public class InteractionProbeModule implements TickforgeModule {
    private final Client client;
    private final EventBus eventBus;

    @Inject
    public InteractionProbeModule(
            Client client,
            EventBus eventBus) {
        this.client = client;
        this.eventBus = eventBus;
    }

    @Override
    public String getId() {
        return "interaction-probe";
    }

    @Override
    public String getName() {
        return "Interaction Probe";
    }

    @Override
    public void startUp() {
        eventBus.register(this);

        log.info("Interaction probe module started");
    }

    @Override
    public void shutDown() {
        eventBus.unregister(this);

        log.info("Interaction probe module stopped");
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        InteractionProbeSnapshot snapshot = createSnapshot(event);

        log.info("{}", snapshot);
    }

    private InteractionProbeSnapshot createSnapshot(MenuOptionClicked event) {
        MenuEntry entry = event.getMenuEntry();

        InteractionProbeSnapshotBuilder builder = InteractionProbeSnapshot.builder()
                .tick(client.getTickCount())
                .gameCycle(client.getGameCycle())
                .option(entry.getOption())
                .target(entry.getTarget())
                .action(entry.getType())
                .identifier(entry.getIdentifier())
                .param0(entry.getParam0())
                .param1(entry.getParam1())
                .worldViewId(entry.getWorldViewId())
                .isItemOp(entry.isItemOp())
                .targetType(InteractionTargetType.UNKNOWN);

        enrichItemContext(builder, entry);
        enrichTargetContext(builder, entry);
        enrichWidgetContext(builder, entry);

        return builder.build();
    }

    private void enrichItemContext(InteractionProbeSnapshotBuilder builder, MenuEntry entry) {
        if (!entry.isItemOp()) {
            return;
        }

        builder
                .itemOp(entry.getItemOp())
                .itemId(entry.getItemId());
    }

    private void enrichTargetContext(InteractionProbeSnapshotBuilder builder, MenuEntry entry) {
        NPC npc = entry.getNpc();
        if (npc != null) {
            WorldPoint worldPoint = npc.getWorldLocation();

            builder
                    .targetType(InteractionTargetType.NPC)
                    .targetId(npc.getId())
                    .targetIndex(npc.getIndex())
                    .targetName(npc.getName())
                    .worldX(worldPoint.getX())
                    .worldY(worldPoint.getY())
                    .plane(worldPoint.getPlane());

            return;
        }

        Player player = entry.getPlayer();
        if (player != null) {
            WorldPoint worldPoint = player.getWorldLocation();

            builder
                    .targetType(InteractionTargetType.PLAYER)
                    .targetName(player.getName())
                    .worldX(worldPoint.getX())
                    .worldY(worldPoint.getY())
                    .plane(worldPoint.getPlane());

            return;
        }

        MenuAction menuAction = entry.getType();
        if (menuAction == MenuAction.WALK) {
            Tile tile = getTileFromMenuEntry(entry);
            if (tile != null) {
                WorldPoint worldPoint = tile.getWorldLocation();

                builder
                        .targetType(InteractionTargetType.TILE)
                        .worldX(worldPoint.getX())
                        .worldY(worldPoint.getY())
                        .plane(worldPoint.getPlane());
            }

            return;
        }

        if (isGameObjectAction(menuAction)) {
            Tile tile = getTileFromMenuEntry(entry);
            if (tile != null) {
                TileObject tileObject = findTileObject(tile, entry);

                if (tileObject != null) {
                    enrichTileObjectContext(builder, tileObject);
                }
            }
            return;
        }

        if (isGroundItemAction(menuAction)) {
            Tile tile = getTileFromMenuEntry(entry);
            if (tile != null) {
                TileItem tileItem = findTileItem(tile, entry);

                if (tileItem != null) {
                    enrichGroundItemContext(builder, tile, tileItem);
                }
            }
            return;
        }
    }

    private Tile getTileFromMenuEntry(MenuEntry entry) {
        WorldView worldView = client.getWorldView(entry.getWorldViewId());

        if (worldView != null) {
            return worldView.getSelectedSceneTile();
        }

        return null;
    }

    private boolean isGameObjectAction(MenuAction action) {
        switch (action) {
            case GAME_OBJECT_FIRST_OPTION:
            case GAME_OBJECT_SECOND_OPTION:
            case GAME_OBJECT_THIRD_OPTION:
            case GAME_OBJECT_FOURTH_OPTION:
            case GAME_OBJECT_FIFTH_OPTION:
            case WIDGET_TARGET_ON_GAME_OBJECT:
            case EXAMINE_OBJECT:
                return true;

            default:
                return false;
        }
    }

    private boolean isGroundItemAction(MenuAction action) {
        switch (action) {
            case GROUND_ITEM_FIRST_OPTION:
            case GROUND_ITEM_SECOND_OPTION:
            case GROUND_ITEM_THIRD_OPTION:
            case GROUND_ITEM_FOURTH_OPTION:
            case GROUND_ITEM_FIFTH_OPTION:
            case WIDGET_TARGET_ON_GROUND_ITEM:
            case EXAMINE_ITEM_GROUND:
                return true;

            default:
                return false;
        }
    }

    private TileObject findTileObject(Tile tile, MenuEntry entry) {
        int targetId = entry.getIdentifier();
        for (GameObject gameObject : tile.getGameObjects()) {
            if (gameObject != null && gameObject.getId() == targetId) {
                return gameObject;
            }
        }

        GroundObject groundObject = tile.getGroundObject();
        if (groundObject != null && groundObject.getId() == targetId) {
            return groundObject;
        }

        return null;
    }

    private TileItem findTileItem(Tile tile, MenuEntry entry) {
        int targetId = entry.getIdentifier();
        for (TileItem tileItem : tile.getGroundItems()) {
            if (tileItem != null && tileItem.getId() == targetId) {
                return tileItem;
            }
        }

        return null;
    }

    private void enrichTileObjectContext(InteractionProbeSnapshotBuilder builder, TileObject tileObject) {
        WorldPoint worldPoint = tileObject.getWorldLocation();

        builder
                .targetType(InteractionTargetType.OBJECT)
                .targetId(tileObject.getId())
                .targetName(client.getObjectDefinition(tileObject.getId()).getName())
                .worldX(worldPoint.getX())
                .worldY(worldPoint.getY())
                .plane(worldPoint.getPlane());
    }

    private void enrichGroundItemContext(InteractionProbeSnapshotBuilder builder, Tile tile, TileItem tileItem) {
        WorldPoint worldPoint = tile.getWorldLocation();

        builder
                .targetType(InteractionTargetType.GROUND_ITEM)
                .targetId(tileItem.getId())
                .targetName(client.getItemDefinition(tileItem.getId()).getName())
                .worldX(worldPoint.getX())
                .worldY(worldPoint.getY())
                .plane(worldPoint.getPlane());
    }

    private void enrichWidgetContext(InteractionProbeSnapshotBuilder builder, MenuEntry entry) {
        Widget widget = entry.getWidget();

        if (widget == null) {
            return;
        }

        builder
                .targetType(InteractionTargetType.WIDGET)
                .widgetId(widget.getId())
                .widgetIndex(widget.getIndex());
    }

    @Value
    @Builder
    public static class InteractionProbeSnapshot {
        // When it happened
        int tick;
        int gameCycle;

        // Raw MenuEntry / menuAction data
        String option;
        String target;
        MenuAction action;
        int identifier;
        int param0;
        int param1;
        int worldViewId;

        // Item context
        boolean isItemOp;
        Integer itemOp;
        Integer itemId;

        // Resolved context
        InteractionTargetType targetType;
        Integer targetId;
        Integer targetIndex;
        String targetName;

        // World context, where applicable
        Integer worldX;
        Integer worldY;
        Integer plane;

        // Widget context, where applicable
        Integer widgetId;
        Integer widgetIndex;
    }

    public enum InteractionTargetType {
        NPC,
        PLAYER,
        OBJECT,
        GROUND_ITEM,
        TILE,
        WIDGET,
        UNKNOWN
    }
}
