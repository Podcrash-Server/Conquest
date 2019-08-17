package me.raindance.champions.events.game;

import me.raindance.champions.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class GamePlayerEvent extends GameEvent {
    private static final HandlerList handlers = new HandlerList();
    protected Player who;

    public GamePlayerEvent(Game game, Player who, String message) {
        super(game, message);
        this.who = who;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Player getWho() {
        return who;
    }
}
