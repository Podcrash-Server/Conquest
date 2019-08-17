package me.raindance.champions.commands;

import me.raindance.champions.Main;
import me.raindance.champions.game.Game;
import me.raindance.champions.game.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class SetMapCommand extends CommandBase {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player && sender.hasPermission("Champions.host")) {
            Player player = (Player) sender;
            if (args.length == 0) {
                sender.sendMessage("Require a mapname");
                sender.sendMessage("List of the available maps: " + Main.getInstance().getMapConfiguration().getKeys(false).toString());
            } else if(args.length == 1){
                if (GameManager.hasPlayer(player) && isValidMap(args[0])) {
                    Game game = GameManager.getGame(player);
                    GameManager.setGameMap(game, args[0]);
                    player.sendMessage("You selected " + args[0]);
                } else if(!isValidMap(args[0])) {
                    player.sendMessage("That is not a valid map: available maps are " + Main.getInstance().getMapConfiguration().getKeys(false).toString());
                }else player.sendMessage("You are currently not in a game");
            } else if(args.length == 2) {
                Game game = GameManager.getGame(Integer.parseInt(args[1]));
                if(isValidMap(args[0])) {
                    GameManager.setGameMap(game, args[0]);
                    player.sendMessage("You selected " + args[0] + " for game #" + game.getId());
                } else {
                    player.sendMessage("That is not a valid map: available maps are " + Main.getInstance().getMapConfiguration().getKeys(false).toString());
                }

            }
        } else {
            sender.sendMessage(String.format("%sChampions> %sYou have insufficient permissions to use that command.", ChatColor.BLUE, ChatColor.GRAY));
        }
        return true;
    }

    private boolean isValidMap(String mapName) {
        Set<String> validMaps = Main.getInstance().getMapConfiguration().getKeys(false);
        for(String map : validMaps) {
            if(mapName.equals(map)) {
                return true;
            }
        }
        return false;
    }
}
