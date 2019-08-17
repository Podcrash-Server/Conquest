package me.raindance.champions.events.game;


import me.raindance.champions.game.Game;
import me.raindance.champions.game.objects.IObjective;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class GameCaptureEvent extends GamePlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private Game game;
    private Player who;
    private IObjective iObjective;

    public GameCaptureEvent(Game game, Player who, IObjective iObjective, String message) {
        super(game, who, message);
        this.iObjective = iObjective;
    }

    public GameCaptureEvent(Game game, Player who, IObjective iObjective){
        this(game, who, iObjective, String.format(ChatColor.BOLD + "You captured %s", iObjective.getName()));
    }

    public IObjective getObjective() {
        return iObjective;
    }

    public static HandlerList getHandlersList(){
        return handlers;
    }


}
