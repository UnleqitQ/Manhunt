package me.unleqitq.manhunt;

import java.util.Set;
import java.util.UUID;

import me.unleqitq.manhunt.Manhunt.Party;


public interface WinListener {
	
	public void onWin(Party party, Set<UUID> winners, Set<UUID> losers);
	
}
