package me.unleqitq.manhunt.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ManhuntEndEvent extends Event {
	
	//TODO: make cancellable
	
	private static final HandlerList handlers = new HandlerList();
	@NotNull
	private final IManhuntInstance manhuntInstance;
	@Nullable
	private final Side side;
	
	public ManhuntEndEvent(@NotNull IManhuntInstance manhuntInstance, @Nullable Side side) {
		this.manhuntInstance = manhuntInstance;
		this.side = side;
	}
	
	
	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	@Nullable
	public Side getSide() {
		return side;
	}
	
	@NotNull
	public IManhuntInstance getManhuntInstance() {
		return manhuntInstance;
	}
	
	public enum Side {
		RUNNER,
		HUNTER
	}
	
}
