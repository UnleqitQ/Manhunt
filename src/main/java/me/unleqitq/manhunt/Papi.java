package me.unleqitq.manhunt;

import java.util.UUID;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;


public class Papi extends PlaceholderExpansion {
	
	
	public Papi() {}
	
	public boolean persist() {
		return true;
	}
	
	public boolean canRegister() {
		return true;
	}
	
	public String getAuthor() {
		return Manhunt.plugin.getDescription().getAuthors().toString();
	}
	
	public String getIdentifier() {
		return "manhunt";
	}
	
	public String getVersion() {
		return Manhunt.plugin.getDescription().getVersion();
	}
	
	public String onPlaceholderRequest(Player player, String identifier) {
		if (player == null) {
			return null;
		}
		identifier = identifier.toLowerCase();
		
		if (identifier.contains("hasmanhunt")) {
			return Boolean.toString(Manhunt.plugin.manager.hasInstance(player.getUniqueId()));
		}
		if (Manhunt.plugin.manager.hasInstance(player.getUniqueId())) {
			UUID uuid = player.getUniqueId();
			ManhuntInstance instance = Manhunt.plugin.manager.getInstance(uuid);
			if (identifier.contains("isowner"))
				return Boolean.toString(instance.getOwner().equals(uuid));
			if (identifier.contains("isrunner"))
				return Boolean.toString(instance.getRunners().contains(uuid));
			if (identifier.contains("ishunter"))
				return Boolean.toString(instance.getHunters().contains(uuid));
			if (identifier.contains("running"))
				return Boolean.toString(instance.isRunning());
			if (identifier.contains("time")) {
				long d = System.currentTimeMillis() - instance.getStartingTime();
				if (instance.isRunning()) {
					return String.format("%02dh %02dm %02ds", d / (60 * 60 * 1000), (d / (60 * 1000)) % 60,
							(d / 1000) % 60);
				}
				return "Not Started";
			}
		}
		
		return "";
	}
	
}
