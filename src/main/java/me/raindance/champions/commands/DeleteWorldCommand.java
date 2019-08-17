package me.raindance.champions.commands;

import me.raindance.champions.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeleteWorldCommand extends CommandBase {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(sender instanceof Player && sender.hasPermission("Champions.developer")){
            Player player = (Player) sender;
            if(args.length == 1){
                String worldName = args[0];
                WorldManager.getInstance().deleteWorld(Bukkit.getWorld(worldName), true);
                return true;
            }else player.sendMessage("There has to be an argument");
        } else {
            sender.sendMessage(String.format("%sChampions> %sYou have insufficient permissions to use that command.", ChatColor.BLUE, ChatColor.GRAY));
        }
        return true;
    }
}
