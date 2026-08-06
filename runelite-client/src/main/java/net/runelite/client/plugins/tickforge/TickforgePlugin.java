package net.runelite.client.plugins.tickforge;

import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.tickforge.framework.TickforgeModuleManager;
import net.runelite.client.plugins.tickforge.ui.TickforgePanel;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Tickforge",
	description = "Hosts Tickforge development tools and activity modules",
	tags = {"tickforge", "development"},
	enabledByDefault = false,
	developerPlugin = true
)
public class TickforgePlugin extends Plugin
{
	@Inject
	private TickforgeModuleManager moduleManager;

	@Inject
	private ClientToolbar clientToolbar;

	private TickforgePanel panel;
	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		log.debug("Tickforge plugin started");

		panel = new TickforgePanel(moduleManager.getRegistry());

		final BufferedImage icon =
			ImageUtil.loadImageResource(getClass(), "tickforge_icon.png");

		navigationButton = NavigationButton.builder()
			.tooltip("Tickforge")
			.icon(icon)
			.priority(2)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navigationButton);
	}

	@Override
	protected void shutDown()
	{
		log.debug("Tickforge plugin stopping");

		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}

		moduleManager.shutDown();

		navigationButton = null;
		panel = null;
	}
}