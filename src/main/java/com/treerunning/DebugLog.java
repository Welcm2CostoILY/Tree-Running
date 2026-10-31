package com.treerunning;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Writes what the tracker sees (Farming XP drops, patch changes, saplings leaving the
 * inventory, and what it decided) to .runelite/tree-running/debug-YYYY-MM-DD.log, so a run
 * can be checked afterwards. Off unless turned on in settings. Writing happens on its own
 * thread, never on the game thread.
 */
@Slf4j
final class DebugLog
{
	static final File FOLDER = new File(RuneLite.RUNELITE_DIR, "tree-running");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final ExecutorService writer = Executors.newSingleThreadExecutor(r ->
	{
		Thread t = new Thread(r, "tree-running-debug-log");
		t.setDaemon(true);
		return t;
	});
	private volatile boolean enabled;

	void setEnabled(boolean enabled)
	{
		boolean was = this.enabled;
		this.enabled = enabled;
		if (enabled && !was)
		{
			log(-1, "--- debug log on ---");
		}
	}

	boolean isEnabled()
	{
		return enabled;
	}

	/** @param tick the game tick, or -1 if not in game */
	void log(int tick, String message)
	{
		if (!enabled)
		{
			return;
		}
		String line = LocalTime.now().format(TIME) + " t" + tick + "  " + message + System.lineSeparator();
		LocalDate day = LocalDate.now();
		writer.execute(() ->
		{
			if (!FOLDER.exists() && !FOLDER.mkdirs())
			{
				return;
			}
			File file = new File(FOLDER, "debug-" + day + ".log");
			try (Writer w = new BufferedWriter(new FileWriter(file, true)))
			{
				w.write(line);
			}
			catch (IOException e)
			{
				log.warn("Couldn't write the Tree Running debug log", e);
			}
		});
	}

	void shutDown()
	{
		writer.shutdown();
	}
}
