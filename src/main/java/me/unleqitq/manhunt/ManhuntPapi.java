package me.unleqitq.manhunt;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.unleqitq.manhunt.api.IManhuntInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class ManhuntPapi extends PlaceholderExpansion {
	
	@Override
	public @NotNull String getIdentifier() {
		return "manhunt";
	}
	
	@Override
	public @NotNull String getAuthor() {
		return "UnleqitQ";
	}
	
	@Override
	public @NotNull String getVersion() {
		return Manhunt.getInstance().getDescription().getVersion();
	}
	
	@Override
	public boolean persist() {
		return true;
	}
	
	@Override
	public @NotNull List<String> getPlaceholders() {
		return List.of("time");
	}
	
	@Override
	public String onPlaceholderRequest(Player player, @NotNull String params) {
		if (params.equalsIgnoreCase("time")) {
			Optional<IManhuntInstance> manhuntInstance = Manhunt.instanceMap.values().stream()
					.filter(mi -> mi.getHunters().contains(player.getUniqueId()) ||
							mi.getRunners().contains(player.getUniqueId())).findFirst();
			if (manhuntInstance.isEmpty())
				return "0.000";
			return String.format("%.03f",
					(System.currentTimeMillis() - manhuntInstance.orElseThrow().getStartMillis()) / 1000.);
		}
		return "";
	}
	
}
