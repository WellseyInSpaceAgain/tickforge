package net.runelite.client.plugins.tickforge;

import javax.inject.Inject;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.tickforge.framework.TickforgeModuleManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        log.info("Tickforge plugin started");
		moduleManager.getRegistry().start("proof");
	}

	@Override
	protected void shutDown()
	{
        log.debug("Tickforge plugin stopping");
		moduleManager.shutDown();
	}
}