package net.runelite.client.plugins.tickforge.devtools;

import dev.tickforge.api.module.TickforgeModule;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ClientTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
public class MenuEntrySnapshotModule implements TickforgeModule
{
	private final Client client;
	private final EventBus eventBus;

	private List<MenuEntrySnapshot> previousSnapshot;

    private volatile boolean blankWalkableTileFilterEnabled = true;

	@Inject
	public MenuEntrySnapshotModule(
		Client client,
		EventBus eventBus)
	{
		this.client = client;
		this.eventBus = eventBus;
	}

	@Override
	public String getId()
	{
		return "menu-entry-snapshot";
	}

	@Override
	public String getName()
	{
		return "Menu Entry Snapshot";
	}

	@Override
	public String getDescription()
	{
		return "Logs the current menu entries whenever the menu snapshot changes.";
	}

	@Override
	public void startUp()
	{
		previousSnapshot = null;
		eventBus.register(this);

		log.info("Menu entry snapshot module started");
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
		previousSnapshot = null;

		log.info("Menu entry snapshot module stopped");
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		List<MenuEntrySnapshot> snapshot = captureSnapshot();

		if (snapshot.equals(previousSnapshot))
		{
			return;
		}

		previousSnapshot = snapshot;

		log.debug("Menu entry snapshot changed: {} entries", snapshot.size());

		for (int index = 0; index < snapshot.size(); index++)
		{
			MenuEntrySnapshot entry = snapshot.get(index);

			log.debug(
				"[{}] option='{}', target='{}', type={}, identifier={}, "
					+ "param0={}, param1={}, itemId={}, worldViewId={}",
				index,
				entry.getOption(),
				entry.getTarget(),
				entry.getType(),
				entry.getIdentifier(),
				entry.getParam0(),
				entry.getParam1(),
				entry.getItemId(),
				entry.getWorldViewId());
		}
	}

    public boolean isBlankWalkableTileFilterEnabled()
    {
        return blankWalkableTileFilterEnabled;
    }

    public void setBlankWalkableTileFilterEnabled(boolean enabled)
    {
        blankWalkableTileFilterEnabled = enabled;

        log.info(
            "Blank walkable tile filter {}",
            enabled ? "enabled" : "disabled");
    }

    private List<MenuEntrySnapshot> captureSnapshot()
    {
        MenuEntry[] menuEntries = client.getMenu().getMenuEntries();
        List<MenuEntrySnapshot> snapshot =
            new ArrayList<>(menuEntries.length);

        for (MenuEntry menuEntry : menuEntries)
        {
            if (shouldIgnore(menuEntry))
            {
                continue;
            }

            snapshot.add(MenuEntrySnapshot.from(menuEntry));
        }

        return snapshot;
    }

    private boolean shouldIgnore(MenuEntry entry)
    {
        return blankWalkableTileFilterEnabled
            && isBlankWalkableTile(entry);
    }

    private boolean isBlankWalkableTile(MenuEntry entry)
    {
        String target = entry.getTarget();

        return entry.getType() == MenuAction.WALK
            && (target == null || target.isEmpty());
    }

	@Value
	private static class MenuEntrySnapshot
	{
		String option;
		String target;
		MenuAction type;
		int identifier;
		int param0;
		int param1;
		int itemId;
		int worldViewId;

		private static MenuEntrySnapshot from(MenuEntry entry)
		{
			return new MenuEntrySnapshot(
				entry.getOption(),
				entry.getTarget(),
				entry.getType(),
				entry.getIdentifier(),
				entry.getParam0(),
				entry.getParam1(),
				entry.getItemId(),
				entry.getWorldViewId());
		}
	}
}