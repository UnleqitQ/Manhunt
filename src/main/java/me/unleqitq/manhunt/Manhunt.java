package me.unleqitq.manhunt;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;


public class Manhunt extends JavaPlugin {
	
	public static Manhunt plugin;
	public ManhuntManager manager;
	private Object papi;
	
	public Manhunt() {
		plugin = this;
	}
	
	@Override
	public void onEnable() {
		manager = new ManhuntManager();
		super.onEnable();
		registerCommand("manhunt", manager);
		// protocolManager.addPacketListener(new PacketAdapter(this,
		// PacketType.Play.Server.) {
		//
		// @Override
		// public void onPacketSending(PacketEvent event) {
		//
		// }
		//
		// });
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			papi = new Papi();
			((Papi) papi).register();
		}
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			((Papi) papi).unregister();
		}
	}
	
	@Nonnull
	public <T extends CommandExecutor> void registerCommand(String cmd, T handler) {
		getCommand(cmd).setExecutor((CommandExecutor) handler);
	}
	
	public static enum Party {
		RUNNER,
		HUNTER
	}
	
}
