package me.unleqitq.manhunt.api;

import java.util.Set;
import java.util.UUID;

public interface IManhuntInstance {
	
	Set<UUID> getHunters();
	Set<UUID> getRunners();
	Set<UUID> getDeadRunners();
	long getStartMillis();
	
	void stop();
	
}
