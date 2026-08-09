package net.runelite.client.plugins.tickforge.framework;

import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.plugins.tickforge.activities.ProofModule;
import net.runelite.client.plugins.tickforge.devtools.GameStateSnapshotModule;
import net.runelite.client.plugins.tickforge.devtools.InteractionProbeModule;
import net.runelite.client.plugins.tickforge.devtools.MenuEntrySnapshotModule;

@Singleton
public class TickforgeModuleManager {
	private final ModuleRegistry registry;

	@Inject
	public TickforgeModuleManager(
			ProofModule proofModule,
			MenuEntrySnapshotModule menuEntrySnapshotModule,
			InteractionProbeModule interactionProbeModule,
			GameStateSnapshotModule gameStateSnapshotModule) {
		registry = new ModuleRegistry(Arrays.asList(
				proofModule,
				menuEntrySnapshotModule,
				interactionProbeModule,
				gameStateSnapshotModule));
	}

	public ModuleRegistry getRegistry() {
		return registry;
	}

	public void shutDown() {
		registry.stopAll();
	}
}