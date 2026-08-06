package net.runelite.client.plugins.tickforge.activities;

import dev.tickforge.api.module.TickforgeModule;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ProofModule implements TickforgeModule
{
	@Override
	public String getId()
	{
		return "proof";
	}

	@Override
	public String getName()
	{
		return "Proof Module";
	}

	@Override
	public void startUp()
	{
		log.info("Proof module started");
	}

	@Override
	public void shutDown()
	{
		log.info("Proof module stopped");
	}
}