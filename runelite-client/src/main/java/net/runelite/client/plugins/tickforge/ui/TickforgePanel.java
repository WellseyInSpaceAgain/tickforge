package net.runelite.client.plugins.tickforge.ui;

import dev.tickforge.api.module.TickforgeModule;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.tickforge.framework.ModuleRegistry;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import javax.swing.JCheckBox;
import net.runelite.client.plugins.tickforge.devtools.MenuEntrySnapshotModule;

@Slf4j
public class TickforgePanel extends PluginPanel
{
	private final ModuleRegistry registry;

	public TickforgePanel(ModuleRegistry registry)
	{
		this.registry = registry;
		rebuild();
	}

	public final void rebuild()
	{
		removeAll();

		final JLabel title = new JLabel("Tickforge Modules");
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		add(title);

		for (TickforgeModule module : registry.getModules())
		{
			add(createModulePanel(module));
		}

		revalidate();
		repaint();
	}

	private JPanel createModulePanel(TickforgeModule module)
	{
		final JPanel container = new JPanel(new BorderLayout(0, 8));
		container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		container.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
			BorderFactory.createEmptyBorder(8, 8, 8, 8)
		));

		final JPanel details = new JPanel();
		details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
		details.setOpaque(false);

		final JLabel nameLabel = new JLabel(module.getName());
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
		details.add(nameLabel);

		final JLabel idLabel = new JLabel(module.getId());
		idLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		details.add(idLabel);

		final String description = module.getDescription();
		if (description != null && !description.isEmpty())
		{
			final JLabel descriptionLabel = new JLabel("<html>" + description + "</html>");
			descriptionLabel.setForeground(ColorScheme.TEXT_COLOR);
			details.add(descriptionLabel);
		}

		addModuleSettings(details, module);

		final JPanel controls = new JPanel(new BorderLayout(8, 0));
		controls.setOpaque(false);

		final JLabel statusLabel = new JLabel();
		final JButton toggleButton = new JButton();

		toggleButton.addActionListener(event ->
			toggleModule(module, toggleButton, statusLabel));

		updateControls(module, toggleButton, statusLabel);

		controls.add(statusLabel, BorderLayout.CENTER);
		controls.add(toggleButton, BorderLayout.EAST);

		container.add(details, BorderLayout.CENTER);
		container.add(controls, BorderLayout.SOUTH);

		return container;
	}

	private void toggleModule(
		TickforgeModule module,
		JButton toggleButton,
		JLabel statusLabel)
	{
		try
		{
			if (registry.isRunning(module.getId()))
			{
				registry.stop(module.getId());
			}
			else
			{
				registry.start(module.getId());
			}

			updateControls(module, toggleButton, statusLabel);
		}
		catch (RuntimeException ex)
		{
			log.error("Unable to change state of Tickforge module {}", module.getId(), ex);

			statusLabel.setText("Failed — check log");
			statusLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		}
	}

	private void updateControls(
		TickforgeModule module,
		JButton toggleButton,
		JLabel statusLabel)
	{
		final boolean running = registry.isRunning(module.getId());

		statusLabel.setText(running ? "Running" : "Stopped");
		statusLabel.setForeground(
			running
				? ColorScheme.PROGRESS_COMPLETE_COLOR
				: ColorScheme.LIGHT_GRAY_COLOR
		);

		toggleButton.setText(running ? "Stop" : "Start");
	}

	private void addModuleSettings(
	JPanel details,
	TickforgeModule module)
	{
		if (module instanceof MenuEntrySnapshotModule)
		{
			addMenuEntrySnapshotSettings(
				details,
				(MenuEntrySnapshotModule) module);
		}
	}

	private void addMenuEntrySnapshotSettings(
		JPanel details,
		MenuEntrySnapshotModule module)
	{
		final JCheckBox blankTileFilter =
			new JCheckBox("Ignore blank walkable tiles");

		blankTileFilter.setOpaque(false);
		blankTileFilter.setSelected(
			module.isBlankWalkableTileFilterEnabled());

		blankTileFilter.setToolTipText(
			"Ignore changing Walk here entries when hovering empty tiles.");

		blankTileFilter.addActionListener(event ->
			module.setBlankWalkableTileFilterEnabled(
				blankTileFilter.isSelected()));

		details.add(blankTileFilter);
	}
}