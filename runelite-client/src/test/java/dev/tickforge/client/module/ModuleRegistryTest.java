package dev.tickforge.client.module;

import dev.tickforge.api.module.TickforgeModule;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModuleRegistryTest
{
	@Test
	public void startsAndStopsModule()
	{
		TestModule module = new TestModule("proof");
		ModuleRegistry registry = new ModuleRegistry(Collections.singleton(module));

		registry.start("proof");

		assertTrue(registry.isRunning("proof"));
		assertEquals(1, module.startCount);

		registry.stop("proof");

		assertFalse(registry.isRunning("proof"));
		assertEquals(1, module.stopCount);
	}

	@Test
	public void repeatedLifecycleCallsAreIdempotent()
	{
		TestModule module = new TestModule("proof");
		ModuleRegistry registry = new ModuleRegistry(Collections.singleton(module));

		registry.start("proof");
		registry.start("proof");
		registry.stop("proof");
		registry.stop("proof");

		assertEquals(1, module.startCount);
		assertEquals(1, module.stopCount);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsDuplicateIds()
	{
		ModuleRegistry registry = new ModuleRegistry(Collections.singleton(new TestModule("proof")));
		registry.register(new TestModule("proof"));
	}

	private static final class TestModule implements TickforgeModule
	{
		private final String id;
		private int startCount;
		private int stopCount;

		private TestModule(String id)
		{
			this.id = id;
		}

		@Override
		public String getId()
		{
			return id;
		}

		@Override
		public String getName()
		{
			return "Test module";
		}

		@Override
		public void startUp()
		{
			startCount++;
		}

		@Override
		public void shutDown()
		{
			stopCount++;
		}
	}
}
