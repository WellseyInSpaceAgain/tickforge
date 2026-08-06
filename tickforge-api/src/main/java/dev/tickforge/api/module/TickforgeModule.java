package dev.tickforge.api.module;

/**
 * A self-contained Tickforge capability controlled by the client-side runtime.
 */
public interface TickforgeModule
{
	/**
	 * Stable identifier used by configuration and persistence.
	 */
	String getId();

	/**
	 * Human-readable name shown in development tools.
	 */
	String getName();

	default String getDescription()
	{
		return "";
	}

	void startUp();

	void shutDown();
}
