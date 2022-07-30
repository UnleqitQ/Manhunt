package me.unleqitq.manhunt;

import me.unleqitq.manhunt.api.IManhuntInstance;
import me.unleqitq.manhunt.api.ManhuntDefinition;
import me.unleqitq.manhunt.api.ManhuntEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.*;

public class ManhuntInstance implements IManhuntInstance, Listener {
	
	private final Map<UUID, Map<UUID, Vector>> lastLocations = new HashMap<>();
	
	private final ManhuntDefinition definition;
	private final Set<UUID> deadRunners = new HashSet<>();
	private final long startMillis;
	
	public ManhuntInstance(ManhuntDefinition definition) {
		this.definition = definition;
		startMillis = System.currentTimeMillis();
	}
	
	@Override
	public Set<UUID> getHunters() {
		return Collections.unmodifiableSet(definition.hunters);
	}
	
	@Override
	public Set<UUID> getRunners() {
		return Collections.unmodifiableSet(definition.runners);
	}
	
	@Override
	public Set<UUID> getDeadRunners() {
		return Collections.unmodifiableSet(deadRunners);
	}
	
	@Override
	public void stop() {
		HandlerList.unregisterAll(this);
		Bukkit.getPluginManager().callEvent(new ManhuntEndEvent(this, null));
		Manhunt.instanceMap.remove(definition.owner);
	}
	
	@Override
	public long getStartMillis() {
		return startMillis;
	}
	
	
	@EventHandler
	public void onRunnerAttack(EntityDamageByEntityEvent event) {
		if (definition.deadRunnersCanFight)
			return;
		if (event.getDamager() instanceof Player damager) {
			if (event.getEntity() instanceof Player damagee) {
				if (deadRunners.contains(damager.getUniqueId()) && definition.hunters.contains(damagee.getUniqueId())) {
					event.setCancelled(true);
				}
			}
		}
		else if (event.getDamager() instanceof Projectile projectile &&
				projectile.getShooter() instanceof Player damager) {
			if (event.getEntity() instanceof Player damagee) {
				if (deadRunners.contains(damager.getUniqueId()) && definition.hunters.contains(damagee.getUniqueId())) {
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onRunnerDeath(PlayerDeathEvent event) {
		if (definition.runners.contains(event.getPlayer().getUniqueId())) {
			deadRunners.add(event.getPlayer().getUniqueId());
			if (deadRunners.size() == definition.runners.size()) {
				HandlerList.unregisterAll(this);
				Bukkit.getPluginManager().callEvent(new ManhuntEndEvent(this, ManhuntEndEvent.Side.HUNTER));
				Manhunt.instanceMap.remove(definition.owner);
			}
		}
	}
	
	@EventHandler
	public void onDragonDeath(EntityDeathEvent event) {
		if (event.getEntity() instanceof EnderDragon dragon) {
			if (dragon.getWorld().getUID().equals(definition.endWorld)) {
				HandlerList.unregisterAll(this);
				Bukkit.getPluginManager().callEvent(new ManhuntEndEvent(this, ManhuntEndEvent.Side.RUNNER));
				Manhunt.instanceMap.remove(definition.owner);
			}
		}
	}
	
	
	@EventHandler
	public void onRunnerMove(PlayerMoveEvent event) {
		if (definition.runners.contains(event.getPlayer().getUniqueId())) {
			lastLocations.putIfAbsent(event.getPlayer().getUniqueId(), new HashMap<>());
			lastLocations.get(event.getPlayer().getUniqueId())
					.put(event.getPlayer().getWorld().getUID(), event.getTo().toVector());
		}
	}
	
	@EventHandler
	public void onCompassClick(PlayerInteractEvent event) {
		if (definition.hunters.contains(event.getPlayer().getUniqueId())) {
			ItemStack item = event.getItem();
			if (item == null || item.getType() != Material.COMPASS)
				return;
			CompassMeta meta = (CompassMeta) item.getItemMeta();
			if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (meta.getPersistentDataContainer().has(new NamespacedKey(Manhunt.getInstance(), "target"))) {
					long[] targetPrim = meta.getPersistentDataContainer()
							.get(new NamespacedKey(Manhunt.getInstance(), "target"), PersistentDataType.LONG_ARRAY);
					UUID target = new UUID(targetPrim[0], targetPrim[1]);
					if (!lastLocations.containsKey(target)) {
						event.getPlayer().sendMessage("§4Somehow the runner is not found here!");
						return;
					}
					if (!lastLocations.get(target).containsKey(event.getPlayer().getWorld().getUID())) {
						event.getPlayer().sendMessage("§4The runner has never been in this world!");
						return;
					}
					meta.setLodestoneTracked(false);
					meta.setLodestone(lastLocations.get(target).get(event.getPlayer().getWorld().getUID())
							.toLocation(event.getPlayer().getWorld()));
				}
			}
			else {
				Inventory inv = Bukkit.createInventory(event.getPlayer(), 6 * 9);
				for (UUID runner : definition.runners) {
					ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
					SkullMeta skullMeta = (SkullMeta) headItem.getItemMeta();
					skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(runner));
					skullMeta.setDisplayName(Bukkit.getOfflinePlayer(runner).getName());
					headItem.setItemMeta(skullMeta);
					inv.addItem(headItem);
				}
			}
			item.setItemMeta(meta);
		}
	}
	
	@EventHandler
	public void onHeadClick(InventoryClickEvent event) {
		if (definition.hunters.contains(event.getWhoClicked().getUniqueId())) {
			ItemStack compassItem = event.getWhoClicked().getInventory().getItemInMainHand();
			if (event.getCurrentItem().getType() == Material.PLAYER_HEAD)
				event.setCancelled(true);
			else
				return;
			if (compassItem.getType() != Material.COMPASS)
				return;
			CompassMeta meta = (CompassMeta) compassItem.getItemMeta();
			SkullMeta skullMeta = (SkullMeta) event.getCurrentItem().getItemMeta();
			meta.getPersistentDataContainer()
					.set(new NamespacedKey(Manhunt.getInstance(), "target"), PersistentDataType.LONG_ARRAY, new long[]{
							skullMeta.getOwningPlayer().getUniqueId().getMostSignificantBits(),
							skullMeta.getOwningPlayer().getUniqueId().getLeastSignificantBits()
					});
			if (!lastLocations.get(skullMeta.getOwningPlayer().getUniqueId())
					.containsKey(event.getWhoClicked().getWorld().getUID())) {
				event.getWhoClicked().sendMessage("§4The runner has never been in this world!");
			}
			else {
				meta.setLodestoneTracked(false);
				meta.setLodestone(lastLocations.get(skullMeta.getOwningPlayer().getUniqueId())
						.get(event.getWhoClicked().getWorld().getUID()).toLocation(event.getWhoClicked().getWorld()));
			}
			compassItem.setItemMeta(meta);
		}
	}
	
	
}
