package net.runelite.client.plugins.tickforge;

import javax.inject.Inject;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.tickforge.framework.TickforgeModuleManager;

@PluginDescriptor(
	name = "Tickforge",
	description = "Hosts Tickforge development tools and activity modules",
	tags = {"tickforge", "development"},
	enabledByDefault = false
)
public class TickforgePlugin extends Plugin
{
	@Inject
	private TickforgeModuleManager moduleManager;

	@Override
	protected void startUp()
	{
		// Manager and registered modules have been constructed.
	}

	@Override
	protected void shutDown()
	{
		moduleManager.shutDown();
	}
}