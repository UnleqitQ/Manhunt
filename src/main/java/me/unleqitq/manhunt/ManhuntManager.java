package me.unleqitq.manhunt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.SkullMeta;


public class ManhuntManager implements Listener, TabExecutor {
	
	public Set<ManhuntInstance> instances;
	public Map<UUID, ManhuntInstance> playerInstances;
	public Map<UUID, Location> trackedLocations;
	
	public ManhuntManager() {
		Bukkit.getPluginManager().registerEvents(this, Manhunt.plugin);
		instances = new HashSet<>();
		playerInstances = new HashMap<>();
		trackedLocations = new HashMap<>();
	}
	
	public ManhuntInstance createInstance(UUID player) {
		if (playerInstances.containsKey(player)) {
			if (playerInstances.get(player).getOwner().equals(player)) {
				Manhunt.plugin.getServer().getPlayer(player).sendMessage("You are already hosting a Manhunt");
				Manhunt.plugin.getServer().getPlayer(player).sendMessage(
						"Use /manhunt stop to stop the Manhunt or /manhunt setowner <player> to make another player the owner and then /manhunt leave");
			}
			else {
				Manhunt.plugin.getServer().getPlayer(player).sendMessage("You already are part of a Manhunt");
				Manhunt.plugin.getServer().getPlayer(player).sendMessage("Use /manhunt leave to leave the Manhunt");
			}
			return null;
		}
		else {
			ManhuntInstance instance = new ManhuntInstance(player);
			instances.add(instance);
			playerInstances.put(player, instance);
			return instance;
		}
	}
	
	public ManhuntInstance getInstance(UUID player) {
		return playerInstances.get(player);
	}
	
	public boolean hasInstance(UUID player) {
		return playerInstances.containsKey(player);
	}
	
	public boolean ownsInstance(UUID player) {
		if (hasInstance(player)) {
			return getInstance(player).getOwner().equals(player);
		}
		return false;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player))
			return new ArrayList<>();
		if (args.length >= 1) {
			if (args.length == 1) {
				String[] args0 = {"start", "leave", "setowner", "stop", "create", "hunters", "runners"};
				List<String> results = new ArrayList<>();
				for (String arg : args0) {
					if (arg.contains(args[0])) {
						results.add(arg);
					}
				}
				return results;
			}
			else {
				if (args.length == 2) {
					if (args[0].contentEquals("hunters") || args[0].contentEquals("runners")) {
						String[] args0 = {"add", "remove"};
						List<String> results = new ArrayList<>();
						for (String arg : args0) {
							if (arg.contains(args[1])) {
								results.add(arg);
							}
						}
						return results;
					}
					else if (args[0].contentEquals("setowner")) {
						List<String> players = new ArrayList<>();
						if (playerInstances.containsKey(((Player) sender).getUniqueId())) {
							for (UUID player : getInstance(((Player) sender).getUniqueId()).getHunters()) {
								players.add(Bukkit.getPlayer(player).getName());
							}
							for (UUID player : getInstance(((Player) sender).getUniqueId()).getRunners()) {
								players.add(Bukkit.getPlayer(player).getName());
							}
						}
						return players;
					}
				}
				if (args.length == 3) {
					List<String> players = new ArrayList<>();
					for (Player player : Bukkit.getOnlinePlayers()) {
						players.add(player.getName());
					}
					return players;
				}
			}
		}
		return new ArrayList<>();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player))
			return false;
		UUID player = ((Player) sender).getUniqueId();
		if (args.length == 0) {
			if (hasInstance(player)) {
				getInstance(player).write(player);
			}
			return true;
		}
		if (args[0].contentEquals("start")) {
			if (ownsInstance(player)) {
				getInstance(player).start(Bukkit.getPlayer(player).getLocation());
				return true;
			}
		}
		if (args[0].contentEquals("stop")) {
			if (ownsInstance(player)) {
				getInstance(player).stop();
				return true;
			}
		}
		if (args[0].contentEquals("create")) {
			return createInstance(player) != null;
		}
		if (args[0].contentEquals("hunters")) {
			if (args.length <= 2)
				return false;
			Player player2 = Bukkit.getPlayer(args[2]);
			if (player2 == null) {
				System.out.println("Player not found");
				return false;
			}
			if (ownsInstance(player)) {
				if (args[1].contentEquals("add")) {
					getInstance(player).addHunter(player2.getUniqueId());
					return true;
				}
				if (args[1].contentEquals("remove")) {
					getInstance(player).removeHunter(player2.getUniqueId());
					return true;
				}
			}
		}
		if (args[0].contentEquals("runners")) {
			if (args.length <= 2)
				return false;
			Player player2 = Bukkit.getPlayer(args[2]);
			if (player2 == null) {
				System.out.println("Player not found");
				return false;
			}
			if (ownsInstance(player)) {
				if (args[1].contentEquals("add")) {
					getInstance(player).addRunner(player2.getUniqueId());
					return true;
				}
				if (args[1].contentEquals("remove")) {
					getInstance(player).removeRunner(player2.getUniqueId());
					return true;
				}
			}
		}
		if (args[0].contentEquals("setowner")) {
			if (ownsInstance(player)) {
				Player player2 = Bukkit.getPlayer(args[2]);
				if (player2 == null)
					return false;
				getInstance(player).setOwner(player2.getUniqueId());
				return true;
			}
		}
		if (args[0].contentEquals("leave")) {
			if (hasInstance(player) && !ownsInstance(player)) {
				ManhuntInstance instance = getInstance(player);
				instance.removeHunter(player);
				if (hasInstance(player)) {
					instance.removeRunner(player);
				}
			}
			if (hasInstance(player)) {
				sender.sendMessage("You can't leave your own manhunt");
			}
			else {
				sender.sendMessage("You are in no manhunt");
			}
		}
		return false;
	}
	
	@EventHandler
	public void onEvent(PlayerInteractEvent ev) {
		Player player = ev.getPlayer();
		UUID uuid = player.getUniqueId();
		if (hasInstance(uuid)) {
			if (getInstance(uuid).getHunters().contains(uuid)) {
				if (ev.getMaterial() == Material.COMPASS) {
					if (ev.getAction() == Action.RIGHT_CLICK_AIR || ev.getAction() == Action.RIGHT_CLICK_BLOCK) {
						OfflinePlayer tracking = Bukkit.getOfflinePlayer(getInstance(uuid).tracking.get(uuid));
						if (!tracking.isOnline()) {
							player.sendMessage("The player is not online");
							return;
						}
						if (player.getWorld().equals(((Player) tracking).getWorld())) {
							// player.setCompassTarget(tracking.getLocation());
							track(player, (Player) tracking, ev.getItem());
						}
						else {
							player.sendMessage("The player is not in your world");
						}
						ev.setCancelled(true);
					}
					if (ev.getAction() == Action.LEFT_CLICK_AIR || ev.getAction() == Action.LEFT_CLICK_BLOCK) {
						Inventory inv = Bukkit.createInventory(player, 27, "Players");
						for (UUID runnerUuid : getInstance(uuid).getRunners()) {
							OfflinePlayer runner = Bukkit.getOfflinePlayer(runnerUuid);
							if (runner.isOnline()) {
								ItemStack head = new ItemStack(Material.PLAYER_HEAD);
								SkullMeta meta = (SkullMeta) head.getItemMeta();
								meta.setOwningPlayer(runner);
								meta.setDisplayName(((Player) runner).getDisplayName());
								List<String> lore = new ArrayList<>();
								lore.add("�5Track Player");
								meta.setLore(lore);
								head.setItemMeta(meta);
								inv.addItem(head);
							}
						}
						player.openInventory(inv);
						ev.setCancelled(true);
					}
				}
			}
		}
	}
	
	public void track(Player player, Player target, ItemStack item) {
		Location location = new Location(target.getWorld(), target.getLocation().getBlockX(), 255,
				target.getLocation().getBlockZ());
		boolean otherTracking = false;
		for (Map.Entry<UUID, Location> entry : trackedLocations.entrySet()) {
			if (!entry.getKey().equals(player.getUniqueId())) {
				if (entry.getValue().equals(location)) {
					otherTracking = true;
					break;
				}
			}
		}
		if (!otherTracking) {
			Location location0 = trackedLocations.get(player.getUniqueId());
			if (location0 != null) {
				location0.getBlock().setType(Material.AIR);
			}
		}
		location.getBlock().setType(Material.LODESTONE);
		CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
		compassMeta.setLodestone(location);
		item.setItemMeta(compassMeta);
		player.sendMessage("Tracking " + target.getName());
		trackedLocations.put(player.getUniqueId(), location);
	}
	
	@EventHandler
	public void onBreak(BlockBreakEvent event) {
		if (playerInstances.containsKey(event.getPlayer())) {
			if (event.getBlock().getLocation().getBlockY() == 255 && event.getBlock().getType() == Material.LODESTONE) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + "Stop destroying the Lodestone");
			}
		}
	}
	
	@EventHandler
	public void onEvent(InventoryClickEvent ev) {
		if (ev.getWhoClicked() == null || ev.getWhoClicked().getOpenInventory() == null) {
			return;
		}
		ItemStack item = ev.getCurrentItem();
		if (item != null && item.getType() == Material.PLAYER_HEAD) {
			SkullMeta meta = (SkullMeta) item.getItemMeta();
			if (meta.getOwningPlayer().isOnline() && hasInstance(ev.getWhoClicked().getUniqueId())) {
				getInstance(ev.getWhoClicked().getUniqueId()).tracking.put(ev.getWhoClicked().getUniqueId(),
						meta.getOwningPlayer().getUniqueId());
				ev.getWhoClicked().closeInventory();
				ev.getWhoClicked().sendMessage(
						"Target changed to " + ((Player) meta.getOwningPlayer()).getDisplayName());
				ev.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onEvent(PlayerRespawnEvent ev) {
		if (hasInstance(ev.getPlayer().getUniqueId())) {
			if (getInstance(ev.getPlayer().getUniqueId()).getHunters().contains(ev.getPlayer().getUniqueId())) {
				ItemStack compass = new ItemStack(Material.COMPASS);
				compass.addEnchantment(Enchantment.VANISHING_CURSE, 1);
				ev.getPlayer().getInventory().addItem(compass);
				if (trackedLocations.containsKey(ev.getPlayer().getUniqueId())) {
					Location location = trackedLocations.get(ev.getPlayer().getUniqueId());
					location.getBlock().setType(Material.LODESTONE);
					CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
					compassMeta.setLodestone(location);
					compass.setItemMeta(compassMeta);
				}
			}
			final ManhuntInstance instance = getInstance(ev.getPlayer().getUniqueId());
			if (instance.isRunning()) {
				ev.getPlayer().sendMessage("YYYYYYYYYYYYY");
				if (ev.getRespawnLocation().getBlock().getType().toString().toLowerCase().contains("bed")) {
					if (ev.isAsynchronous()) {
						Bukkit.getScheduler().runTask(Manhunt.plugin, () -> {
							ev.getPlayer().sendMessage("XXXXXXXXXX");
							//ev.getPlayer().setBedSpawnLocation(instance.getSpawn());
							instance.getSpawn().getBlock().setType(Material.WHITE_BED);
							ev.getPlayer().teleport(instance.getSpawn());
							ev.setRespawnLocation(instance.getSpawn());
						});
					}
					else {
						ev.getPlayer().sendMessage("XXXXXXXXXX");
						//ev.getPlayer().setBedSpawnLocation(instance.getSpawn());
						instance.getSpawn().getBlock().setType(Material.WHITE_BED);
						ev.getPlayer().teleport(instance.getSpawn());
						ev.setRespawnLocation(instance.getSpawn());
					}
					Bukkit.getScheduler().runTaskLater(Manhunt.plugin, () -> {
						ev.getPlayer().teleport(instance.getSpawn());
					}, 10);
					Bukkit.getScheduler().runTaskLater(Manhunt.plugin, () -> {
						ev.getPlayer().teleport(instance.getSpawn());
					}, 20);
					Bukkit.getScheduler().runTaskLater(Manhunt.plugin, () -> {
						ev.getPlayer().teleport(instance.getSpawn());
					}, 30);
					Bukkit.getScheduler().runTaskLater(Manhunt.plugin, () -> {
						ev.getPlayer().teleport(instance.getSpawn());
						instance.getSpawn().getBlock().setType(Material.AIR);
					}, 40);
				}
			}
		}
	}
	
	@EventHandler
	public void onEvent(PlayerDeathEvent ev) {
		if (hasInstance(ev.getEntity().getUniqueId())) {
			if (getInstance(ev.getEntity().getUniqueId()).getRunners().contains(ev.getEntity().getUniqueId())) {
				getInstance(ev.getEntity().getUniqueId()).death(ev.getEntity().getUniqueId());
			}
		}
	}
	
	@EventHandler
	public void onEvent(EntityDamageByEntityEvent ev) {
		if (ev.getEntityType() == EntityType.ENDER_DRAGON) {
			if (ev.getDamager() != null && ev.getDamager().getType() == EntityType.PLAYER) {
				if (((EnderDragon) ev.getEntity()).getHealth() - ev.getFinalDamage() <= 0) {
					Player player = (Player) ev.getDamager();
					if (hasInstance(player.getUniqueId())) {
						getInstance(player.getUniqueId()).onRunnerWin();
					}
				}
			}
		}
	}
	
}
