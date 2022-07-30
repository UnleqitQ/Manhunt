package me.unleqitq.manhunt;


import me.unleqitq.commandframework.CommandManager;
import me.unleqitq.commandframework.building.argument.BooleanArgument;
import me.unleqitq.commandframework.building.argument.EnumArgument;
import me.unleqitq.commandframework.building.argument.PlayerArgument;
import me.unleqitq.commandframework.building.argument.WorldArgument;
import me.unleqitq.commandframework.building.command.FrameworkCommand;
import me.unleqitq.manhunt.api.IManhuntInstance;
import me.unleqitq.manhunt.api.ManhuntDefinition;
import me.unleqitq.manhunt.api.ManhuntEndEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class Manhunt extends JavaPlugin {
	
	private static Manhunt instance;
	public Object papi;
	public static Map<UUID, IManhuntInstance> instanceMap = new HashMap<>();
	public static Map<UUID, ManhuntDefinition> definitionMap = new HashMap<>();
	public static Map<UUID, Map.Entry<UUID, ManhuntEndEvent.Side>> requests = new HashMap<>();
	public CommandManager commandManager;
	
	@Override
	public void onEnable() {
		instance = this;
		commandManager = new CommandManager(this);
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			papi = new ManhuntPapi();
			((ManhuntPapi) papi).register();
		}
		registerCommands();
	}
	
	@Override
	public void onDisable() {
		if (papi != null) {
			((ManhuntPapi) papi).unregister();
		}
	}
	
	private void registerCommands() {
		FrameworkCommand.Builder<Player> manhuntBuilder = FrameworkCommand.playerCommandBuilder("manhunt");
		
		commandManager.register(manhuntBuilder.subCommand("start").handler(c -> {
			Player sender = (Player) c.getSender();
			ManhuntDefinition definition = getDefinition(sender.getUniqueId());
			if (definition.hunters.size() == 0) {
				sender.sendMessage("§6Manhunt >> §4There are no hunters!");
				return false;
			}
			if (definition.runners.size() == 0) {
				sender.sendMessage("§6Manhunt >> §4There are no runners!");
				return false;
			}
			
			if (instanceMap.containsKey(sender.getUniqueId())) {
				sender.sendMessage("§6Manhunt >> §4You already have a running Manhunt Game!");
				return false;
			}
			if (instanceMap.values().stream().anyMatch(
					instance -> instance.getHunters().contains(sender.getUniqueId()) ||
							instance.getRunners().contains(sender.getUniqueId()))) {
				sender.sendMessage("§6Manhunt >> §4You already participating in a running Manhunt Game!");
				return false;
			}
			
			instanceMap.put(sender.getUniqueId(), new ManhuntInstance(definition));
			sender.sendMessage("§6Manhunt >> §aStarted Manhunt Game");
			
			return true;
		}));
		
		commandManager.register(manhuntBuilder.subCommand("stop").handler(c -> {
			Player sender = (Player) c.getSender();
			
			if (!instanceMap.containsKey(sender.getUniqueId())) {
				sender.sendMessage("§6Manhunt >> §4You have no running Manhunt Game!");
				return false;
			}
			
			instanceMap.get(sender.getUniqueId()).stop();
			sender.sendMessage("§6Manhunt >> §aStopped Manhunt Game");
			
			return true;
		}));
		
		commandManager.register(manhuntBuilder.subCommand("reset").handler(c -> {
			Player sender = (Player) c.getSender();
			
			definitionMap.put(sender.getUniqueId(), new ManhuntDefinition(sender.getUniqueId()));
			sender.sendMessage("§6Manhunt >> §aReset definition");
			
			return true;
		}));
		
		commandManager.register(
				manhuntBuilder.subCommand("accept").argument(PlayerArgument.of("requester")).handler(c -> {
					Player sender = (Player) c.getSender();
					Player requester = c.get("requester");
					
					if (!requests.containsKey(sender.getUniqueId()) ||
							!requests.get(sender.getUniqueId()).getKey().equals(requester.getUniqueId())) {
						sender.sendMessage("§6Manhunt >> §4You have no request from this player!");
					}
					
					getDefinition(requester.getUniqueId()).runners.remove(sender.getUniqueId());
					getDefinition(requester.getUniqueId()).hunters.remove(sender.getUniqueId());
					
					(requests.get(sender.getUniqueId()).getValue() == ManhuntEndEvent.Side.HUNTER ?
							getDefinition(requester.getUniqueId()).hunters :
							getDefinition(requester.getUniqueId()).runners).add(sender.getUniqueId());
					requester.sendMessage("§6Manhunt >> §a" + sender.getName() + " added as a " +
							requests.get(sender.getUniqueId()).getValue().name().toLowerCase());
					requester.sendMessage("§6Manhunt >> §aYou were added as a " +
							requests.get(sender.getUniqueId()).getValue().name().toLowerCase());
					requests.remove(sender.getUniqueId());
					
					return true;
				}));
		
		commandManager.register(
				manhuntBuilder.subCommand("reject").argument(PlayerArgument.of("requester")).handler(c -> {
					Player sender = (Player) c.getSender();
					Player requester = c.get("requester");
					
					if (!requests.containsKey(sender.getUniqueId()) ||
							!requests.get(sender.getUniqueId()).getKey().equals(requester.getUniqueId())) {
						sender.sendMessage("§6Manhunt >> §4You have no request from this player!");
					}
					
					(requests.get(sender.getUniqueId()).getValue() == ManhuntEndEvent.Side.HUNTER ?
							getDefinition(requester.getUniqueId()).hunters :
							getDefinition(requester.getUniqueId()).runners).add(sender.getUniqueId());
					requester.sendMessage("§6Manhunt >> §a" + sender.getName() + " rejected the invite");
					requester.sendMessage("§6Manhunt >> §aYou rejected the invite");
					requests.remove(sender.getUniqueId());
					
					return true;
				}));
		
		FrameworkCommand.Builder<Player> settingsBuilder = manhuntBuilder.subCommand("settings");
		
		commandManager.register(
				settingsBuilder.subCommand("all-runners-must-die").argument(BooleanArgument.of("flag")).handler(c -> {
					getDefinition(((Player) c.getSender()).getUniqueId()).allRunnersMustDie = c.get("flag");
					if (getDefinition(((Player) c.getSender()).getUniqueId()).allRunnersMustDie) {
						c.getSender().sendMessage("§6Manhunt >> §aNow all runners must die for the hunters to win");
					}
					else {
						c.getSender().sendMessage("§6Manhunt >> §aNow only one runner must die for the hunters to win");
					}
					return true;
				}));
		
		commandManager.register(
				settingsBuilder.subCommand("dead-runners-can-continue").argument(BooleanArgument.of("flag"))
						.handler(c -> {
							getDefinition(((Player) c.getSender()).getUniqueId()).deadRunnersCanContinue =
									c.get("flag");
							if (getDefinition(((Player) c.getSender()).getUniqueId()).deadRunnersCanContinue) {
								c.getSender().sendMessage("§6Manhunt >> §aNow dead runners can help the other runners");
							}
							else {
								c.getSender().sendMessage("§6Manhunt >> §aNow dead runners can only spectate");
							}
							return true;
						}));
		
		commandManager.register(
				settingsBuilder.subCommand("dead-runners-can-fight").argument(BooleanArgument.of("flag")).handler(c -> {
					getDefinition(((Player) c.getSender()).getUniqueId()).deadRunnersCanFight = c.get("flag");
					if (getDefinition(((Player) c.getSender()).getUniqueId()).deadRunnersCanFight) {
						c.getSender().sendMessage("§6Manhunt >> §aNow dead runners can fight the hunters");
					}
					else {
						c.getSender().sendMessage("§6Manhunt >> §aNow dead runners can not fight the hunters");
					}
					return true;
				}));
		
		commandManager.register(
				settingsBuilder.subCommand("end-world").argument(WorldArgument.of("world")).handler(c -> {
					World world = c.get("world");
					if (world.getEnvironment() != World.Environment.THE_END) {
						c.getSender().sendMessage("§6Manhunt >> §aThis world's environment is not the end");
						return false;
					}
					getDefinition(((Player) c.getSender()).getUniqueId()).endWorld = world.getUID();
					return true;
				}));
		
		commandManager.register(settingsBuilder.subCommand("add").argument(PlayerArgument.of("player"))
				.argument(EnumArgument.of("type", ManhuntEndEvent.Side.class)).handler(c -> {
					Player sender = (Player) c.getSender();
					Player player = c.get("player");
					ManhuntEndEvent.Side type = c.get("type");
					
					requests.put(player.getUniqueId(), Map.entry(sender.getUniqueId(), type));
					sender.sendMessage("§6Manhunt >> §aSent a request to " + player.getName());
					player.sendMessage(
							Component.text("§6Manhunt >> §a" + sender.getName() + " invited you to a manhunt ")
									.append(Component.text("§a§l[Accept]")
											.clickEvent(ClickEvent.runCommand("/manhunt accept " + sender.getName())))
									.append(Component.text(" ")).append(Component.text("§c§l[Reject]")
											.clickEvent(ClickEvent.runCommand("/manhunt reject " + sender.getName()))));
					
					return true;
				}));
		
		commandManager.register(
				settingsBuilder.subCommand("remove").argument(PlayerArgument.of("player")).handler(c -> {
					Player sender = (Player) c.getSender();
					Player player = c.get("player");
					ManhuntEndEvent.Side type = c.get("type");
					
					requests.put(player.getUniqueId(), Map.entry(sender.getUniqueId(), type));
					sender.sendMessage("§6Manhunt >> §aRemoved " + player.getName());
					player.sendMessage("§6Manhunt >> §aYou were removed from the manhunt");
					
					return true;
				}));
		
	}
	
	@NotNull
	public static ManhuntDefinition getDefinition(UUID player) {
		if (!definitionMap.containsKey(player))
			definitionMap.put(player, new ManhuntDefinition(player));
		return definitionMap.get(player);
	}
	
	public static Manhunt getInstance() {
		return instance;
	}
	
	
}
