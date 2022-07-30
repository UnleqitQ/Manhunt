package me.unleqitq.manhunt.api;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ManhuntDefinition implements Cloneable {
	
	@NotNull
	public UUID owner;
	public final Set<UUID> hunters = new HashSet<>();
	public final Set<UUID> runners = new HashSet<>();
	
	public boolean allRunnersMustDie;
	public boolean deadRunnersCanContinue;
	public boolean deadRunnersCanFight;
	
	public UUID endWorld;
	
	public ManhuntDefinition(@NotNull UUID owner) {
		this.owner = owner;
		Optional<World> firstEnd =
				Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.THE_END).findFirst();
		if (firstEnd.isEmpty())
			throw new IllegalStateException("No End World found!");
		
	}
	
	
	@Override
	public ManhuntDefinition clone() {
		ManhuntDefinition clone = new ManhuntDefinition(owner);
		clone.allRunnersMustDie = allRunnersMustDie;
		clone.deadRunnersCanContinue = allRunnersMustDie;
		clone.deadRunnersCanFight = allRunnersMustDie;
		clone.hunters.addAll(hunters);
		clone.runners.addAll(runners);
		clone.endWorld = endWorld;
		return clone;
	}
	
}
