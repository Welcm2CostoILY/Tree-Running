package com.treerunning;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Starts RuneLite with this plugin loaded. Run this class's main method from IntelliJ
 * (VM option: -ea, program argument: --developer-mode), or use ./gradlew runClient.
 */
public class TreeRunningPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TreeRunningPlugin.class);
		RuneLite.main(args);
	}
}
