package dev.tickforge.client.module;

import dev.tickforge.api.module.TickforgeModule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Owns module registration and lifecycle state.
 *
 * This class deliberately contains no RuneLite event or UI integration yet.
 */
public final class ModuleRegistry
{
	private final Map<String, TickforgeModule> modules = new LinkedHashMap<>();
	private final Set<String> runningModuleIds = new LinkedHashSet<>();

	public ModuleRegistry(Collection<? extends TickforgeModule> modules)
	{
		for (TickforgeModule module : modules)
		{
			register(module);
		}
	}

	public void register(TickforgeModule module)
	{
		if (module == null)
		{
			throw new IllegalArgumentException("module must not be null");
		}

		String id = validateId(module.getId());
		TickforgeModule existing = modules.putIfAbsent(id, module);
		if (existing != null)
		{
			throw new IllegalArgumentException("duplicate module id: " + id);
		}
	}

	public List<TickforgeModule> getModules()
	{
		return Collections.unmodifiableList(new ArrayList<>(modules.values()));
	}

	public Optional<TickforgeModule> find(String id)
	{
		return Optional.ofNullable(modules.get(id));
	}

	public boolean isRunning(String id)
	{
		return runningModuleIds.contains(id);
	}

	public void start(String id)
	{
		TickforgeModule module = requireModule(id);
		if (runningModuleIds.contains(id))
		{
			return;
		}

		module.startUp();
		runningModuleIds.add(id);
	}

	public void stop(String id)
	{
		TickforgeModule module = requireModule(id);
		if (!runningModuleIds.contains(id))
		{
			return;
		}

		try
		{
			module.shutDown();
		}
		finally
		{
			runningModuleIds.remove(id);
		}
	}

	public void stopAll()
	{
		List<String> running = new ArrayList<>(runningModuleIds);
		Collections.reverse(running);
		for (String id : running)
		{
			stop(id);
		}
	}

	private TickforgeModule requireModule(String id)
	{
		TickforgeModule module = modules.get(id);
		if (module == null)
		{
			throw new IllegalArgumentException("unknown module id: " + id);
		}
		return module;
	}

	private static String validateId(String id)
	{
		if (id == null || id.trim().isEmpty())
		{
			throw new IllegalArgumentException("module id must not be blank");
		}
		return id;
	}
}
