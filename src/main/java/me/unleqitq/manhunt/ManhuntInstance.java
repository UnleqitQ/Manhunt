package me.unleqitq.manhunt;

import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;


public class ManhuntInstance {
	
	private UUID owner;
	private Set<UUID> hunters;
	private Set<UUID> runners;
	public Map<UUID, UUID> tracking;
	private Set<UUID> died;
	public Map<UUID, Map<UUID, Location>> locationPoints;
	private Location spawn;
	
	private boolean running;
	private long startingTime;
	private boolean finished;
	private boolean runnerWon;
	
	private BukkitTask task;
	
	
	public ManhuntInstance(UUID owner) {
		this.owner = owner;
		hunters = new HashSet<>();
		runners = new HashSet<>();
		tracking = new HashMap<>();
		died = new HashSet<>();
		locationPoints = new HashMap<>();
		Bukkit.getPlayer(owner).sendMessage("Created manhunt");
	}
	
	
	public boolean isRunning() {
		return running;
	}
	
	public long getStartingTime() {
		return startingTime;
	}
	
	public void setOwner(UUID owner) {
		this.owner = owner;
	}
	
	public void addHunter(UUID player) {
		if (Manhunt.plugin.manager.hasInstance(player)) {
			if (Manhunt.plugin.manager.getInstance(player) != this) {
				Bukkit.getPlayer(owner).sendMessage("This player is already part of another manhunt");
			}
		}
		hunters.add(player);
		runners.remove(player);
		Manhunt.plugin.manager.playerInstances.put(player, this);
		Bukkit.getPlayer(owner).sendMessage(
				"Added " + Bukkit.getPlayer(player).getName() + "(" + player + ") as hunter");
	}
	
	public void addRunner(UUID player) {
		if (Manhunt.plugin.manager.hasInstance(player)) {
			if (Manhunt.plugin.manager.getInstance(player) != this) {
				Bukkit.getPlayer(owner).sendMessage("This player is already part of another manhunt");
			}
		}
		hunters.remove(player);
		runners.add(player);
		Manhunt.plugin.manager.playerInstances.put(player, this);
		Bukkit.getPlayer(owner).sendMessage(
				"Added " + Bukkit.getPlayer(player).getName() + "(" + player + ") as runner");
	}
	
	public void removeHunter(UUID player) {
		if (!Manhunt.plugin.manager.hasInstance(player) || Manhunt.plugin.manager.getInstance(player) != this) {
			Bukkit.getPlayer(owner).sendMessage("This player is not in your manhunt");
			return;
		}
		if (hunters.remove(player)) {
			Bukkit.getPlayer(owner).sendMessage(
					"Removed " + Bukkit.getPlayer(player).getName() + "(" + player + ") from hunters");
		}
		if (player != owner)
			Manhunt.plugin.manager.playerInstances.remove(player);
	}
	
	public void removeRunner(UUID player) {
		if (!Manhunt.plugin.manager.hasInstance(player) || Manhunt.plugin.manager.getInstance(player) != this) {
			Bukkit.getPlayer(owner).sendMessage("This player is not in your manhunt");
			return;
		}
		if (runners.remove(player)) {
			Bukkit.getPlayer(owner).sendMessage(
					"Removed " + Bukkit.getPlayer(player).getName() + "(" + player + ") from runners");
		}
		runners.remove(player);
		if (player != owner)
			Manhunt.plugin.manager.playerInstances.remove(player);
		Bukkit.getPlayer(owner).sendMessage(
				"Removed " + Bukkit.getPlayer(player).getName() + "(" + player + ") from runners");
	}
	
	public void start(Location location) {
		if (hunters.size() == 0) {
			Bukkit.getPlayer(owner).sendMessage("You need at least 1 hunter");
			return;
		}
		if (runners.size() == 0) {
			Bukkit.getPlayer(owner).sendMessage("You need at least 1 runner");
			return;
		}
		Bukkit.getPlayer(owner).sendMessage("Started manhunt");
		UUID runner = (UUID) runners.toArray()[0];
		for (UUID uuid : hunters) {
			tracking.put(uuid, runner);
		}
		for (UUID uuid : hunters) {
			Player player = Bukkit.getPlayer(uuid);
			if (player != null) {
				for (Iterator<Advancement> iterator = Bukkit.advancementIterator(); iterator.hasNext(); ) {
					Advancement advancement = iterator.next();
					AdvancementProgress progress = player.getAdvancementProgress(advancement);
					for (String criteria : progress.getAwardedCriteria())
						progress.revokeCriteria(criteria);
				}
				player.getInventory().clear();
				ItemStack compass = new ItemStack(Material.COMPASS);
				compass.addEnchantment(Enchantment.VANISHING_CURSE, 1);
				player.getInventory().addItem(compass);
				player.setGameMode(GameMode.SURVIVAL);
				player.setInvisible(false);
				player.setInvulnerable(false);
				player.setAllowFlight(false);
				player.setBedSpawnLocation(location, true);
				player.giveExpLevels(-1000);
				player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
				player.setSaturation(5);
				player.setFoodLevel(20);
				player.setExhaustion(0);
				player.setSaturatedRegenRate(10);
				player.teleport(location);
			}
		}
		for (UUID uuid : runners) {
			Player player = Bukkit.getPlayer(uuid);
			if (player != null) {
				for (Iterator<Advancement> iterator = Bukkit.advancementIterator(); iterator.hasNext(); ) {
					Advancement advancement = iterator.next();
					AdvancementProgress progress = player.getAdvancementProgress(advancement);
					for (String criteria : progress.getAwardedCriteria())
						progress.revokeCriteria(criteria);
				}
				player.getInventory().clear();
				player.setGameMode(GameMode.SURVIVAL);
				player.setInvisible(false);
				player.setInvulnerable(false);
				player.setAllowFlight(false);
				player.setBedSpawnLocation(location, true);
				player.giveExpLevels(-1000);
				player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
				player.setSaturation(5);
				player.setFoodLevel(20);
				player.setExhaustion(0);
				player.setSaturatedRegenRate(10);
				player.teleport(location);
			}
			locationPoints.put(uuid, new HashMap<>());
		}
		spawn = location;
		startingTime = System.currentTimeMillis();
		running = true;
		if (task != null)
			task.cancel();
		task = Bukkit.getScheduler().runTaskTimer(Manhunt.plugin, this::updateLocations, 0, 40);
	}
	
	public UUID getOwner() {
		return owner;
	}
	
	public Location getSpawn() {
		return spawn;
	}
	
	public Set<UUID> getHunters() {
		return hunters;
	}
	
	public Set<UUID> getRunners() {
		return runners;
	}
	
	public void stop() {
		if (task != null)
			task.cancel();
		Bukkit.getPlayer(owner).sendMessage("Stopped manhunt");
		for (UUID uuid : hunters) {
			Manhunt.plugin.manager.playerInstances.remove(uuid);
		}
		for (UUID uuid : runners) {
			Manhunt.plugin.manager.playerInstances.remove(uuid);
		}
		Manhunt.plugin.manager.playerInstances.remove(owner);
		Manhunt.plugin.manager.instances.remove(this);
	}
	
	public void write(UUID uuid) {
		Player player = Bukkit.getPlayer(uuid);
		player.sendMessage(ChatColor.GOLD + "--- Hunters ---");
		for (UUID pl : hunters) {
			player.sendMessage(Bukkit.getOfflinePlayer(pl).getName());
		}
		player.sendMessage(ChatColor.GOLD + "--- Runners ---");
		for (UUID pl : runners) {
			player.sendMessage(Bukkit.getOfflinePlayer(pl).getName());
		}
	}
	
	
	public void onHuntersWin() {
		finished = true;
		for (UUID uuid : hunters) {
			if (Bukkit.getPlayer(uuid) != null) {
				Player player = Bukkit.getPlayer(uuid);
				player.sendMessage(
						ChatColor.RED + "" + ChatColor.BOLD + "================================================================");
				player.sendMessage("");
				player.sendMessage(ChatColor.RED + "YOU WON");
				player.sendMessage("");
				player.sendMessage(
						ChatColor.RED + "" + ChatColor.BOLD + "================================================================");
			}
		}
		for (UUID uuid : runners) {
			if (Bukkit.getPlayer(uuid) != null) {
				Player player = Bukkit.getPlayer(uuid);
				player.sendMessage(
						ChatColor.RED + "" + ChatColor.BOLD + "================================================================");
				player.sendMessage("");
				player.sendMessage(ChatColor.RED + "YOU LOST");
				player.sendMessage("");
				player.sendMessage(
						ChatColor.RED + "" + ChatColor.BOLD + "================================================================");
			}
		}
		stop();
	}
	
	public void onRunnerWin() {
		finished = true;
		runnerWon = true;
		for (UUID uuid : runners) {
			if (Bukkit.getPlayer(uuid) != null) {
				Player player = Bukkit.getPlayer(uuid);
				player.sendMessage(
						ChatColor.RED + "" + ChatColor.BOLD + "================================================================");
				player.sendMessage("");
				player.sendMessage(ChatColor.RED + "YOU WON");
				player.sendMessage("");
				player.sendMessage(
						ChatColor.RED + "" + ChatColor.BOLD + "================================================================");
			}
		}
		for (UUID uuid : hunters) {
			if (Bukkit.getPlayer(uuid) != null) {
				Player player = Bukkit.getPlayer(uuid);
				player.sendMessage(
						ChatColor.RED + "" + ChatColor.BOLD + "================================================================");
				player.sendMessage("");
				player.sendMessage(ChatColor.RED + "YOU LOST");
				player.sendMessage("");
				player.sendMessage(
						ChatColor.RED + "" + ChatColor.BOLD + "================================================================");
			}
		}
		stop();
	}
	
	public void death(UUID uuid) {
		if (runners.contains(uuid)) {
			died.add(uuid);
			if (died.size() >= runners.size()) {
				onHuntersWin();
			}
		}
	}
	
	public void updateLocations() {
		for (UUID runner : runners) {
			if (!locationPoints.containsKey(runner))
				locationPoints.put(runner, new HashMap<>());
			Player p = Bukkit.getPlayer(runner);
			if (p != null) {
				locationPoints.get(runner).put(p.getWorld().getUID(), p.getLocation().clone());
			}
		}
	}
	
}
