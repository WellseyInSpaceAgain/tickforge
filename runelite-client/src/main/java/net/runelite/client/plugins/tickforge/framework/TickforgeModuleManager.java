package net.runelite.client.plugins.tickforge.framework;

import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.plugins.tickforge.activities.ProofModule;

@Singleton
public class TickforgeModuleManager
{
	private final ModuleRegistry registry;

	@Inject
	public TickforgeModuleManager(ProofModule proofModule)
	{
		registry = new ModuleRegistry(Arrays.asList(
			proofModule
		));
	}

	public ModuleRegistry getRegistry()
	{
		return registry;
	}

	public void shutDown()
	{
		registry.stopAll();
	}
}