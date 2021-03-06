package org.minimallycorrect.tickprofiler.minecraft.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import org.minimallycorrect.tickprofiler.Log;
import org.minimallycorrect.tickprofiler.minecraft.TickProfiler;
import org.minimallycorrect.tickprofiler.minecraft.profiling.*;
import org.minimallycorrect.tickprofiler.util.TableFormatter;

import java.util.*;

public class ProfileCommand extends Command {
	public static String name = "profile";

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean requireOp() {
		return TickProfiler.instance.requireOpForProfileCommand;
	}

	@Override
	public void processCommand(final ICommandSender commandSender, List<String> arguments) {
		process(commandSender, arguments);
	}

	private void process(final ICommandSender commandSender, List<String> arguments) {
		World world = null;
		int time_;
		Integer x = null;
		Integer z = null;
		ProfilingState type;
		try {
			if (arguments.isEmpty())
				throw new UsageException();

			type = ProfilingState.get(arguments.get(0));
			if (type == null)
				throw new UsageException();

			if (type == ProfilingState.CHUNK_ENTITIES && arguments.size() > 2) {
				x = Integer.valueOf(arguments.remove(1));
				z = Integer.valueOf(arguments.remove(1));
			}

			time_ = type.time;

			if (arguments.size() > 1) {
				time_ = Integer.valueOf(arguments.get(1));
			}

			switch (type) {
				case PACKETS:
					PacketProfiler.profile(commandSender, time_);
					return;
				case UTILISATION:
					UtilisationProfiler.profile(commandSender, time_);
					return;
				case LOCK_CONTENTION:
					int resolution = 240;
					if (arguments.size() > 2) {
						resolution = Integer.valueOf(arguments.get(2));
					}
					ContentionProfiler.profile(commandSender, time_, resolution);
					return;
				case LAG_SPIKE_DETECTOR:
					LagSpikeProfiler.profile(commandSender, time_);
					return;
			}

			if (arguments.size() > 2) {
				world = DimensionManager.getWorld(Integer.valueOf(arguments.get(2)));
			} else if (type == ProfilingState.CHUNK_ENTITIES && commandSender instanceof Entity) {
				world = ((Entity) commandSender).world;
			}
			if (type == ProfilingState.CHUNK_ENTITIES && x == null) {
				if (!(commandSender instanceof Entity)) {
					throw new UsageException("/profile c needs chunk arguments when used from console");
				}
				Entity entity = (Entity) commandSender;
				x = entity.chunkCoordX;
				z = entity.chunkCoordZ;
			}
		} catch (UsageException e) {
			sendChat(commandSender, getUsage(commandSender));
			return;
		}

		final List<World> worlds = new ArrayList<>();
		if (world == null) {
			Collections.addAll(worlds, DimensionManager.getWorlds());
		} else {
			worlds.add(world);
		}
		final int time = time_;
		final EntityTickProfiler entityTickProfiler = EntityTickProfiler.INSTANCE;
		if (!entityTickProfiler.startProfiling(() -> sendChat(commandSender, entityTickProfiler.writeStringData(new TableFormatter(commandSender)).toString()), type, time, worlds)) {
			sendChat(commandSender, "Someone else is currently profiling.");
			return;
		}
		if (type == ProfilingState.CHUNK_ENTITIES) {
			entityTickProfiler.setLocation(x, z);
		}
		sendChat(commandSender, "Profiling for " + time + " seconds in " + (world == null ? "all worlds " : Log.name(world))
			+ (type == ProfilingState.CHUNK_ENTITIES ? " at " + x + ',' + z : ""));
	}

	@Override
	public String getUsage(ICommandSender icommandsender) {
		return "Usage: /profile [e/p/u/l/s/(c [chunkX] [chunk z])] timeInSeconds dimensionID\n" +
			"example - profile for 30 seconds in chunk 8,1 in all worlds: /profile c 8 1\n" +
			"example - profile for 10 seconds in dimension 4: /profile e 10 4\n" +
			"example - profile packets: /profile p";
	}

	public enum ProfilingState {
		NONE(null, 0),
		ENTITIES("e", 30),
		CHUNK_ENTITIES("c", 30),
		PACKETS("p", 30),
		UTILISATION("u", 240),
		LOCK_CONTENTION("l", 240),
		LAG_SPIKE_DETECTOR("s", 600);

		static final Map<String, ProfilingState> states = new HashMap<>();

		static {
			for (ProfilingState p : ProfilingState.values()) {
				states.put(p.shortcut, p);
			}
		}

		final String shortcut;
		final int time;

		ProfilingState(String shortcut, int time) {
			this.shortcut = shortcut;
			this.time = time;
		}

		public static ProfilingState get(String shortcut) {
			return states.get(shortcut.toLowerCase());
		}
	}
}
