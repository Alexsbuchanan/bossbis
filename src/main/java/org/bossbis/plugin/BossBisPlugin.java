package org.bossbis.plugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "BossBiS",
	description = "Bank-aware best-in-slot loadout planner",
	tags = {"boss", "dps", "gear", "bis"}
)
public class BossBisPlugin extends Plugin
{
	@Override
	protected void startUp()
	{
		log.debug("BossBiS started (v0.0 skeleton)");
	}

	@Override
	protected void shutDown()
	{
		log.debug("BossBiS stopped");
	}
}
